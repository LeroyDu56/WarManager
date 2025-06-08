package org.Novania.WarManager.gui;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.Novania.WarManager.WarManager;
import org.Novania.WarManager.models.War;
import org.Novania.WarManager.models.WarSide;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class SideSelectionGUI implements Listener {
    
    private final WarManager plugin;
    private final War war;
    private final String nationName;
    private final UUID playerUUID;
    private static final Set<UUID> processingPlayers = new HashSet<>();
    private boolean isActive = true;
    
    public SideSelectionGUI(WarManager plugin, War war, String nationName, Player player) {
        this.plugin = plugin;
        this.war = war;
        this.nationName = nationName;
        this.playerUUID = player.getUniqueId();
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        
        plugin.getLogger().info("SideSelectionGUI créé pour " + nationName + " par " + player.getName());
    }
    
    public void openGUI(Player player) {
        if (!isActive) {
            plugin.getLogger().warning("Tentative d'ouverture d'un GUI désactivé pour " + nationName);
            return;
        }
        
        int size = 27;
        String title = "§6Camp pour §e" + nationName + " §6- " + player.getName();
        
        Inventory inv = Bukkit.createInventory(null, size, title);
        
        // Ajouter une info sur la nation
        ItemStack nationInfo = new ItemStack(Material.NAME_TAG);
        ItemMeta nationMeta = nationInfo.getItemMeta();
        if (nationMeta != null) {
            nationMeta.setDisplayName("§e§lNation: §f" + nationName);
            List<String> nationLore = new ArrayList<>();
            nationLore.add("§7Choisissez un camp pour cette nation");
            nationLore.add("§7Joueur: §f" + player.getName());
            nationLore.add("");
            
            // Vérifier si déjà dans un camp
            WarSide currentSide = getCurrentSide();
            if (currentSide != null) {
                nationLore.add("§6Actuellement dans: " + currentSide.getDisplayName());
                nationLore.add("§7Cliquez sur un autre camp pour changer");
            } else {
                nationLore.add("§7Non assignée à un camp");
            }
            
            nationMeta.setLore(nationLore);
            nationInfo.setItemMeta(nationMeta);
        }
        inv.setItem(4, nationInfo);
        
        // Ajouter les camps disponibles
        List<WarSide> sides = war.getSides();
        int[] sideSlots = {10, 12, 14, 16}; // Positions pour les camps
        
        for (int i = 0; i < Math.min(sides.size(), 4); i++) {
            WarSide side = sides.get(i);
            ItemStack sideItem = createSideItem(side);
            inv.setItem(sideSlots[i], sideItem);
        }
        
        // Bouton pour retirer de tous les camps
        WarSide currentSide = getCurrentSide();
        if (currentSide != null) {
            ItemStack removeButton = new ItemStack(Material.BARRIER);
            ItemMeta removeMeta = removeButton.getItemMeta();
            if (removeMeta != null) {
                removeMeta.setDisplayName("§c✖ Retirer de la guerre");
                List<String> removeLore = new ArrayList<>();
                removeLore.add("§7Retirer §f" + nationName + " §7de tous les camps");
                removeLore.add("§c§lClic pour confirmer");
                removeMeta.setLore(removeLore);
                removeButton.setItemMeta(removeMeta);
            }
            inv.setItem(20, removeButton);
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
        inv.setItem(22, backButton);
        
        player.openInventory(inv);
        plugin.getLogger().info("GUI ouvert pour " + nationName + " à " + player.getName());
    }
    
    private ItemStack createSideItem(WarSide side) {
        ItemStack item = new ItemStack(Material.WHITE_BANNER);
        ItemMeta meta = item.getItemMeta();
        
        if (meta != null) {
            meta.setDisplayName(side.getDisplayName());
            
            List<String> lore = new ArrayList<>();
            lore.add("§7Points: §f" + side.getPoints() + "/" + war.getRequiredPoints());
            lore.add("§7Nations: §f" + side.getNations().size());
            lore.add("");
            
            if (!side.getNations().isEmpty()) {
                lore.add("§6Membres actuels:");
                for (String nation : side.getNations()) {
                    if (nation.equals(nationName)) {
                        lore.add("  §a✓ " + nation + " §7(actuel)");
                    } else {
                        lore.add("  §7- " + nation);
                    }
                }
                lore.add("");
            }
            
            if (side.hasNation(nationName)) {
                lore.add("§a§l✓ Nation déjà dans ce camp");
                item.setType(Material.LIME_BANNER);
            } else {
                lore.add("§e§lClic pour rejoindre ce camp");
                
                // Couleur différente selon le camp
                switch (side.getColor()) {
                    case "§c": item.setType(Material.RED_BANNER); break;
                    case "§9": item.setType(Material.BLUE_BANNER); break;
                    case "§a": item.setType(Material.GREEN_BANNER); break;
                    case "§e": item.setType(Material.YELLOW_BANNER); break;
                    case "§6": item.setType(Material.ORANGE_BANNER); break;
                    case "§5": item.setType(Material.PURPLE_BANNER); break;
                    case "§b": item.setType(Material.CYAN_BANNER); break;
                    case "§0": item.setType(Material.BLACK_BANNER); break;
                    case "§7": item.setType(Material.GRAY_BANNER); break;
                    default: item.setType(Material.WHITE_BANNER); break;
                }
            }
            
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        
        return item;
    }
    
    private WarSide getCurrentSide() {
        for (WarSide side : war.getSides()) {
            if (side.hasNation(nationName)) {
                return side;
            }
        }
        return null;
    }
    
    private void deactivate() {
        isActive = false;
        HandlerList.unregisterAll(this);
        plugin.getLogger().info("SideSelectionGUI désactivé pour " + nationName);
    }
    
    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getPlayer().getUniqueId().equals(playerUUID) && 
            event.getView().getTitle().contains("§6Camp pour §e" + nationName)) {
            
            plugin.getLogger().info("Inventaire fermé pour " + nationName + " - désactivation du GUI");
            deactivate();
        }
    }
    
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        // Vérifier que c'est le bon GUI et le bon joueur
        if (!event.getView().getTitle().contains("§6Camp pour §e" + nationName) || 
            !event.getWhoClicked().getUniqueId().equals(playerUUID)) {
            return;
        }
        
        if (!isActive) {
            plugin.getLogger().info("Event ignoré - GUI désactivé pour " + nationName);
            return;
        }
        
        event.setCancelled(true);
        
        Player player = (Player) event.getWhoClicked();
        
        // Protection anti-double-clic
        if (processingPlayers.contains(player.getUniqueId())) {
            plugin.getLogger().info("Double-clic détecté pour " + player.getName() + " (" + nationName + ") - ignoré");
            return;
        }
        
        ItemStack item = event.getCurrentItem();
        
        if (item == null || item.getType() == Material.AIR) {
            return;
        }
        
        // Marquer le joueur comme en traitement
        processingPlayers.add(player.getUniqueId());
        
        plugin.getLogger().info("Traitement du clic pour " + nationName + " par " + player.getName());
        
        // Désactiver ce GUI immédiatement
        deactivate();
        
        // Fermer l'inventaire
        player.closeInventory();
        
        try {
            // Bouton retour
            if (item.getType() == Material.ARROW) {
                plugin.getLogger().info("Bouton retour cliqué pour " + nationName);
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    new NationSelectionGUI(plugin, war).openGUI(player);
                    processingPlayers.remove(player.getUniqueId());
                }, 3L);
                return;
            }
            
            // Bouton retirer
            if (item.getType() == Material.BARRIER) {
                plugin.getLogger().info("Bouton retirer cliqué pour " + nationName);
                
                WarSide currentSide = getCurrentSide();
                if (currentSide != null) {
                    plugin.getLogger().info("Retrait de " + nationName + " du camp " + currentSide.getName());
                    plugin.getWarDataManager().removeNationFromSide(war.getId(), currentSide.getName(), nationName);
                    player.sendMessage("§aNation §f" + nationName + " §aretirée du camp " + currentSide.getDisplayName());
                }
                
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    new NationSelectionGUI(plugin, war).openGUI(player);
                    processingPlayers.remove(player.getUniqueId());
                }, 3L);
                return;
            }
            
            // Sélection d'un camp (bannières)
            if (item.getType().name().contains("BANNER")) {
                if (!item.hasItemMeta() || !item.getItemMeta().hasDisplayName()) {
                    processingPlayers.remove(player.getUniqueId());
                    return;
                }
                
                String displayName = item.getItemMeta().getDisplayName();
                plugin.getLogger().info("Camp sélectionné: " + displayName + " pour nation " + nationName);
                
                // Trouver le camp correspondant
                WarSide targetSide = null;
                for (WarSide side : war.getSides()) {
                    if (side.getDisplayName().equals(displayName)) {
                        targetSide = side;
                        break;
                    }
                }
                
                if (targetSide != null) {
                    plugin.getLogger().info("Camp cible trouvé: " + targetSide.getName() + " pour " + nationName);
                    
                    // Retirer de l'ancien camp
                    WarSide currentSide = getCurrentSide();
                    if (currentSide != null && !currentSide.getName().equals(targetSide.getName())) {
                        plugin.getLogger().info("Retrait de " + nationName + " du camp " + currentSide.getName());
                        plugin.getWarDataManager().removeNationFromSide(war.getId(), currentSide.getName(), nationName);
                    }
                    
                    // Ajouter au nouveau camp
                    if (!targetSide.hasNation(nationName)) {
                        plugin.getLogger().info("Ajout de " + nationName + " au camp " + targetSide.getName());
                        plugin.getWarDataManager().addNationToSide(war.getId(), targetSide.getName(), nationName);
                        player.sendMessage("§aNation §f" + nationName + " §aajoutée au camp " + targetSide.getDisplayName());
                    } else {
                        player.sendMessage("§eNation §f" + nationName + " §eest déjà dans ce camp");
                    }
                    
                } else {
                    plugin.getLogger().warning("Camp cible non trouvé pour: " + displayName + " (nation: " + nationName + ")");
                    player.sendMessage("§cErreur : Camp non trouvé");
                }
                
                // Retourner au GUI des nations
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    new NationSelectionGUI(plugin, war).openGUI(player);
                    processingPlayers.remove(player.getUniqueId());
                }, 3L);
                return;
            }
            
        } catch (Exception e) {
            plugin.getLogger().severe("Erreur dans SideSelectionGUI pour " + nationName + ": " + e.getMessage());
            e.printStackTrace();
            player.sendMessage("§cErreur lors du changement de camp");
        } finally {
            // S'assurer que le joueur soit retiré de la liste de traitement
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                processingPlayers.remove(player.getUniqueId());
            }, 10L);
        }
    }
}