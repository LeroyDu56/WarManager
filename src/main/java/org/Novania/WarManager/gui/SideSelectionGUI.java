package org.Novania.WarManager.gui;

import java.util.ArrayList;
import java.util.List;

import org.Novania.WarManager.WarManager;
import org.Novania.WarManager.models.War;
import org.Novania.WarManager.models.WarSide;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class SideSelectionGUI {
    
    private final WarManager plugin;
    private final War war;
    private final String nationName;

    
    public SideSelectionGUI(WarManager plugin, War war, String nationName, Player requester) {
        this.plugin = plugin;
        this.war = war;
        this.nationName = nationName;
    }
    
    public void openGUI(Player player) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            // Récupérer les données à jour
            War currentWar = plugin.getWarDataManager().getWar(war.getId());
            if (currentWar == null) {
                player.sendMessage("§cErreur : Guerre introuvable");
                return;
            }
            
            // Retour sur le thread principal
            Bukkit.getScheduler().runTask(plugin, () -> {
                createAndShowGUI(player, currentWar);
            });
        });
    }
    
    private void createAndShowGUI(Player player, War currentWar) {
        List<WarSide> sides = currentWar.getSides();
        
        // DEBUG
        plugin.getLogger().info("=== DEBUG SIDE SELECTION ===");
        plugin.getLogger().info("Guerre: " + currentWar.getName() + " (ID: " + currentWar.getId() + ")");
        plugin.getLogger().info("Nombre de camps: " + sides.size());
        for (WarSide side : sides) {
            plugin.getLogger().info("  - Camp: " + side.getName() + " (" + side.getDisplayName() + ")");
        }
        
        if (sides.isEmpty()) {
            player.sendMessage("§cAucun camp disponible pour cette guerre !");
            player.sendMessage("§7Créez d'abord des camps avec /waradmin addside ou via l'interface de gestion");
            return;
        }
        
        int size = Math.max(18, ((sides.size() + 8) / 9) * 9 + 9); // +9 pour les boutons utilitaires
        Inventory inv = Bukkit.createInventory(null, size, "§6Camp pour " + nationName + " - Guerre #" + currentWar.getId());
        
        // Ajouter les camps disponibles
        for (int i = 0; i < sides.size() && i < size - 9; i++) {
            WarSide side = sides.get(i);
            ItemStack sideItem = createSideItem(side, currentWar);
            inv.setItem(i, sideItem);
            plugin.getLogger().info("Ajout du camp " + side.getName() + " au slot " + i);
        }
        
        // Bouton retour
        ItemStack backButton = new ItemStack(Material.ARROW);
        ItemMeta backMeta = backButton.getItemMeta();
        if (backMeta != null) {
            backMeta.setDisplayName("§c← Retour");
            List<String> backLore = new ArrayList<>();
            backLore.add("§7Retourner à la sélection des nations");
            backMeta.setLore(backLore);
            backButton.setItemMeta(backMeta);
        }
        inv.setItem(size - 1, backButton);
        
        player.openInventory(inv);
    }
    
    private ItemStack createSideItem(WarSide side, War currentWar) {
        ItemStack item = new ItemStack(Material.WHITE_BANNER);
        setBannerColorByCode(item, side.getColor());
        
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(side.getDisplayName());
            
            List<String> lore = new ArrayList<>();
            lore.add("§7Points: §f" + side.getPoints() + "§7/§f" + currentWar.getRequiredPoints());
            lore.add("§7Kills: §f" + side.getKills());
            lore.add("§7Nations: §f" + side.getNations().size());
            lore.add("");
            
            if (!side.getNations().isEmpty()) {
                lore.add("§6Nations dans ce camp:");
                for (String nation : side.getNations()) {
                    lore.add("  §7• §f" + nation);
                }
                lore.add("");
            }
            
            // Vérifier si la nation est déjà dans ce camp
            if (side.hasNation(nationName)) {
                lore.add("§a✓ " + nationName + " est déjà dans ce camp");
                lore.add("§c§lClic pour retirer du camp");
                item.setType(Material.GREEN_BANNER);
            } else {
                lore.add("§e§lClic pour ajouter " + nationName);
            }
            
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        
        return item;
    }
    
    private void setBannerColorByCode(ItemStack item, String color) {
        switch (color) {
            case "§c" -> item.setType(Material.RED_BANNER);
            case "§9" -> item.setType(Material.BLUE_BANNER);
            case "§a" -> item.setType(Material.GREEN_BANNER);
            case "§e" -> item.setType(Material.YELLOW_BANNER);
            case "§6" -> item.setType(Material.ORANGE_BANNER);
            case "§5" -> item.setType(Material.PURPLE_BANNER);
            case "§b" -> item.setType(Material.CYAN_BANNER);
            case "§0" -> item.setType(Material.BLACK_BANNER);
            case "§7" -> item.setType(Material.GRAY_BANNER);
            default -> item.setType(Material.WHITE_BANNER);
        }
    }
    
    public static void handleClickStatic(InventoryClickEvent event, Player player, WarManager plugin) {
        String title = event.getView().getTitle();
        if (!title.startsWith("§6Camp pour ")) {
            return;
        }
        
        // PROTECTION: S'assurer que l'événement est annulé
        event.setCancelled(true);
        
        ItemStack item = event.getCurrentItem();
        if (item == null || item.getType() == Material.AIR) {
            return;
        }
        
        // Fermer l'inventaire immédiatement
        player.closeInventory();
        
        // Extraire le nom de la nation et l'ID de guerre depuis le titre
        // Format: "§6Camp pour NationName - Guerre #1"
        String nationName = "";
        int warId = -1;
        
        if (title.contains(" - Guerre #")) {
            String[] parts = title.split(" - Guerre #");
            if (parts.length >= 2) {
                nationName = parts[0].replace("§6Camp pour ", "").trim();
                try {
                    warId = Integer.parseInt(parts[1].trim());
                } catch (NumberFormatException e) {
                    warId = -1;
                }
            }
        } else {
            // Ancien format sans ID de guerre
            nationName = title.substring("§6Camp pour ".length()).trim();
        }
        
        plugin.getLogger().info("DEBUG SideSelection - Nation: " + nationName + ", WarId: " + warId);
        
        // Bouton retour
        if (item.getType() == Material.ARROW) {
            Bukkit.getScheduler().runTask(plugin, () -> {
                player.sendMessage("§eRetour à la sélection des nations");
                // new NationSelectionGUI(plugin, war).openGUI(player);
            });
            return;
        }
        
        // Sélection d'un camp
        if (item.getType().name().contains("BANNER") && item.hasItemMeta() && item.getItemMeta() != null && item.getItemMeta().hasDisplayName()) {
            handleSideSelection(player, nationName, item, plugin);
        }
    }
    
    private static void handleSideSelection(Player player, String nationName, ItemStack item, WarManager plugin) {
        if (item.getItemMeta() == null) {
            player.sendMessage("§cErreur : Item invalide");
            return;
        }
        
        String sideDisplayName = item.getItemMeta().getDisplayName();
        
        // Extraire le nom réel du camp (sans les codes couleur)
        String sideName = extractRealSideName(sideDisplayName);
        if (sideName.isEmpty()) {
            player.sendMessage("§cErreur : Impossible d'identifier le camp");
            return;
        }
        
        // Traitement asynchrone
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            // Trouver la guerre (on devrait avoir l'ID mais on va chercher par nation)
            War war = findWarForNation(plugin, nationName);
            if (war == null) {
                player.sendMessage("§cErreur : Guerre introuvable pour cette nation");
                return;
            }
            
            WarSide side = war.getSideByName(sideName);
            if (side == null) {
                player.sendMessage("§cErreur : Camp introuvable");
                return;
            }
            
            // Retour sur le thread principal
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (side.hasNation(nationName)) {
                    // Retirer la nation du camp
                    plugin.getWarDataManager().removeNationFromSide(war.getId(), sideName, nationName);
                    player.sendMessage("§aNation §f" + nationName + " §aretirée du camp " + side.getDisplayName());
                } else {
                    // Ajouter la nation au camp
                    plugin.getWarDataManager().addNationToSide(war.getId(), sideName, nationName);
                    player.sendMessage("§aNation §f" + nationName + " §aajoutée au camp " + side.getDisplayName());
                }
                
                // Rouvrir le GUI avec les données mises à jour
                new SideSelectionGUI(plugin, war, nationName, player).openGUI(player);
            });
        });
    }
    
    private static String extractRealSideName(String displayName) {
        // Retirer tous les codes couleur
        return displayName.replaceAll("§[0-9a-fk-or]", "").trim();
    }
    
    private static War findWarForNation(WarManager plugin, String nationName) {
        // D'abord, vérifier si la nation est déjà dans une guerre
        for (War war : plugin.getWarDataManager().getActiveWars().values()) {
            for (WarSide side : war.getSides()) {
                if (side.hasNation(nationName)) {
                    plugin.getLogger().info("Nation " + nationName + " trouvée dans guerre #" + war.getId());
                    return war;
                }
            }
        }
        
        // Sinon, retourner une guerre active qui a des camps disponibles
        for (War war : plugin.getWarDataManager().getActiveWars().values()) {
            if (war.isActive() && !war.getSides().isEmpty()) {
                plugin.getLogger().info("Guerre active avec camps trouvée: #" + war.getId());
                return war;
            }
        }
        
        plugin.getLogger().warning("Aucune guerre appropriée trouvée pour " + nationName);
        return null;
    }
}