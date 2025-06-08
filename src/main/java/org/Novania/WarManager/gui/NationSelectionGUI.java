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

import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.object.Nation;

public class NationSelectionGUI {
    
    private final WarManager plugin;
    private final War war;
    private int currentPage;
    private static final int NATIONS_PER_PAGE = 45;
    
    public NationSelectionGUI(WarManager plugin, War war) {
        this(plugin, war, 0);
    }
    
    public NationSelectionGUI(WarManager plugin, War war, int page) {
        this.plugin = plugin;
        this.war = war;
        this.currentPage = page;
    }
    
    public void openGUI(Player player) {
        // NOUVEAU: Enregistrer le contexte de guerre pour ce joueur
        if (plugin.getGuiManager() != null) {
            plugin.getGuiManager().getPlayerWarContext().put(player.getUniqueId(), war.getId());
        }
        
        // Récupération asynchrone des nations
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            List<Nation> allNations = new ArrayList<>(TownyAPI.getInstance().getNations());
            int totalPages = (int) Math.ceil((double) allNations.size() / NATIONS_PER_PAGE);
            
            // Retour sur le thread principal
            Bukkit.getScheduler().runTask(plugin, () -> {
                // S'assurer que la page est valide
                if (currentPage < 0) currentPage = 0;
                if (currentPage >= totalPages) currentPage = Math.max(0, totalPages - 1);
                
                Inventory inv = Bukkit.createInventory(null, 54, MessageUtils.getMessageRaw("gui.nation_selection") + " - Page " + (currentPage + 1) + " - Guerre #" + war.getId());
                
                // Calculer les nations à afficher
                int startIndex = currentPage * NATIONS_PER_PAGE;
                int endIndex = Math.min(startIndex + NATIONS_PER_PAGE, allNations.size());
                
                // Ajouter les nations (slots 0-44)
                int slot = 0;
                for (int i = startIndex; i < endIndex; i++) {
                    Nation nation = allNations.get(i);
                    ItemStack item = createNationItem(nation);
                    inv.setItem(slot++, item);
                }
                
                addNavigationButtons(inv, totalPages, startIndex, endIndex, allNations.size());
                player.openInventory(inv);
            });
        });
    }
    
    private void addNavigationButtons(Inventory inv, int totalPages, int startIndex, int endIndex, int totalNations) {
        // Bouton page précédente
        if (currentPage > 0) {
            ItemStack prevButton = new ItemStack(Material.ARROW);
            ItemMeta prevMeta = prevButton.getItemMeta();
            if (prevMeta != null) {
                prevMeta.setDisplayName("§e← Page Précédente");
                List<String> prevLore = new ArrayList<>();
                prevLore.add("§7Page " + currentPage + "/" + totalPages);
                prevMeta.setLore(prevLore);
                prevButton.setItemMeta(prevMeta);
            }
            inv.setItem(45, prevButton);
        }
        
        // Info page actuelle
        ItemStack pageInfo = new ItemStack(Material.BOOK);
        ItemMeta pageInfoMeta = pageInfo.getItemMeta();
        if (pageInfoMeta != null) {
            pageInfoMeta.setDisplayName("§6Page " + (currentPage + 1) + " / " + Math.max(1, totalPages));
            List<String> pageInfoLore = new ArrayList<>();
            pageInfoLore.add("§7Nations " + (startIndex + 1) + "-" + endIndex + " sur " + totalNations);
            pageInfoLore.add("§7Guerre: §f" + war.getName());
            pageInfoMeta.setLore(pageInfoLore);
            pageInfo.setItemMeta(pageInfoMeta);
        }
        inv.setItem(49, pageInfo);
        
        // Bouton page suivante
        if (currentPage < totalPages - 1) {
            ItemStack nextButton = new ItemStack(Material.ARROW);
            ItemMeta nextMeta = nextButton.getItemMeta();
            if (nextMeta != null) {
                nextMeta.setDisplayName("§ePage Suivante →");
                List<String> nextLore = new ArrayList<>();
                nextLore.add("§7Page " + (currentPage + 2) + "/" + totalPages);
                nextMeta.setLore(nextLore);
                nextButton.setItemMeta(nextMeta);
            }
            inv.setItem(53, nextButton);
        }
        
        // Bouton retour et gestion des camps
        addUtilityButtons(inv);
    }
    
    private void addUtilityButtons(Inventory inv) {
        // Bouton retour
        ItemStack backButton = new ItemStack(Material.RED_STAINED_GLASS_PANE);
        ItemMeta backMeta = backButton.getItemMeta();
        if (backMeta != null) {
            backMeta.setDisplayName("§c← Retour aux guerres");
            backButton.setItemMeta(backMeta);
        }
        inv.setItem(47, backButton);
        
        // Bouton gestion des camps
        ItemStack manageSides = new ItemStack(Material.EMERALD_BLOCK);
        ItemMeta manageMeta = manageSides.getItemMeta();
        if (manageMeta != null) {
            manageMeta.setDisplayName("§a⚙ Gérer les camps");
            List<String> manageLore = new ArrayList<>();
            manageLore.add("§7Camps actuels: §f" + war.getSides().size());
            manageMeta.setLore(manageLore);
            manageSides.setItemMeta(manageMeta);
        }
        inv.setItem(51, manageSides);
    }
    
    private ItemStack createNationItem(Nation nation) {
        ItemStack item = new ItemStack(Material.EMERALD);
        ItemMeta meta = item.getItemMeta();
        
        if (meta != null) {
            meta.setDisplayName("§a" + nation.getName());
            
            List<String> lore = new ArrayList<>();
            lore.add("§7Villes: §f" + nation.getTowns().size());
            lore.add("§7Résidents: §f" + nation.getResidents().size());
            lore.add("");
            
            // Vérifier si la nation est déjà dans la guerre
            WarSide currentSide = getCurrentSide(nation.getName());
            if (currentSide != null) {
                lore.add("§6✓ Dans le camp: " + currentSide.getDisplayName());
                lore.add("§e§lClic pour changer de camp");
                item.setType(Material.GOLD_INGOT);
            } else {
                if (war.getSides().isEmpty()) {
                    lore.add("§c✗ Aucun camp disponible");
                    item.setType(Material.REDSTONE);
                } else {
                    lore.add("§e§lClic pour ajouter à un camp");
                }
            }
            
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        
        return item;
    }
    
    private WarSide getCurrentSide(String nationName) {
        for (WarSide side : war.getSides()) {
            if (side.hasNation(nationName)) {
                return side;
            }
        }
        return null;
    }
    
    public static void handleClickStatic(InventoryClickEvent event, Player player, WarManager plugin) {
        String title = event.getView().getTitle();
        if (!title.startsWith(MessageUtils.getMessageRaw("gui.nation_selection"))) {
            return;
        }
        
        // PROTECTION: S'assurer que l'événement est annulé
        event.setCancelled(true);
        
        ItemStack item = event.getCurrentItem();
        if (item == null || item.getType() == Material.AIR) {
            return;
        }
        
        int slot = event.getSlot();
        
        // Fermer immédiatement l'inventaire
        player.closeInventory();
        
        // Extraire les informations depuis le titre
        int currentPage = 0;
        int warId = -1;
        
        // Format: "Sélection des nations - Page X - Guerre #Y"
        if (title.contains(" - Page ") && title.contains(" - Guerre #")) {
            String[] parts = title.split(" - ");
            for (String part : parts) {
                if (part.startsWith("Page ")) {
                    try {
                        currentPage = Integer.parseInt(part.substring(5)) - 1; // -1 car base 1 vers base 0
                    } catch (NumberFormatException e) {
                        currentPage = 0;
                    }
                } else if (part.startsWith("Guerre #")) {
                    try {
                        warId = Integer.parseInt(part.substring(8));
                    } catch (NumberFormatException e) {
                        warId = -1;
                    }
                }
            }
        }
        
        // Récupérer la guerre
        final int finalWarId = warId;
        final int finalCurrentPage = currentPage;
        
        // DEBUG
        plugin.getLogger().info("=== DEBUG NAVIGATION ===");
        plugin.getLogger().info("Titre: " + title);
        plugin.getLogger().info("Slot cliqué: " + slot);
        plugin.getLogger().info("Type d'item: " + item.getType());
        plugin.getLogger().info("Page actuelle extraite: " + finalCurrentPage);
        plugin.getLogger().info("War ID extrait: " + finalWarId);
        
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            War war = null;
            
            if (finalWarId != -1) {
                war = plugin.getWarDataManager().getWar(finalWarId);
            }
            
            if (war == null) {
                war = findWarForContext(plugin);
            }
            
            if (war == null) {
                player.sendMessage("§cErreur : Aucune guerre active trouvée");
                return;
            }
            
            final War finalWar = war;
            
            Bukkit.getScheduler().runTask(plugin, () -> {
                // Calculer le nombre total de pages pour les vérifications
                List<Nation> allNations = new ArrayList<>(TownyAPI.getInstance().getNations());
                int totalPages = Math.max(1, (int) Math.ceil((double) allNations.size() / NATIONS_PER_PAGE));
                
                // Navigation - Page précédente
                if (slot == 45 && item.getType() == Material.ARROW) {
                    int newPage = Math.max(0, finalCurrentPage - 1);
                    plugin.getLogger().info("Navigation page précédente: " + finalCurrentPage + " -> " + newPage);
                    new NationSelectionGUI(plugin, finalWar, newPage).openGUI(player);
                    return;
                }
                
                // Navigation - Page suivante
                if (slot == 53 && item.getType() == Material.ARROW) {
                    int newPage = Math.min(totalPages - 1, finalCurrentPage + 1);
                    plugin.getLogger().info("Navigation page suivante: " + finalCurrentPage + " -> " + newPage + " (max: " + (totalPages - 1) + ")");
                    new NationSelectionGUI(plugin, finalWar, newPage).openGUI(player);
                    return;
                }
                
                // Bouton retour
                if (slot == 47 && item.getType() == Material.RED_STAINED_GLASS_PANE) {
                    new WarSelectionGUI(plugin, true).openGUI(player);
                    return;
                }
                
                // Bouton gérer les camps
                if (slot == 51 && item.getType() == Material.EMERALD_BLOCK) {
                    new SideManagementGUI(plugin, finalWar).openGUI(player);
                    return;
                }
                
                // Sélection d'une nation (slots 0-44)
                if (slot < 45 && item.hasItemMeta() && item.getItemMeta() != null && item.getItemMeta().hasDisplayName()) {
                    String nationName = item.getItemMeta().getDisplayName().substring(2); // Enlever "§a"
                    
                    // Ouvrir le GUI de sélection de camp pour cette nation
                    new SideSelectionGUI(plugin, finalWar, nationName, player).openGUI(player);
                }
            });
        });
    }
    
    private static War findWarForContext(WarManager plugin) {
        // Retourner la première guerre active
        return plugin.getWarDataManager().getActiveWars().values().stream().findFirst().orElse(null);
    }
}