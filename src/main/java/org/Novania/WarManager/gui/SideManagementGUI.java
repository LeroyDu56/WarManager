package org.Novania.WarManager.gui;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

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

public class SideManagementGUI {
    
    private final WarManager plugin;
    private final War war;
    private static final long CHAT_TIMEOUT = 30000; // 30 secondes
    
    public SideManagementGUI(WarManager plugin, War war) {
        this.plugin = plugin;
        this.war = war;
    }
    
    public void openGUI(Player player) {
        // NOUVEAU: Enregistrer le contexte de guerre pour ce joueur
        if (plugin.getGuiManager() != null) {
            plugin.getGuiManager().getPlayerWarContext().put(player.getUniqueId(), war.getId());
        }
        
        int size = 54;
        Inventory inv = Bukkit.createInventory(null, size, "§cGestion des camps - Guerre #" + war.getId());
        
        // Informations sur la guerre
        ItemStack warInfo = createWarInfoItem();
        inv.setItem(4, warInfo);
        
        // Bouton pour ajouter un camp
        ItemStack addSide = createAddSideButton();
        inv.setItem(10, addSide);
        
        // Afficher les camps existants
        displayExistingSides(inv);
        
        // Boutons utilitaires
        addUtilityButtons(inv);
        
        player.openInventory(inv);
    }
    
    private ItemStack createWarInfoItem() {
        ItemStack warInfo = new ItemStack(Material.BOOK);
        ItemMeta warMeta = warInfo.getItemMeta();
        if (warMeta != null) {
            warMeta.setDisplayName("§e§lGuerre: §f" + war.getName());
            List<String> warLore = new ArrayList<>();
            warLore.add("§7ID: §f" + war.getId());
            warLore.add("§7Type: §f" + war.getCasusBeliType());
            warLore.add("§7Points requis: §f" + war.getRequiredPoints());
            warLore.add("§7Camps actuels: §f" + war.getSides().size());
            warMeta.setLore(warLore);
            warInfo.setItemMeta(warMeta);
        }
        return warInfo;
    }
    
    private ItemStack createAddSideButton() {
        ItemStack addSide = new ItemStack(Material.EMERALD_BLOCK);
        ItemMeta addMeta = addSide.getItemMeta();
        if (addMeta != null) {
            addMeta.setDisplayName("§a§l+ Ajouter un Camp");
            List<String> addLore = new ArrayList<>();
            addLore.add("§7Créer un nouveau camp pour cette guerre");
            addLore.add("§e§lClic pour commencer");
            addMeta.setLore(addLore);
            addSide.setItemMeta(addMeta);
        }
        return addSide;
    }
    
    private void displayExistingSides(Inventory inv) {
        List<WarSide> sides = war.getSides();
        int[] sideSlots = {19, 21, 23, 25, 28, 30, 32, 34}; // 8 emplacements max
        
        for (int i = 0; i < Math.min(sides.size(), 8); i++) {
            WarSide side = sides.get(i);
            ItemStack sideItem = createSideManagementItem(side);
            inv.setItem(sideSlots[i], sideItem);
        }
    }
    
    private ItemStack createSideManagementItem(WarSide side) {
        ItemStack item = new ItemStack(Material.WHITE_BANNER);
        ItemMeta meta = item.getItemMeta();
        
        if (meta != null) {
            meta.setDisplayName(side.getDisplayName() + " §7(§f" + side.getName() + "§7)");
            
            List<String> lore = new ArrayList<>();
            lore.add("§7Points: §f" + side.getPoints() + "/" + war.getRequiredPoints());
            lore.add("§7Kills: §f" + side.getKills());
            lore.add("§7Nations: §f" + side.getNations().size());
            lore.add("");
            
            if (!side.getNations().isEmpty()) {
                lore.add("§6Nations dans ce camp:");
                for (String nation : side.getNations()) {
                    lore.add("  §7- §f" + nation);
                }
                lore.add("");
            }
            
            lore.add("§c§lClic gauche §7pour supprimer");
            lore.add("§e§lClic droit §7pour modifier");
            
            setBannerColorByCode(item, side.getColor());
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
    
    private void addUtilityButtons(Inventory inv) {
        // Bouton retour
        ItemStack backButton = new ItemStack(Material.ARROW);
        ItemMeta backMeta = backButton.getItemMeta();
        if (backMeta != null) {
            backMeta.setDisplayName("§c← Retour");
            List<String> backLore = new ArrayList<>();
            backLore.add("§7Retourner à la gestion des nations");
            backMeta.setLore(backLore);
            backButton.setItemMeta(backMeta);
        }
        inv.setItem(49, backButton);
        
        // Bouton d'aide
        ItemStack helpItem = new ItemStack(Material.PAPER);
        ItemMeta helpMeta = helpItem.getItemMeta();
        if (helpMeta != null) {
            helpMeta.setDisplayName("§b§lAide");
            List<String> helpLore = new ArrayList<>();
            helpLore.add("§7Gestion des camps:");
            helpLore.add("§e• Clic gauche §7sur un camp pour le supprimer");
            helpLore.add("§e• Clic sur + §7pour ajouter un camp");
            helpLore.add("");
            helpLore.add("§7Couleurs disponibles:");
            helpLore.add("§cr §9b §ag §ey §6o §5p §bc §fw §0k §7gray");
            helpMeta.setLore(helpLore);
            helpItem.setItemMeta(helpMeta);
        }
        inv.setItem(8, helpItem);
    }
    
    public static void handleClickStatic(InventoryClickEvent event, Player player, WarManager plugin) {
        if (!event.getView().getTitle().startsWith("§cGestion des camps")) {
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
        
        // Bouton retour
        if (item.getType() == Material.ARROW) {
            Bukkit.getScheduler().runTask(plugin, () -> {
                // Récupérer l'ID de guerre depuis le titre
                String title = event.getView().getTitle();
                int warId = extractWarIdFromTitle(title);
                if (warId != -1) {
                    War war = plugin.getWarDataManager().getWar(warId);
                    if (war != null) {
                        new NationSelectionGUI(plugin, war).openGUI(player);
                        return;
                    }
                }
                player.sendMessage("§eRetour à la gestion des nations");
            });
            return;
        }
        
        // Bouton ajouter un camp
        if (item.getType() == Material.EMERALD_BLOCK) {
            if (plugin.getGuiManager() != null) {
                Map<UUID, String> awaitingInput = plugin.getGuiManager().getAwaitingInput();
                awaitingInput.put(player.getUniqueId(), "side_name");
                player.sendMessage("§e§l▶ Création d'un nouveau camp");
                player.sendMessage("§7Tapez le nom du camp dans le chat:");
                player.sendMessage("§8(Tapez 'annuler' pour annuler)");
                
                // Nettoyer après timeout
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    awaitingInput.remove(player.getUniqueId());
                }, CHAT_TIMEOUT / 50); // Convertir en ticks
            }
            return;
        }
        
        // Gestion d'un camp existant
        if (item.getType().name().contains("BANNER")) {
            handleSideManagement(event, player, item, plugin);
        }
    }
    
    private static void handleSideManagement(InventoryClickEvent event, Player player, ItemStack item, WarManager plugin) {
        if (!item.hasItemMeta() || !item.getItemMeta().hasDisplayName()) {
            return;
        }
        
        String displayName = item.getItemMeta().getDisplayName();
        String realName = extractRealSideName(displayName);
        
        if (realName.isEmpty()) {
            player.sendMessage("§cErreur : Impossible d'identifier le camp");
            return;
        }
        
        // Traitement asynchrone
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            // Récupérer la guerre depuis le titre de l'inventaire
            String title = event.getView().getTitle();
            int warId = extractWarIdFromTitle(title);
            
            if (warId == -1) {
                player.sendMessage("§cErreur : Impossible de déterminer la guerre");
                return;
            }
            
            War war = plugin.getWarDataManager().getWar(warId);
            if (war == null) {
                player.sendMessage("§cErreur : Guerre introuvable");
                return;
            }
            
            WarSide side = war.getSideByName(realName);
            if (side != null) {
                if (event.getClick().isLeftClick()) {
                    handleSideRemoval(player, side, war, plugin);
                } else if (event.getClick().isRightClick()) {
                    player.sendMessage("§7Modification des camps pas encore implémentée");
                }
            }
        });
    }
    
    private static String extractRealSideName(String displayName) {
        if (displayName.contains("(§f") && displayName.contains("§7)")) {
            int start = displayName.lastIndexOf("(§f") + 3;
            int end = displayName.lastIndexOf("§7)");
            if (start > 2 && end > start) {
                return displayName.substring(start, end);
            }
        }
        return "";
    }
    
    private static int extractWarIdFromTitle(String title) {
        try {
            if (title.contains("Guerre #")) {
                String[] parts = title.split("Guerre #");
                if (parts.length > 1) {
                    String idPart = parts[1].trim();
                    return Integer.parseInt(idPart);
                }
            }
        } catch (NumberFormatException e) {
            // Ignore parsing errors
        }
        return -1;
    }
    
    private static void handleSideRemoval(Player player, WarSide side, War war, WarManager plugin) {
        if (!side.getNations().isEmpty()) {
            Bukkit.getScheduler().runTask(plugin, () -> {
                player.sendMessage("§cImpossible de supprimer un camp qui contient des nations !");
                player.sendMessage("§7Retirez d'abord toutes les nations du camp " + side.getDisplayName());
            });
            return;
        }
        
        Bukkit.getScheduler().runTask(plugin, () -> {
            plugin.getWarDataManager().removeSideFromWar(war.getId(), side.getName());
            player.sendMessage("§aCamp " + side.getDisplayName() + " §asupprimé");
            String logMessage = player.getName() + " a supprimé le camp " + side.getName() + " (guerre #" + war.getId() + ")";
            plugin.getLogger().info(logMessage);
            
            // Rafraîchir le GUI
            new SideManagementGUI(plugin, war).openGUI(player);
        });
    }
    
    public static void handleChatInput(Player player, String message, String inputType, 
                                     Map<UUID, String> awaitingInput, Map<UUID, String> awaitingColor, 
                                     WarManager plugin) {
        if (!"side_name".equals(inputType)) return;
        
        if (message.length() < 2 || message.length() > 20) {
            player.sendMessage("§cLe nom doit faire entre 2 et 20 caractères !");
            return;
        }
        
        awaitingInput.remove(player.getUniqueId());
        awaitingColor.put(player.getUniqueId(), message);
        
        player.sendMessage("§a✓ Nom du camp: §f" + message);
        player.sendMessage("§7Maintenant, choisissez une couleur:");
        player.sendMessage("§cr §7(rouge), §9b §7(bleu), §ag §7(vert), §ey §7(jaune)");
        player.sendMessage("§6o §7(orange), §5p §7(violet), §bc §7(cyan), §fw §7(blanc)");
        player.sendMessage("§0k §7(noir), §7gray §7(gris)");
    }
    
    public static void handleColorInput(Player player, String message, String sideName, 
                                      Map<UUID, String> awaitingColor, WarManager plugin) {
        String color = getColorFromInput(message);
        if (color == null) {
            player.sendMessage("§cCouleur invalide ! Couleurs disponibles:");
            player.sendMessage("§cr §9b §ag §ey §6o §5p §bc §fw §0k §7gray");
            return;
        }
        
        awaitingColor.remove(player.getUniqueId());
        
        // CORRECTION: Utiliser le contexte de guerre du joueur
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            War war = null;
            
            // Essayer de récupérer depuis le contexte du joueur
            if (plugin.getGuiManager() != null) {
                Integer warId = plugin.getGuiManager().getPlayerWarContext().get(player.getUniqueId());
                if (warId != null) {
                    war = plugin.getWarDataManager().getWar(warId);
                    plugin.getLogger().info("DEBUG: Contexte guerre trouvé pour " + player.getName() + " : guerre #" + warId);
                }
            }
            
            // Fallback: prendre la première guerre active
            if (war == null) {
                for (War w : plugin.getWarDataManager().getActiveWars().values()) {
                    if (w.isActive()) {
                        war = w;
                        plugin.getLogger().info("DEBUG: Fallback guerre active : #" + w.getId());
                        break;
                    }
                }
            }
            
            if (war == null) {
                player.sendMessage("§cErreur : Aucune guerre active trouvée pour créer le camp");
                plugin.getLogger().warning("ERREUR: Aucune guerre trouvée pour créer le camp " + sideName);
                return;
            }
            
            final War finalWar = war;
            
            plugin.getLogger().info("DEBUG: Création du camp " + sideName + " dans la guerre #" + finalWar.getId());
            
            // VRAIMENT créer le camp en base de données
            plugin.getWarDataManager().addSideToWar(finalWar.getId(), sideName, color);
            
            Bukkit.getScheduler().runTask(plugin, () -> {
                player.sendMessage("§a✓ Camp " + color + sideName + " §acréé avec succès dans la guerre #" + finalWar.getId() + " !");
                String logMessage = player.getName() + " a créé le camp " + sideName + " dans la guerre #" + finalWar.getId();
                plugin.getLogger().info(logMessage);
                
                // Rouvrir le GUI de gestion des camps avec la guerre mise à jour
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    War updatedWar = plugin.getWarDataManager().getWar(finalWar.getId());
                    if (updatedWar != null) {
                        new SideManagementGUI(plugin, updatedWar).openGUI(player);
                    }
                }, 10L); // Petit délai pour que la DB soit mise à jour
            });
        });
    }
    
    private static String getColorFromInput(String input) {
        return switch (input.toLowerCase()) {
            case "r", "red", "rouge" -> "§c";
            case "b", "blue", "bleu" -> "§9";
            case "g", "green", "vert" -> "§a";
            case "y", "yellow", "jaune" -> "§e";
            case "o", "orange" -> "§6";
            case "p", "purple", "violet" -> "§5";
            case "c", "cyan" -> "§b";
            case "w", "white", "blanc" -> "§f";
            case "k", "black", "noir" -> "§0";
            case "gray", "grey", "gris" -> "§7";
            default -> null;
        };
    }
}