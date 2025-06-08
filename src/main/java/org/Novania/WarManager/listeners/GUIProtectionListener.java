package org.Novania.WarManager.listeners;

import org.Novania.WarManager.WarManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.inventory.InventoryType;

/**
 * Listener spécialisé pour protéger les GUIs WarManager
 * Empêche toute manipulation d'items dans nos interfaces
 */
public class GUIProtectionListener implements Listener {
    
    private final WarManager plugin;
    
    public GUIProtectionListener(WarManager plugin) {
        this.plugin = plugin;
    }
    
    /**
     * Protection principale contre tous les clics
     */
    @EventHandler(priority = EventPriority.LOWEST) // LOWEST pour être sûr d'être appelé en premier
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }
        
        String title = event.getView().getTitle();
        
        // Vérifier si c'est un de nos GUIs
        if (isWarManagerGUI(title)) {
            // Annuler TOUS les types de clics possibles
            event.setCancelled(true);
            
            // Protection supplémentaire pour certaines actions spéciales
            InventoryAction action = event.getAction();
            switch (action) {
                case PICKUP_ALL:
                case PICKUP_HALF:
                case PICKUP_ONE:
                case PICKUP_SOME:
                case PLACE_ALL:
                case PLACE_ONE:
                case PLACE_SOME:
                case SWAP_WITH_CURSOR:
                case DROP_ALL_CURSOR:
                case DROP_ONE_CURSOR:
                case DROP_ALL_SLOT:
                case DROP_ONE_SLOT:
                case MOVE_TO_OTHER_INVENTORY:
                case HOTBAR_MOVE_AND_READD:
                case HOTBAR_SWAP:
                case CLONE_STACK:
                case COLLECT_TO_CURSOR:
                    event.setCancelled(true);
                    break;
                default:
                    event.setCancelled(true);
                    break;
            }
        }
    }
    
    /**
     * Protection contre le glisser-déposer
     */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }
        
        String title = event.getView().getTitle();
        
        if (isWarManagerGUI(title)) {
            event.setCancelled(true);
        }
    }
    
    /**
     * Protection contre le déplacement automatique d'items (hoppers, etc.)
     */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onInventoryMoveItem(InventoryMoveItemEvent event) {
        // Empêcher les hoppers ou autres systèmes de prendre des items de nos GUIs
        if (event.getSource().getType() == InventoryType.CHEST) {
            String sourceTitle = event.getSource().getViewers().isEmpty() ? "" : 
                event.getSource().getViewers().get(0).getOpenInventory().getTitle();
            
            if (isWarManagerGUI(sourceTitle)) {
                event.setCancelled(true);
            }
        }
        
        if (event.getDestination().getType() == InventoryType.CHEST) {
            String destTitle = event.getDestination().getViewers().isEmpty() ? "" : 
                event.getDestination().getViewers().get(0).getOpenInventory().getTitle();
                
            if (isWarManagerGUI(destTitle)) {
                event.setCancelled(true);
            }
        }
    }
    
    /**
     * Vérifie si un titre correspond à un de nos GUIs
     */
    private boolean isWarManagerGUI(String title) {
        if (title == null || title.isEmpty()) {
            return false;
        }
        
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
               title.contains("sélection");
    }
}