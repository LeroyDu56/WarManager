package org.Novania.WarManager.gui;

import org.Novania.WarManager.WarManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class GUIManager implements Listener {
    
    private final WarManager plugin;
    private final Map<UUID, Long> lastClick = new ConcurrentHashMap<>();
    private final Set<UUID> processingPlayers = ConcurrentHashMap.newKeySet();
    private final Map<UUID, String> awaitingInput = new ConcurrentHashMap<>();
    private final Map<UUID, String> awaitingColor = new ConcurrentHashMap<>();
    
    private static final long CLICK_COOLDOWN = 250; // 250ms entre les clics
    
    public GUIManager(WarManager plugin) {
        this.plugin = plugin;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }
    
    /**
     * Vérifie si un joueur peut cliquer (anti-spam)
     */
    public boolean canClick(Player player) {
        UUID uuid = player.getUniqueId();
        long now = System.currentTimeMillis();
        
        // Vérifier le cooldown
        Long lastClickTime = lastClick.get(uuid);
        if (lastClickTime != null && (now - lastClickTime) < CLICK_COOLDOWN) {
            return false;
        }
        
        // Vérifier si déjà en traitement
        if (processingPlayers.contains(uuid)) {
            return false;
        }
        
        lastClick.put(uuid, now);
        return true;
    }
    
    /**
     * Marque un joueur comme en traitement
     */
    public void setProcessing(UUID uuid, boolean processing) {
        if (processing) {
            processingPlayers.add(uuid);
        } else {
            processingPlayers.remove(uuid);
        }
    }
    
    /**
     * Gestion centralisée des clics d'inventaire
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }
        
        Player player = (Player) event.getWhoClicked();
        String title = event.getView().getTitle();
        
        // Vérifier si c'est un GUI WarManager
        if (!isWarManagerGUI(title)) {
            return;
        }
        
        event.setCancelled(true);
        
        // Vérification anti-spam
        if (!canClick(player)) {
            return;
        }
        
        // Marquer comme en traitement
        setProcessing(player.getUniqueId(), true);
        
        try {
            // Déléguer selon le type de GUI
            if (title.contains("Gestion des Guerres") || title.contains("Sélection de guerre")) {
                handleWarSelectionClick(event, player);
            } else if (title.contains("Sélection des nations")) {
                handleNationSelectionClick(event, player);
            } else if (title.contains("Camp pour")) {
                handleSideSelectionClick(event, player);
            } else if (title.contains("Gestion des camps")) {
                handleSideManagementClick(event, player);
            } else if (title.contains("Statistiques")) {
                handleWarStatsClick(event, player);
            }
        } finally {
            // Libérer le verrou après un délai
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                setProcessing(player.getUniqueId(), false);
            }, 10L);
        }
    }
    
    private boolean isWarManagerGUI(String title) {
        return title.contains("Guerre") || 
               title.contains("Camp") || 
               title.contains("Nation") || 
               title.contains("Statistiques") ||
               title.contains("Gestion");
    }
    
    private void handleWarSelectionClick(InventoryClickEvent event, Player player) {
        // Déléguer à WarSelectionGUI mais de manière optimisée
        WarSelectionGUI gui = new WarSelectionGUI(plugin, player.hasPermission("warmanager.admin"));
        gui.handleClick(event, player);
    }
    
    private void handleNationSelectionClick(InventoryClickEvent event, Player player) {
        // Logique pour NationSelectionGUI
        NationSelectionGUI.handleClickStatic(event, player, plugin);
    }
    
    private void handleSideSelectionClick(InventoryClickEvent event, Player player) {
        // Logique pour SideSelectionGUI
        SideSelectionGUI.handleClickStatic(event, player, plugin);
    }
    
    private void handleSideManagementClick(InventoryClickEvent event, Player player) {
        // Logique pour SideManagementGUI
        SideManagementGUI.handleClickStatic(event, player, plugin);
    }
    
    private void handleWarStatsClick(InventoryClickEvent event, Player player) {
        // Logique pour WarStatsGUI
        WarStatsGUI.handleClickStatic(event, player, plugin);
    }
    
    /**
     * Gestion du chat pour les inputs
     */
    @EventHandler(priority = EventPriority.HIGHEST)
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
            player.sendMessage("§cAction annulée");
            return;
        }
        
        // Traitement selon le type d'input
        if (awaitingInput.containsKey(playerId)) {
            String inputType = awaitingInput.get(playerId);
            SideManagementGUI.handleChatInput(player, message, inputType, awaitingInput, awaitingColor, plugin);
        } else if (awaitingColor.containsKey(playerId)) {
            String sideName = awaitingColor.get(playerId);
            SideManagementGUI.handleColorInput(player, message, sideName, awaitingColor, plugin);
        }
    }
    
    /**
     * Nettoyage à la fermeture d'inventaire
     */
    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        
        // Nettoyer le traitement avec un délai
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            setProcessing(uuid, false);
        }, 5L);
    }
    
    /**
     * Nettoyage à la déconnexion
     */
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        lastClick.remove(uuid);
        processingPlayers.remove(uuid);
        awaitingInput.remove(uuid);
        awaitingColor.remove(uuid);
    }
    
    /**
     * Getters pour les maps d'attente
     */
    public Map<UUID, String> getAwaitingInput() {
        return awaitingInput;
    }
    
    public Map<UUID, String> getAwaitingColor() {
        return awaitingColor;
    }
    
    /**
     * Nettoyage complet
     */
    public void cleanup() {
        HandlerList.unregisterAll(this);
        lastClick.clear();
        processingPlayers.clear();
        awaitingInput.clear();
        awaitingColor.clear();
    }
}