// WarSelectionGUI.java
package org.Novania.WarManager.gui;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.Novania.WarManager.WarManager;
import org.Novania.WarManager.models.War;
import org.Novania.WarManager.utils.MessageUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class WarSelectionGUI implements Listener {
    
    private final WarManager plugin;
    private final boolean isAdmin;
    
    public WarSelectionGUI(WarManager plugin) {
        this(plugin, false);
    }
    
    public WarSelectionGUI(WarManager plugin, boolean isAdmin) {
        this.plugin = plugin;
        this.isAdmin = isAdmin;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }
    
    public void openGUI(Player player) {
        Map<Integer, War> wars = plugin.getWarDataManager().getActiveWars();
        int size = Math.max(9, ((wars.size() + 8) / 9) * 9);
        
        String title = isAdmin ? "§cGestion des Guerres" : MessageUtils.getMessageRaw("gui.war_selection");
        Inventory inv = Bukkit.createInventory(null, size, title);
        
        int slot = 0;
        for (War war : wars.values()) {
            if (slot < size) {
                ItemStack item = createWarItem(war);
                inv.setItem(slot++, item);
            }
        }
        
        // Si aucune guerre, ajouter un item informatif
        if (wars.isEmpty()) {
            ItemStack noWars = new ItemStack(Material.BARRIER);
            ItemMeta meta = noWars.getItemMeta();
            meta.setDisplayName("§cAucune guerre active");
            List<String> lore = new ArrayList<>();
            lore.add("§7Il n'y a actuellement aucune guerre active.");
            if (isAdmin) {
                lore.add("");
                lore.add("§e/waradmin create <nom> <type>");
                lore.add("§7pour créer une nouvelle guerre");
            }
            meta.setLore(lore);
            noWars.setItemMeta(meta);
            inv.setItem(4, noWars);
        }
        
        player.openInventory(inv);
    }
    
    private ItemStack createWarItem(War war) {
        ItemStack item = new ItemStack(Material.IRON_SWORD);
        ItemMeta meta = item.getItemMeta();
        
        if (meta != null) {
            // Format simple et cohérent pour le parsing
            meta.setDisplayName("§aGuerre #" + war.getId() + ": " + war.getName());
            
            List<String> lore = new ArrayList<>();
            lore.add("§7Casus Belli: §f" + war.getCasusBeliType());
            lore.add("§7Points requis: §f" + war.getRequiredPoints());
            lore.add("§7Statut: " + (war.isActive() ? "§aActive" : "§cTerminée"));
            lore.add("§7Date: §f" + war.getStartDate().toLocalDate());
            lore.add("");
            
            if (!war.getSides().isEmpty()) {
                lore.add("§6Camps:");
                war.getSides().forEach(side -> {
                    lore.add("  " + side.getDisplayName() + " §7(" + side.getPoints() + " pts)");
                    if (!side.getNations().isEmpty()) {
                        String nations = String.join(", ", side.getNations());
                        if (nations.length() > 30) {
                            nations = nations.substring(0, 27) + "...";
                        }
                        lore.add("    §8" + nations);
                    }
                });
            } else {
                lore.add("§7Aucun camp configuré");
            }
            
            lore.add("");
            if (isAdmin) {
                lore.add("§e§lClic gauche §7pour gérer");
                lore.add("§c§lClic droit §7pour infos détaillées");
            } else {
                lore.add("§e§lClic gauche §7pour voir les stats");
            }
            
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        
        return item;
    }
    
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        String title = event.getView().getTitle();
        if (!title.equals("§cGestion des Guerres") && 
            !title.equals(MessageUtils.getMessageRaw("gui.war_selection"))) {
            return;
        }
        
        event.setCancelled(true);
        
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }
        
        Player player = (Player) event.getWhoClicked();
        ItemStack item = event.getCurrentItem();
        
        if (item == null || item.getType() == Material.AIR || item.getType() == Material.BARRIER) {
            return;
        }
        
        if (!item.hasItemMeta() || !item.getItemMeta().hasDisplayName()) {
            return;
        }
        
        // Fermer immédiatement l'inventaire pour éviter les multi-clics
        player.closeInventory();
        
        try {
            String displayName = item.getItemMeta().getDisplayName();
            plugin.getLogger().info("Clic détecté sur: " + displayName);
            
            // Extraire l'ID depuis "§aGuerre #1: NomDeLaGuerre"
            if (displayName.contains("Guerre #")) {
                String[] parts = displayName.split("Guerre #");
                if (parts.length > 1) {
                    String idPart = parts[1].split(":")[0].trim();
                    int warId = Integer.parseInt(idPart);
                    
                    plugin.getLogger().info("ID extrait: " + warId);
                    
                    War war = plugin.getWarDataManager().getWar(warId);
                    if (war != null) {
                        plugin.getLogger().info("Guerre trouvée: " + war.getName());
                        
                        // Délai pour éviter les problèmes de timing
                        Bukkit.getScheduler().runTaskLater(plugin, () -> {
                            // Différent comportement selon le type de clic et les permissions
                            if (isAdmin) {
                                if (event.getClick().isRightClick()) {
                                    // Clic droit = infos détaillées via commande
                                    player.performCommand("waradmin info " + warId);
                                } else {
                                    // Clic gauche = gestion via GUI
                                    new NationSelectionGUI(plugin, war).openGUI(player);
                                }
                            } else {
                                // Joueur normal = stats
                                new WarStatsGUI(plugin, war).openGUI(player);
                            }
                        }, 1L); // Délai de 1 tick
                        
                    } else {
                        player.sendMessage("§cErreur : Guerre introuvable (ID: " + warId + ")");
                        plugin.getLogger().warning("Guerre introuvable avec l'ID : " + warId);
                    }
                } else {
                    plugin.getLogger().warning("Format de nom invalide: " + displayName);
                    player.sendMessage("§cErreur de format du nom de guerre");
                }
            } else {
                plugin.getLogger().warning("Nom ne contient pas 'Guerre #': " + displayName);
                player.sendMessage("§cFormat de guerre non reconnu");
            }
            
        } catch (NumberFormatException e) {
            player.sendMessage("§cErreur : ID de guerre invalide");
            plugin.getLogger().severe("Erreur de parsing de l'ID : " + e.getMessage());
            
        } catch (Exception e) {
            player.sendMessage("§cErreur lors de la sélection de la guerre");
            plugin.getLogger().severe("Erreur dans onClick WarSelectionGUI : " + e.getMessage());
            e.printStackTrace();
        }
    }
}