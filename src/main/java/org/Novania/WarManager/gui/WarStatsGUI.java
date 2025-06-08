// WarStatsGUI.java
package org.Novania.WarManager.gui;

import java.util.ArrayList;
import java.util.List;

import org.Novania.WarManager.WarManager;
import org.Novania.WarManager.models.War;
import org.Novania.WarManager.models.WarSide;
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

public class WarStatsGUI implements Listener {
    
    private final WarManager plugin;
    private final War war;
    
    public WarStatsGUI(WarManager plugin, War war) {
        this.plugin = plugin;
        this.war = war;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }
    
    public void openGUI(Player player) {
        Inventory inv = Bukkit.createInventory(null, 54, MessageUtils.getMessageRaw("gui.war_stats") + " - " + war.getName());
        
        // Info générale de la guerre
        ItemStack warInfo = createWarInfoItem();
        inv.setItem(4, warInfo);
        
        // Items pour chaque camp
        List<WarSide> sides = war.getSides();
        if (sides.size() >= 1) {
            inv.setItem(19, createSideItem(sides.get(0), 0));
        }
        if (sides.size() >= 2) {
            inv.setItem(25, createSideItem(sides.get(1), 1));
        }
        if (sides.size() >= 3) {
            inv.setItem(37, createSideItem(sides.get(2), 2));
        }
        if (sides.size() >= 4) {
            inv.setItem(43, createSideItem(sides.get(3), 3));
        }
        
        // Statistiques globales
        ItemStack globalStats = createGlobalStatsItem();
        inv.setItem(13, globalStats);
        
        // Progression de la guerre
        ItemStack progression = createProgressionItem();
        inv.setItem(31, progression);
        
        // Bouton retour
        ItemStack back = new ItemStack(Material.ARROW);
        ItemMeta backMeta = back.getItemMeta();
        if (backMeta != null) {
            backMeta.setDisplayName("§c← Retour");
            List<String> backLore = new ArrayList<>();
            backLore.add("§7Retourner à la liste des guerres");
            backMeta.setLore(backLore);
            back.setItemMeta(backMeta);
        }
        inv.setItem(49, back);
        
        // Bouton actualiser
        ItemStack refresh = new ItemStack(Material.CLOCK);
        ItemMeta refreshMeta = refresh.getItemMeta();
        if (refreshMeta != null) {
            refreshMeta.setDisplayName("§e⟳ Actualiser");
            List<String> refreshLore = new ArrayList<>();
            refreshLore.add("§7Actualiser les statistiques");
            refreshMeta.setLore(refreshLore);
            refresh.setItemMeta(refreshMeta);
        }
        inv.setItem(45, refresh);
        
        player.openInventory(inv);
    }
    
    private ItemStack createWarInfoItem() {
        ItemStack item = new ItemStack(Material.ENCHANTED_BOOK);
        ItemMeta meta = item.getItemMeta();
        
        if (meta != null) {
            meta.setDisplayName("§e§l" + war.getName() + " §7(#" + war.getId() + ")");
            
            List<String> lore = new ArrayList<>();
            lore.add("§7Casus Belli: §f" + war.getCasusBeliType());
            lore.add("§7Points pour victoire: §f" + war.getRequiredPoints());
            lore.add("§7Date de début: §f" + war.getStartDate().toLocalDate());
            lore.add("§7Heure de début: §f" + war.getStartDate().toLocalTime().toString().substring(0, 5));
            lore.add("");
            
            WarSide winner = war.getWinner();
            if (winner != null) {
                lore.add("§a§l🏆 VICTOIRE: " + winner.getDisplayName());
                if (war.getEndDate() != null) {
                    lore.add("§7Date de fin: §f" + war.getEndDate().toLocalDate());
                }
            } else {
                lore.add("§7Statut: " + (war.isActive() ? "§aEn cours" : "§cTerminée"));
                
                // Calculer qui est en tête
                if (!war.getSides().isEmpty()) {
                    WarSide leading = war.getSides().stream()
                            .max((s1, s2) -> Integer.compare(s1.getPoints(), s2.getPoints()))
                            .orElse(null);
                    
                    if (leading != null && leading.getPoints() > 0) {
                        lore.add("§7En tête: " + leading.getDisplayName() + " §7(" + leading.getPoints() + " pts)");
                    }
                }
            }
            
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        
        return item;
    }
    
    private ItemStack createSideItem(WarSide side, int sideIndex) {
        // Utiliser différents matériaux selon l'index
        Material[] materials = {
            Material.RED_BANNER,
            Material.BLUE_BANNER, 
            Material.GREEN_BANNER,
            Material.YELLOW_BANNER
        };
        
        ItemStack item = new ItemStack(materials[Math.min(sideIndex, materials.length - 1)]);
        ItemMeta meta = item.getItemMeta();
        
        if (meta != null) {
            meta.setDisplayName(side.getDisplayName());
            
            List<String> lore = new ArrayList<>();
            lore.add("§7Points: §f" + side.getPoints() + "§7/§f" + war.getRequiredPoints());
            lore.add("§7Kills: §f" + side.getKills());
            lore.add("§7Nations: §f" + side.getNations().size());
            lore.add("");
            
            if (!side.getNations().isEmpty()) {
                lore.add("§6Nations participantes:");
                for (String nation : side.getNations()) {
                    lore.add("  §7• §f" + nation);
                }
                lore.add("");
            }
            
            // Barre de progression
            double progress = (double) side.getPoints() / war.getRequiredPoints();
            int filledBars = (int) (progress * 20);
            StringBuilder progressBar = new StringBuilder("§7[");
            for (int i = 0; i < 20; i++) {
                if (i < filledBars) {
                    progressBar.append("§a█");
                } else {
                    progressBar.append("§8█");
                }
            }
            progressBar.append("§7] §f").append(String.format("%.1f", progress * 100)).append("%");
            
            lore.add(progressBar.toString());
            
            // Indicateur de victoire potentielle
            if (side.getPoints() >= war.getRequiredPoints()) {
                lore.add("§a§l🏆 VAINQUEUR !");
            } else {
                int remaining = war.getRequiredPoints() - side.getPoints();
                lore.add("§7Reste §f" + remaining + " §7points pour gagner");
            }
            
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        
        return item;
    }
    
    private ItemStack createGlobalStatsItem() {
        ItemStack item = new ItemStack(Material.COMPARATOR);
        ItemMeta meta = item.getItemMeta();
        
        if (meta != null) {
            meta.setDisplayName("§b§lStatistiques Globales");
            
            List<String> lore = new ArrayList<>();
            
            // Calculs globaux
            int totalKills = war.getSides().stream().mapToInt(WarSide::getKills).sum();
            int totalNations = war.getSides().stream().mapToInt(s -> s.getNations().size()).sum();
            int totalPoints = war.getSides().stream().mapToInt(WarSide::getPoints).sum();
            
            lore.add("§7Camps total: §f" + war.getSides().size());
            lore.add("§7Nations impliquées: §f" + totalNations);
            lore.add("§7Kills total: §f" + totalKills);
            lore.add("§7Points total: §f" + totalPoints);
            lore.add("");
            
            if (war.getStartDate() != null) {
                long daysSinceStart = java.time.Duration.between(war.getStartDate(), java.time.LocalDateTime.now()).toDays();
                lore.add("§7Durée: §f" + daysSinceStart + " jour(s)");
                
                if (totalKills > 0 && daysSinceStart > 0) {
                    double killsPerDay = (double) totalKills / daysSinceStart;
                    lore.add("§7Kills/jour: §f" + String.format("%.1f", killsPerDay));
                }
            }
            
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        
        return item;
    }
    
    private ItemStack createProgressionItem() {
        ItemStack item = new ItemStack(Material.EXPERIENCE_BOTTLE);
        ItemMeta meta = item.getItemMeta();
        
        if (meta != null) {
            meta.setDisplayName("§d§lProgression de la Guerre");
            
            List<String> lore = new ArrayList<>();
            
            if (!war.getSides().isEmpty()) {
                // Trier les camps par points
                List<WarSide> sortedSides = new ArrayList<>(war.getSides());
                sortedSides.sort((s1, s2) -> Integer.compare(s2.getPoints(), s1.getPoints()));
                
                lore.add("§7Classement actuel:");
                for (int i = 0; i < sortedSides.size(); i++) {
                    WarSide side = sortedSides.get(i);
                    String position = "";
                    switch (i) {
                        case 0: position = "§61er"; break;
                        case 1: position = "§72ème"; break;
                        case 2: position = "§c3ème"; break;
                        default: position = "§8" + (i + 1) + "ème"; break;
                    }
                    
                    lore.add("  " + position + " §7- " + side.getDisplayName() + " §7(" + side.getPoints() + " pts)");
                }
                
                lore.add("");
                
                // Progression vers la victoire
                WarSide leader = sortedSides.get(0);
                if (leader.getPoints() > 0) {
                    double leaderProgress = (double) leader.getPoints() / war.getRequiredPoints() * 100;
                    lore.add("§7Progression du leader: §f" + String.format("%.1f", leaderProgress) + "%");
                    
                    if (leaderProgress < 100) {
                        int remaining = war.getRequiredPoints() - leader.getPoints();
                        lore.add("§7Points restants: §f" + remaining);
                    }
                }
            }
            
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        
        return item;
    }
    
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        String title = event.getView().getTitle();
        if (!title.startsWith(MessageUtils.getMessageRaw("gui.war_stats"))) {
            return;
        }
        
        event.setCancelled(true);
        
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }
        
        Player player = (Player) event.getWhoClicked();
        ItemStack item = event.getCurrentItem();
        
        if (item == null || item.getType() == Material.AIR) {
            return;
        }
        
        // Bouton retour
        if (item.getType() == Material.ARROW) {
            player.closeInventory();
            new WarSelectionGUI(plugin).openGUI(player);
        }
        
        // Bouton actualiser
        if (item.getType() == Material.CLOCK) {
            // Recharger la guerre depuis la base de données
            War refreshedWar = plugin.getWarDataManager().getWar(war.getId());
            if (refreshedWar != null) {
                player.closeInventory();
                new WarStatsGUI(plugin, refreshedWar).openGUI(player);
                player.sendMessage("§aStatistiques actualisées !");
            } else {
                player.sendMessage("§cErreur lors de l'actualisation");
            }
        }
    }
}