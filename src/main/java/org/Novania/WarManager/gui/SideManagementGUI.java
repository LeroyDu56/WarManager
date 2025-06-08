package org.Novania.WarManager.gui;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.Novania.WarManager.WarManager;
import org.Novania.WarManager.models.War;
import org.Novania.WarManager.models.WarSide;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class SideManagementGUI implements Listener {
    
    private final WarManager plugin;
    private final War war;
    private static final Map<UUID, String> awaitingInput = new HashMap<>();
    private static final Map<UUID, String> awaitingColor = new HashMap<>();
    
    public SideManagementGUI(WarManager plugin, War war) {
        this.plugin = plugin;
        this.war = war;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }
    
    public void openGUI(Player player) {
        int size = 54; // Taille plus grande pour plus d'options
        
        Inventory inv = Bukkit.createInventory(null, size, "§cGestion des camps - Guerre #" + war.getId());
        
        // Informations sur la guerre
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
        inv.setItem(4, warInfo);
        
        // Bouton pour ajouter un camp
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
        inv.setItem(10, addSide);
        
        // Afficher les camps existants
        List<WarSide> sides = war.getSides();
        int[] sideSlots = {19, 21, 23, 25, 28, 30, 32, 34}; // 8 emplacements max
        
        for (int i = 0; i < Math.min(sides.size(), 8); i++) {
            WarSide side = sides.get(i);
            ItemStack sideItem = createSideManagementItem(side);
            inv.setItem(sideSlots[i], sideItem);
        }
        
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
        
        // Boutons d'aide
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
        
        player.openInventory(inv);
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
            
            // Couleur de bannière selon le camp
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
            
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        
        return item;
    }
    
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!event.getView().getTitle().startsWith("§cGestion des camps")) {
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
            new NationSelectionGUI(plugin, war).openGUI(player);
            return;
        }
        
        // Bouton ajouter un camp
        if (item.getType() == Material.EMERALD_BLOCK) {
            player.closeInventory();
            awaitingInput.put(player.getUniqueId(), "side_name");
            player.sendMessage("§e§l▶ Création d'un nouveau camp");
            player.sendMessage("§7Tapez le nom du camp dans le chat:");
            player.sendMessage("§8(Tapez 'annuler' pour annuler)");
            return;
        }
        
        // Gestion d'un camp existant
        if (item.getType().name().contains("BANNER")) {
            if (!item.hasItemMeta() || !item.getItemMeta().hasDisplayName()) {
                return;
            }
            
            String displayName = item.getItemMeta().getDisplayName();
            
            // Extraire le nom du camp depuis "§cNomCamp §7(§frealName§7)"
            String realName = "";
            if (displayName.contains("(") && displayName.contains(")")) {
                int start = displayName.lastIndexOf("(§f") + 3;
                int end = displayName.lastIndexOf("§7)");
                if (start > 2 && end > start) {
                    realName = displayName.substring(start, end);
                }
            }
            
            if (realName.isEmpty()) {
                player.sendMessage("§cErreur : Impossible d'identifier le camp");
                return;
            }
            
            WarSide side = war.getSideByName(realName);
            if (side != null) {
                if (event.getClick().isLeftClick()) {
                    // Supprimer le camp
                    if (!side.getNations().isEmpty()) {
                        player.sendMessage("§cImpossible de supprimer un camp qui contient des nations !");
                        player.sendMessage("§7Retirez d'abord toutes les nations du camp " + side.getDisplayName());
                        return;
                    }
                    
                    plugin.getWarDataManager().removeSideFromWar(war.getId(), side.getName());
                    player.sendMessage("§aCamp " + side.getDisplayName() + " §asupprimé");
                    plugin.getLogger().info(player.getName() + " a supprimé le camp " + side.getName() + " (guerre #" + war.getId() + ")");
                    
                    // Rafraîchir le GUI
                    openGUI(player);
                    
                } else if (event.getClick().isRightClick()) {
                    // Modifier le camp (futur - pour l'instant juste un message)
                    player.sendMessage("§7Modification des camps pas encore implémentée");
                    player.sendMessage("§7Utilisez §e/waradmin §7pour modifier les points");
                }
            }
        }
    }
    
    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();
        String message = event.getMessage().trim();
        
        // Vérifier si le joueur est en attente d'input
        if (!awaitingInput.containsKey(playerId) && !awaitingColor.containsKey(playerId)) {
            return;
        }
        
        event.setCancelled(true);
        
        // Annulation
        if (message.equalsIgnoreCase("annuler") || message.equalsIgnoreCase("cancel")) {
            awaitingInput.remove(playerId);
            awaitingColor.remove(playerId);
            player.sendMessage("§cCréation de camp annulée");
            
            // Rouvrir le GUI après un délai
            Bukkit.getScheduler().runTask(plugin, () -> {
                new SideManagementGUI(plugin, war).openGUI(player);
            });
            return;
        }
        
        // Étape 1: Nom du camp
        if (awaitingInput.containsKey(playerId) && awaitingInput.get(playerId).equals("side_name")) {
            if (message.length() < 2 || message.length() > 20) {
                player.sendMessage("§cLe nom doit faire entre 2 et 20 caractères !");
                player.sendMessage("§7Tapez le nom du camp:");
                return;
            }
            
            // Vérifier que le nom n'existe pas déjà
            for (WarSide existingSide : war.getSides()) {
                if (existingSide.getName().equalsIgnoreCase(message)) {
                    player.sendMessage("§cUn camp avec ce nom existe déjà !");
                    player.sendMessage("§7Tapez un autre nom:");
                    return;
                }
            }
            
            awaitingInput.remove(playerId);
            awaitingColor.put(playerId, message); // Stocker le nom temporairement
            
            player.sendMessage("§a✓ Nom du camp: §f" + message);
            player.sendMessage("§7Maintenant, choisissez une couleur:");
            player.sendMessage("§cr §7(rouge), §9b §7(bleu), §ag §7(vert), §ey §7(jaune)");
            player.sendMessage("§6o §7(orange), §5p §7(violet), §bc §7(cyan), §fw §7(blanc)");
            player.sendMessage("§0k §7(noir), §7gray §7(gris)");
            player.sendMessage("§8(Tapez la lettre ou le nom de la couleur)");
            return;
        }
        
        // Étape 2: Couleur du camp
        if (awaitingColor.containsKey(playerId)) {
            String sideName = awaitingColor.get(playerId);
            String color = getColorFromInput(message);
            
            if (color == null) {
                player.sendMessage("§cCouleur invalide ! Couleurs disponibles:");
                player.sendMessage("§cr §9b §ag §ey §6o §5p §bc §fw §0k §7gray");
                return;
            }
            
            awaitingColor.remove(playerId);
            
            // Créer le camp
            Bukkit.getScheduler().runTask(plugin, () -> {
                plugin.getWarDataManager().addSideToWar(war.getId(), sideName, color);
                player.sendMessage("§a✓ Camp " + color + sideName + " §acréé avec succès !");
                plugin.getLogger().info(player.getName() + " a créé le camp " + sideName + " (guerre #" + war.getId() + ")");
                
                // Rouvrir le GUI
                new SideManagementGUI(plugin, war).openGUI(player);
            });
        }
    }
    
    private String getColorFromInput(String input) {
        switch (input.toLowerCase()) {
            case "r": case "red": case "rouge": return "§c";
            case "b": case "blue": case "bleu": return "§9";
            case "g": case "green": case "vert": return "§a";
            case "y": case "yellow": case "jaune": return "§e";
            case "o": case "orange": return "§6";
            case "p": case "purple": case "violet": return "§5";
            case "c": case "cyan": return "§b";
            case "w": case "white": case "blanc": return "§f";
            case "k": case "black": case "noir": return "§0";
            case "gray": case "grey": case "gris": return "§7";
            default: return null;
        }
    }
    
    // Nettoyer les maps quand le joueur se déconnecte
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        UUID playerId = event.getPlayer().getUniqueId();
        awaitingInput.remove(playerId);
        awaitingColor.remove(playerId);
    }
}