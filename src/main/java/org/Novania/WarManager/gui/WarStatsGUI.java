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
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class WarStatsGUI {
    
    private final WarManager plugin;
    private final War war;
    
    public WarStatsGUI(WarManager plugin, War war) {
        this.plugin = plugin;
        this.war = war;
    }
    
    public void openGUI(Player player) {
        // Construction asynchrone de l'interface
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            // Récupérer les données nécessaires
            War currentWar = plugin.getWarDataManager().getWar(war.getId());
            if (currentWar == null) {
                player.sendMessage("§cErreur : Guerre introuvable");
                return;
            }
            
            // Retour sur le thread principal pour créer l'inventaire
            Bukkit.getScheduler().runTask(plugin, () -> {
                Inventory inv = Bukkit.createInventory(null, 54, MessageUtils.getMessageRaw("gui.war_stats") + " - " + currentWar.getName());
                
                // Construction optimisée de l'interface
                buildInterface(inv, currentWar);
                player.openInventory(inv);
            });
        });
    }
    
    private void buildInterface(Inventory inv, War currentWar) {
        // Info générale de la guerre
        ItemStack warInfo = createWarInfoItem(currentWar);
        inv.setItem(4, warInfo);
        
        // Items pour chaque camp (positions optimisées)
        List<WarSide> sides = currentWar.getSides();
        int[] sidePositions = {19, 25, 37, 43}; // Positions symétriques
        
        for (int i = 0; i < Math.min(sides.size(), 4); i++) {
            WarSide side = sides.get(i);
            ItemStack sideItem = createSideItem(side, currentWar, i);
            inv.setItem(sidePositions[i], sideItem);
        }
        
        // Statistiques globales
        ItemStack globalStats = createGlobalStatsItem(currentWar);
        inv.setItem(13, globalStats);
        
        // Progression de la guerre
        ItemStack progression = createProgressionItem(currentWar);
        inv.setItem(31, progression);
        
        // Boutons utilitaires
        addUtilityButtons(inv);
    }
    
    private ItemStack createWarInfoItem(War currentWar) {
        ItemStack item = new ItemStack(Material.ENCHANTED_BOOK);
        ItemMeta meta = item.getItemMeta();
        
        if (meta != null) {
            meta.setDisplayName("§e§l" + currentWar.getName() + " §7(#" + currentWar.getId() + ")");
            
            List<String> lore = new ArrayList<>();
            lore.add("§7Casus Belli: §f" + currentWar.getCasusBeliType());
            lore.add("§7Points pour victoire: §f" + currentWar.getRequiredPoints());
            lore.add("§7Date de début: §f" + currentWar.getStartDate().toLocalDate());
            lore.add("§7Heure de début: §f" + currentWar.getStartDate().toLocalTime().toString().substring(0, 5));
            lore.add("");
            
            WarSide winner = currentWar.getWinner();
            if (winner != null) {
                lore.add("§a§l🏆 VICTOIRE: " + winner.getDisplayName());
                if (currentWar.getEndDate() != null) {
                    lore.add("§7Date de fin: §f" + currentWar.getEndDate().toLocalDate());
                }
            } else {
                lore.add("§7Statut: " + (currentWar.isActive() ? "§aEn cours" : "§cTerminée"));
                
                // Leader actuel
                if (!currentWar.getSides().isEmpty()) {
                    WarSide leader = currentWar.getSides().stream()
                            .max((s1, s2) -> Integer.compare(s1.getPoints(), s2.getPoints()))
                            .orElse(null);
                    
                    if (leader != null && leader.getPoints() > 0) {
                        lore.add("§7En tête: " + leader.getDisplayName() + " §7(" + leader.getPoints() + " pts)");
                    }
                }
            }
            
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        
        return item;
    }
    
    private ItemStack createSideItem(WarSide side, War currentWar, int sideIndex) {
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
            lore.add("§7Points: §f" + side.getPoints() + "§7/§f" + currentWar.getRequiredPoints());
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
            
            // Barre de progression optimisée
            double progress = (double) side.getPoints() / currentWar.getRequiredPoints();
            String progressBar = createProgressBar(progress);
            lore.add(progressBar);
            
            // Statut de victoire
            if (side.getPoints() >= currentWar.getRequiredPoints()) {
                lore.add("§a§l🏆 VAINQUEUR !");
            } else {
                int remaining = currentWar.getRequiredPoints() - side.getPoints();
                lore.add("§7Reste §f" + remaining + " §7points pour gagner");
            }
            
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        
        return item;
    }
    
    private String createProgressBar(double progress) {
        int filledBars = (int) (progress * 20);
        StringBuilder progressBar = new StringBuilder("§7[");
        for (int i = 0; i < 20; i++) {
            progressBar.append(i < filledBars ? "§a█" : "§8█");
        }
        progressBar.append("§7] §f").append(String.format("%.1f", progress * 100)).append("%");
        return progressBar.toString();
    }
    
    private ItemStack createGlobalStatsItem(War currentWar) {
        ItemStack item = new ItemStack(Material.COMPARATOR);
        ItemMeta meta = item.getItemMeta();
        
        if (meta != null) {
            meta.setDisplayName("§b§lStatistiques Globales");
            
            List<String> lore = new ArrayList<>();
            
            int totalKills = currentWar.getSides().stream().mapToInt(WarSide::getKills).sum();
            int totalNations = currentWar.getSides().stream().mapToInt(s -> s.getNations().size()).sum();
            int totalPoints = currentWar.getSides().stream().mapToInt(WarSide::getPoints).sum();
            
            lore.add("§7Camps total: §f" + currentWar.getSides().size());
            lore.add("§7Nations impliquées: §f" + totalNations);
            lore.add("§7Kills total: §f" + totalKills);
            lore.add("§7Points total: §f" + totalPoints);
            lore.add("");
            
            if (currentWar.getStartDate() != null) {
                long daysSinceStart = java.time.Duration.between(currentWar.getStartDate(), java.time.LocalDateTime.now()).toDays();
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
    
    private ItemStack createProgressionItem(War currentWar) {
        ItemStack item = new ItemStack(Material.EXPERIENCE_BOTTLE);
        ItemMeta meta = item.getItemMeta();
        
        if (meta != null) {
            meta.setDisplayName("§d§lProgression de la Guerre");
            
            List<String> lore = new ArrayList<>();
            
            if (!currentWar.getSides().isEmpty()) {
                // Trier les camps par points
                List<WarSide> sortedSides = new ArrayList<>(currentWar.getSides());
                sortedSides.sort((s1, s2) -> Integer.compare(s2.getPoints(), s1.getPoints()));
                
                lore.add("§7Classement actuel:");
                String[] positions = {"§61er", "§72ème", "§c3ème", "§84ème"};
                
                for (int i = 0; i < sortedSides.size(); i++) {
                    WarSide side = sortedSides.get(i);
                    String position = i < positions.length ? positions[i] : "§8" + (i + 1) + "ème";
                    lore.add("  " + position + " §7- " + side.getDisplayName() + " §7(" + side.getPoints() + " pts)");
                }
                
                lore.add("");
                
                // Progression vers la victoire
                WarSide leader = sortedSides.get(0);
                if (leader.getPoints() > 0) {
                    double leaderProgress = (double) leader.getPoints() / currentWar.getRequiredPoints() * 100;
                    lore.add("§7Progression du leader: §f" + String.format("%.1f", leaderProgress) + "%");
                    
                    if (leaderProgress < 100) {
                        int remaining = currentWar.getRequiredPoints() - leader.getPoints();
                        lore.add("§7Points restants: §f" + remaining);
                    }
                }
            }
            
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        
        return item;
    }
    
    private void addUtilityButtons(Inventory inv) {
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
    }
    
    public static void handleClickStatic(InventoryClickEvent event, Player player, WarManager plugin) {
        String title = event.getView().getTitle();
        if (!title.startsWith(MessageUtils.getMessageRaw("gui.war_stats"))) {
            return;
        }
        
        ItemStack item = event.getCurrentItem();
        if (item == null || item.getType() == Material.AIR) {
            return;
        }
        
        // Fermer l'inventaire immédiatement
        player.closeInventory();
        
        // Traitement selon le type de bouton
        if (item.getType() == Material.ARROW) {
            // Bouton retour
            Bukkit.getScheduler().runTask(plugin, () -> {
                new WarSelectionGUI(plugin).openGUI(player);
            });
        } else if (item.getType() == Material.CLOCK) {
            // Bouton actualiser
            handleRefresh(title, player, plugin);
        }
    }
    
    private static void handleRefresh(String title, Player player, WarManager plugin) {
        // Extraire l'ID de guerre depuis le titre
        try {
            String warName = title.replace(MessageUtils.getMessageRaw("gui.war_stats") + " - ", "");
            
            // Recherche asynchrone de la guerre
            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                War refreshedWar = null;
                for (War war : plugin.getWarDataManager().getActiveWars().values()) {
                    if (war.getName().equals(warName)) {
                        refreshedWar = plugin.getWarDataManager().getWar(war.getId());
                        break;
                    }
                }
                
                final War finalWar = refreshedWar;
                Bukkit.getScheduler().runTask(plugin, () -> {
                    if (finalWar != null) {
                        new WarStatsGUI(plugin, finalWar).openGUI(player);
                        player.sendMessage("§aStatistiques actualisées !");
                    } else {
                        player.sendMessage("§cErreur lors de l'actualisation");
                    }
                });
            });
            
        } catch (Exception e) {
            player.sendMessage("§cErreur lors de l'actualisation");
            plugin.getLogger().warning("Erreur lors de l'actualisation des stats: " + e.getMessage());
        }
    }
}