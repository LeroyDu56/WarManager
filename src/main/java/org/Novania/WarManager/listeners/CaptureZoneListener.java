package org.Novania.WarManager.listeners;

import org.Novania.WarManager.WarManager;
import org.Novania.WarManager.models.CaptureZone;
import org.Novania.WarManager.models.War;
import org.Novania.WarManager.models.WarSide;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Resident;

public class CaptureZoneListener implements Listener {
    
    private final WarManager plugin;
    
    public CaptureZoneListener(WarManager plugin) {
        this.plugin = plugin;
    }
    
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        
        if (event.getClickedBlock() == null) {
            return;
        }
        
        Material blockType = event.getClickedBlock().getType();
        if (!blockType.name().contains("BANNER")) {
            return;
        }
        
        Player player = event.getPlayer();
        Location clickLocation = event.getClickedBlock().getLocation();
        
        plugin.getLogger().info("=== Interaction avec drapeau détectée ===");
        plugin.getLogger().info("Joueur: " + player.getName());
        plugin.getLogger().info("Position: " + clickLocation.getBlockX() + ", " + clickLocation.getBlockY() + ", " + clickLocation.getBlockZ());
        plugin.getLogger().info("Type de bloc: " + blockType.name());
        
        // Vérifier si c'est une zone de capture
        CaptureZone zone = null;
        for (CaptureZone z : plugin.getCaptureZoneManager().getActiveZones().values()) {
            if (z.isActive() && !z.isExpired()) {
                Location flagLoc = z.getFlagLocation();
                plugin.getLogger().info("Vérification zone #" + z.getId() + " - Flag: " + flagLoc.getBlockX() + ", " + flagLoc.getBlockY() + ", " + flagLoc.getBlockZ());
                
                // Vérifier si c'est le même bloc (tolérance de 1 bloc)
                if (Math.abs(clickLocation.getBlockX() - flagLoc.getBlockX()) <= 1 &&
                    Math.abs(clickLocation.getBlockY() - flagLoc.getBlockY()) <= 1 &&
                    Math.abs(clickLocation.getBlockZ() - flagLoc.getBlockZ()) <= 1 &&
                    clickLocation.getWorld().equals(flagLoc.getWorld())) {
                    zone = z;
                    plugin.getLogger().info("✓ Zone de capture trouvée: #" + z.getId());
                    break;
                }
            }
        }
        
        if (zone == null) {
            plugin.getLogger().info("❌ Aucune zone de capture trouvée à cette position");
            return;
        }
        
        event.setCancelled(true); // Empêcher l'interaction normale avec le drapeau
        
        plugin.getLogger().info("Interaction avec drapeau de zone #" + zone.getId() + " par " + player.getName());
        
        // Obtenir la nation du joueur
        String playerNation = getNationName(player);
        if (playerNation == null) {
            player.sendMessage("§cVous devez faire partie d'une nation pour capturer une zone !");
            plugin.getLogger().info("❌ " + player.getName() + " n'est pas dans une nation");
            return;
        }
        
        plugin.getLogger().info("Nation du joueur: " + playerNation);
        
        // Vérifier que la guerre est active
        War war = plugin.getWarDataManager().getWar(zone.getWarId());
        if (war == null || !war.isActive()) {
            player.sendMessage("§cLa guerre associée à cette zone n'est pas active !");
            plugin.getLogger().info("❌ Guerre #" + zone.getWarId() + " non active");
            return;
        }
        
        plugin.getLogger().info("Guerre trouvée: #" + war.getId() + " - " + war.getName());
        
        // Trouver le camp du joueur
        WarSide playerSide = null;
        for (WarSide side : war.getSides()) {
            plugin.getLogger().info("Vérification camp " + side.getName() + ": " + side.getNations());
            if (side.hasNation(playerNation)) {
                playerSide = side;
                plugin.getLogger().info("✓ Camp trouvé: " + side.getName() + " pour nation " + playerNation);
                break;
            }
        }
        
        if (playerSide == null) {
            player.sendMessage("§cVotre nation n'est pas impliquée dans cette guerre !");
            plugin.getLogger().info("❌ Nation " + playerNation + " pas dans la guerre");
            
            // Debug: afficher tous les camps
            plugin.getLogger().info("Camps disponibles:");
            for (WarSide side : war.getSides()) {
                plugin.getLogger().info("  - " + side.getName() + ": " + side.getNations());
            }
            return;
        }
        
        // Vérifier si la zone est expirée
        if (zone.isExpired()) {
            player.sendMessage("§cCette zone de capture est expirée !");
            plugin.getLogger().info("❌ Zone expirée");
            return;
        }
        
        // Vérifier si le camp contrôle déjà la zone
        if (playerSide.getName().equals(zone.getCurrentController())) {
            player.sendMessage("§eVotre camp contrôle déjà cette zone !");
            plugin.getLogger().info("ℹ Camp " + playerSide.getName() + " contrôle déjà la zone");
            showZoneStatus(player, zone, war);
            return;
        }
        
        // === CAPTURE LA ZONE ===
        plugin.getLogger().info("🏁 CAPTURE EN COURS - Zone #" + zone.getId() + " par " + playerSide.getName());
        
        String previousController = zone.getCurrentController();
        plugin.getCaptureZoneManager().captureZone(zone.getId(), playerSide.getName(), player.getName());
        
        player.sendMessage("§a✓ Zone " + zone.getName() + " capturée pour " + playerSide.getDisplayName() + " !");
        plugin.getLogger().info("✅ Zone capturée avec succès !");
        
        showZoneStatus(player, zone, war);
    }
    
    private void showZoneStatus(Player player, CaptureZone zone, War war) {
        player.sendMessage("§8§m----§r §6Zone: " + zone.getName() + " §8§m----§r");
        player.sendMessage("§7Temps restant: §f" + zone.getRemainingMinutes() + " minutes");
        
        if (zone.getCurrentController() != null) {
            WarSide controller = war.getSideByName(zone.getCurrentController());
            if (controller != null) {
                player.sendMessage("§7Contrôlée par: " + controller.getDisplayName());
            }
        } else {
            player.sendMessage("§7Contrôlée par: §cAucun camp");
        }
        
        player.sendMessage("§7Statistiques de contrôle:");
        for (WarSide side : war.getSides()) {
            long controlTime = zone.getTotalControlTime(side.getName());
            if (controlTime > 0) {
                player.sendMessage("  " + side.getDisplayName() + "§7: §f" + zone.formatControlTime(controlTime));
            }
        }
        player.sendMessage("§8§m------------------------§r");
    }
    
    private String getNationName(Player player) {
        try {
            Resident resident = TownyAPI.getInstance().getResident(player);
            if (resident != null && resident.hasTown() && resident.getTown().hasNation()) {
                Nation nation = resident.getTown().getNation();
                return nation.getName();
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Erreur lors de la récupération de la nation pour " + player.getName() + ": " + e.getMessage());
        }
        return null;
    }
}