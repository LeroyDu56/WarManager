package org.Novania.WarManager.listeners;

import org.Novania.WarManager.WarManager;
import org.Novania.WarManager.models.War;
import org.Novania.WarManager.models.WarSide;
import org.Novania.WarManager.utils.MessageUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;

import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Resident;

public class PlayerDeathListener implements Listener {
    
    private final WarManager plugin;
    
    public PlayerDeathListener(WarManager plugin) {
        this.plugin = plugin;
    }
    
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = false)
    public void onPlayerDeath(PlayerDeathEvent event) {
        // Log de base pour vérifier que l'event se déclenche
        plugin.getLogger().info("=== PlayerDeathEvent détecté ===");
        
        Player victim = event.getEntity();
        Player killer = victim.getKiller();
        
        plugin.getLogger().info("Victime: " + victim.getName());
        plugin.getLogger().info("Tueur: " + (killer != null ? killer.getName() : "null"));
        
        // Vérifier qu'il y a bien un tueur joueur
        if (killer == null) {
            plugin.getLogger().info("Aucun tueur joueur → pas de kill de guerre");
            return;
        }
        
        // Vérifier que les deux sont bien des joueurs connectés
        if (!killer.isOnline() || !victim.isOnline()) {
            plugin.getLogger().info("Un des joueurs n'est pas connecté → ignoré");
            return;
        }
        
        // Obtenir les nations des joueurs via Towny
        String killerNation = getNationName(killer);
        String victimNation = getNationName(victim);
        
        plugin.getLogger().info("Nation du tueur: " + killerNation);
        plugin.getLogger().info("Nation de la victime: " + victimNation);
        
        if (killerNation == null) {
            plugin.getLogger().info("Tueur " + killer.getName() + " n'est pas dans une nation → ignoré");
            return;
        }
        
        if (victimNation == null) {
            plugin.getLogger().info("Victime " + victim.getName() + " n'est pas dans une nation → ignoré");
            return;
        }
        
        if (killerNation.equals(victimNation)) {
            plugin.getLogger().info("Même nation (" + killerNation + ") → kill ignoré");
            return;
        }
        
        plugin.getLogger().info("Nations différentes → vérification des guerres actives");
        
        // Vérifier dans quelle guerre ces nations sont impliquées
        var activeWars = plugin.getWarDataManager().getActiveWars();
        plugin.getLogger().info("Nombre de guerres actives: " + activeWars.size());
        
        boolean killCounted = false;
        for (War war : activeWars.values()) {
            plugin.getLogger().info("Vérification guerre #" + war.getId() + " (" + war.getName() + ") - Active: " + war.isActive());
            
            if (!war.isActive()) {
                plugin.getLogger().info("Guerre #" + war.getId() + " inactive → ignorée");
                continue;
            }
            
            if (areNationsInOpposingSides(war, killerNation, victimNation)) {
                plugin.getLogger().info("✓ Nations en guerre opposée dans guerre #" + war.getId());
                
                // Obtenir le camp du tueur
                WarSide killerSide = getPlayerSide(war, killerNation);
                if (killerSide != null) {
                    plugin.getLogger().info("✓ Camp du tueur trouvé: " + killerSide.getName());
                    
                    // FIX: Enregistrer le kill en base de données (sans ajouter de points)
                    plugin.getWarDataManager().recordKill(
                        war.getId(),
                        killer.getUniqueId(),
                        victim.getUniqueId(),
                        killerNation,
                        victimNation
                    );
                    
                    // FIX: Ajouter les points au camp (SEULEMENT ici selon la config)
                    int killPoints = plugin.getConfigManager().getKillPoints();
                    int oldPoints = killerSide.getPoints();
                    killerSide.addPoints(killPoints); // Ajouter SEULEMENT les points configurés
                    int newPoints = killerSide.getPoints();
                    
                    plugin.getLogger().info("Points kill ajoutés: " + killPoints + " (avant: " + oldPoints + ", après: " + newPoints + ")");
                    
                    // Mettre à jour en base de données
                    plugin.getWarDataManager().updateSidePoints(war.getId(), killerSide.getName(), newPoints);
                    
                    // Diffuser le message de kill
                    String message = MessageUtils.getMessage("war.kill_registered")
                            .replace("{killer}", killer.getName())
                            .replace("{victim}", victim.getName())
                            .replace("{points}", String.valueOf(killPoints))
                            .replace("{side}", killerSide.getDisplayName());
                    
                    if (plugin.getConfig().getBoolean("settings.notifications.kill_broadcast", true)) {
                        Bukkit.broadcastMessage(message);
                        plugin.getLogger().info("Message de kill diffusé: " + message);
                    }
                    
                    // Vérifier si la guerre est gagnée
                    if (newPoints >= war.getRequiredPoints()) {
                        plugin.getLogger().info("🏆 VICTOIRE ! " + killerSide.getName() + " a atteint " + war.getRequiredPoints() + " points");
                        
                        war.setActive(false);
                        
                        String winMessage = MessageUtils.getMessage("war.ended")
                                .replace("{winner}", killerSide.getDisplayName());
                        
                        Bukkit.broadcastMessage("§6§l=== VICTOIRE ! ===");
                        Bukkit.broadcastMessage(winMessage);
                        Bukkit.broadcastMessage("§6§l================");
                        
                        // Terminer la guerre
                        plugin.getWarDataManager().endWar(war.getId());
                        
                    } else {
                        // Annoncer la progression
                        int remaining = war.getRequiredPoints() - newPoints;
                        String progressMessage = killerSide.getDisplayName() + " §7: §f" + newPoints + "§7/§f" + war.getRequiredPoints() + " pts §7(§f" + remaining + " §7restants)";
                        
                        if (plugin.getConfig().getBoolean("settings.notifications.war_updates", true)) {
                            Bukkit.broadcastMessage("§7[§6Guerre #" + war.getId() + "§7] " + progressMessage);
                        }
                    }
                    
                    killCounted = true;
                } else {
                    plugin.getLogger().warning("❌ Camp du tueur introuvable pour " + killerNation + " dans guerre #" + war.getId());
                    
                    // Debug: afficher tous les camps
                    plugin.getLogger().info("Camps disponibles dans cette guerre:");
                    for (WarSide side : war.getSides()) {
                        plugin.getLogger().info("  - " + side.getName() + ": " + side.getNations());
                    }
                }
                
                break; // Une guerre trouvée, pas besoin de continuer
            } else {
                plugin.getLogger().info("Nations pas en camps opposés dans guerre #" + war.getId());
            }
        }
        
        if (!killCounted) {
            plugin.getLogger().info("❌ Kill non comptabilisé: nations pas en guerre ou pas de guerre active");
        } else {
            plugin.getLogger().info("✅ Kill comptabilisé avec succès !");
        }
        
        plugin.getLogger().info("=== Fin PlayerDeathEvent ===");
    }
    
    private String getNationName(Player player) {
        try {
            Resident resident = TownyAPI.getInstance().getResident(player);
            if (resident != null && resident.hasTown() && resident.getTown().hasNation()) {
                Nation nation = resident.getTown().getNation();
                String nationName = nation.getName();
                plugin.getLogger().info("Nation trouvée pour " + player.getName() + ": " + nationName);
                return nationName;
            } else {
                plugin.getLogger().info("Pas de nation trouvée pour " + player.getName() + " (resident=" + resident + ")");
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Erreur lors de la récupération de la nation pour " + player.getName() + ": " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }
    
    private boolean areNationsInOpposingSides(War war, String nation1, String nation2) {
        WarSide side1 = null;
        WarSide side2 = null;
        
        plugin.getLogger().info("Vérification camps pour " + nation1 + " et " + nation2);
        
        for (WarSide side : war.getSides()) {
            plugin.getLogger().info("Camp " + side.getName() + " contient: " + side.getNations());
            
            if (side.hasNation(nation1)) {
                side1 = side;
                plugin.getLogger().info(nation1 + " trouvé dans le camp " + side.getName());
            }
            if (side.hasNation(nation2)) {
                side2 = side;
                plugin.getLogger().info(nation2 + " trouvé dans le camp " + side.getName());
            }
        }
        
        boolean opposing = side1 != null && side2 != null && !side1.equals(side2);
        plugin.getLogger().info("Résultat: " + nation1 + " (" + (side1 != null ? side1.getName() : "null") + ") vs " + 
                               nation2 + " (" + (side2 != null ? side2.getName() : "null") + ") → Opposés: " + opposing);
        
        return opposing;
    }
    
    private WarSide getPlayerSide(War war, String nationName) {
        for (WarSide side : war.getSides()) {
            if (side.hasNation(nationName)) {
                return side;
            }
        }
        return null;
    }
}