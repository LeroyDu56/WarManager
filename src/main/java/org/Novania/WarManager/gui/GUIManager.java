package org.Novania.WarManager.gui;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.Novania.WarManager.WarManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class GUIManager implements Listener {
    
    private final WarManager plugin;
    private final Map<UUID, Long> lastClick = new ConcurrentHashMap<>();
    private final Map<UUID, String> awaitingInput = new ConcurrentHashMap<>();
    private final Map<UUID, String> awaitingColor = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> playerWarContext = new ConcurrentHashMap<>(); // NOUVEAU: Contexte de guerre par joueur
    
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
        
        lastClick.put(uuid, now);
        return true;
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
        
        // IMPORTANT: Annuler TOUS les clics dans nos GUIs
        event.setCancelled(true);
        
        // Vérification anti-spam
        if (!canClick(player)) {
            return;
        }
        
        try {
            // DEBUG
            plugin.getLogger().info("=== DEBUG GUI CLICK ===");
            plugin.getLogger().info("Titre: " + title);
            plugin.getLogger().info("Joueur: " + player.getName());
            plugin.getLogger().info("Permission admin: " + player.hasPermission("warmanager.admin"));
            
            // Déléguer selon le type de GUI
            if (title.contains("Gestion des Guerres")) {
                // Mode admin uniquement
                plugin.getLogger().info("Mode: Gestion admin");
                handleWarSelectionClick(event, player);
            } else if (title.contains("Sélection de guerre") || title.equals("§8Sélection de guerre")) {
                // Mode joueur uniquement
                plugin.getLogger().info("Mode: Sélection joueur");
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
        } catch (Exception e) {
            plugin.getLogger().warning("Erreur dans la gestion du clic GUI: " + e.getMessage());
        }
    }
    
    /**
     * Empêcher le glisser-déposer dans nos GUIs
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }
        
        String title = event.getView().getTitle();
        
        // Vérifier si c'est un GUI WarManager
        if (isWarManagerGUI(title)) {
            // Annuler TOUS les glisser-déposer dans nos GUIs
            event.setCancelled(true);
        }
    }
    
    private boolean isWarManagerGUI(String title) {
        return title.contains("Guerre") || 
               title.contains("Camp") || 
               title.contains("Nation") || 
               title.contains("Statistiques") ||
               title.contains("Gestion") ||
               title.contains("Sélection") ||
               // Messages français
               title.contains("guerre") ||
               title.contains("camp") ||
               title.contains("nation") ||
               title.contains("statistiques") ||
               title.contains("gestion") ||
               title.contains("sélection") ||
               // Titres spécifiques
               title.equals("§8Sélection de guerre") ||
               title.equals("§cGestion des Guerres");
    }
    
    private void handleWarSelectionClick(InventoryClickEvent event, Player player) {
        String title = event.getView().getTitle();
        
        // Déterminer le mode selon le titre exact
        boolean isAdminMode = title.equals("§cGestion des Guerres");
        boolean isPlayerMode = title.equals("§8Sélection de guerre");
        
        plugin.getLogger().info("handleWarSelectionClick - Admin mode: " + isAdminMode + ", Player mode: " + isPlayerMode);
        
        // Déléguer à WarSelectionGUI avec le bon mode
        WarSelectionGUI gui = new WarSelectionGUI(plugin, isAdminMode);
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
        // Nettoyage automatique après fermeture
        UUID uuid = event.getPlayer().getUniqueId();
        
        // Nettoyer le cache de clics après un délai
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            lastClick.remove(uuid);
        }, 100L); // 5 secondes
    }
    
    /**
     * Nettoyage à la déconnexion
     */
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        lastClick.remove(uuid);
        awaitingInput.remove(uuid);
        awaitingColor.remove(uuid);
        playerWarContext.remove(uuid);
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
    
    public Map<UUID, Integer> getPlayerWarContext() {
        return playerWarContext;
    }
    
    /**
     * Nettoyage complet
     */
    public void cleanup() {
        HandlerList.unregisterAll(this);
        lastClick.clear();
        awaitingInput.clear();
        awaitingColor.clear();
        playerWarContext.clear();
    }
}