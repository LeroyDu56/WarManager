package org.Novania.WarManager.listeners;

import org.Novania.WarManager.WarManager;
import org.Novania.WarManager.models.CaptureZone;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityExplodeEvent;

public class CaptureZoneProtectionListener implements Listener {
    
    private final WarManager plugin;
    
    public CaptureZoneProtectionListener(WarManager plugin) {
        this.plugin = plugin;
    }
    
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();
        
        // Vérifier si le joueur a la permission admin
        if (player.hasPermission("warmanager.admin")) {
            return; // Les admins peuvent tout casser
        }
        
        // Vérifier si c'est un bloc protégé d'une zone de capture
        if (isProtectedBlock(block)) {
            event.setCancelled(true);
            player.sendMessage("§c⚠ Ce bloc fait partie d'une zone de capture et ne peut pas être cassé !");
            player.sendMessage("§7Seuls les administrateurs peuvent modifier les zones de capture.");
            
            plugin.getLogger().info("Tentative de casse de bloc protégé par " + player.getName() + 
                                  " à " + block.getX() + ", " + block.getY() + ", " + block.getZ());
        }
    }
    
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();
        
        // Vérifier si le joueur a la permission admin
        if (player.hasPermission("warmanager.admin")) {
            return; // Les admins peuvent tout placer
        }
        
        // Vérifier si on essaie de placer sur une zone protégée
        if (isInProtectedZone(block.getLocation())) {
            event.setCancelled(true);
            player.sendMessage("§c⚠ Vous ne pouvez pas placer de blocs dans une zone de capture !");
            player.sendMessage("§7Cette zone est protégée pour maintenir l'intégrité du gameplay.");
            
            plugin.getLogger().info("Tentative de placement de bloc dans zone protégée par " + player.getName() + 
                                  " à " + block.getX() + ", " + block.getY() + ", " + block.getZ());
        }
    }
    
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityExplode(EntityExplodeEvent event) {
        // Protéger contre les explosions (TNT, Creepers, etc.)
        event.blockList().removeIf(block -> {
            if (isProtectedBlock(block)) {
                plugin.getLogger().info("Bloc protégé sauvé d'une explosion à " + 
                                      block.getX() + ", " + block.getY() + ", " + block.getZ());
                return true; // Retirer le bloc de la liste d'explosion
            }
            return false;
        });
    }
    
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockExplode(BlockExplodeEvent event) {
        // Protéger contre les explosions de blocs (TNT, etc.)
        event.blockList().removeIf(block -> {
            if (isProtectedBlock(block)) {
                plugin.getLogger().info("Bloc protégé sauvé d'une explosion de bloc à " + 
                                      block.getX() + ", " + block.getY() + ", " + block.getZ());
                return true; // Retirer le bloc de la liste d'explosion
            }
            return false;
        });
    }
    
    /**
     * Vérifie si un bloc est protégé (drapeau ou bloc de support d'une zone de capture)
     */
    private boolean isProtectedBlock(Block block) {
        Location blockLoc = block.getLocation();
        
        for (CaptureZone zone : plugin.getCaptureZoneManager().getActiveZones().values()) {
            if (!zone.isActive()) {
                continue;
            }
            
            Location flagLoc = zone.getFlagLocation();
            Location supportLoc = flagLoc.clone().add(0, -1, 0);
            
            // Vérifier si c'est le drapeau (avec tolérance)
            if (isSameBlock(blockLoc, flagLoc)) {
                plugin.getLogger().info("Bloc protégé détecté: Drapeau de zone #" + zone.getId());
                return true;
            }
            
            // Vérifier si c'est le bloc de support
            if (isSameBlock(blockLoc, supportLoc)) {
                plugin.getLogger().info("Bloc protégé détecté: Support de zone #" + zone.getId());
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Vérifie si une location est dans une zone de capture protégée
     */
    private boolean isInProtectedZone(Location location) {
        for (CaptureZone zone : plugin.getCaptureZoneManager().getActiveZones().values()) {
            if (!zone.isActive()) {
                continue;
            }
            
            Location flagLoc = zone.getFlagLocation();
            
            // Zone de protection de 3x3 blocs autour du drapeau
            if (location.getWorld().equals(flagLoc.getWorld()) &&
                Math.abs(location.getBlockX() - flagLoc.getBlockX()) <= 1 &&
                Math.abs(location.getBlockY() - flagLoc.getBlockY()) <= 2 && // 2 blocs en hauteur
                Math.abs(location.getBlockZ() - flagLoc.getBlockZ()) <= 1) {
                
                plugin.getLogger().info("Emplacement dans zone protégée #" + zone.getId());
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Compare deux locations de blocs
     */
    private boolean isSameBlock(Location loc1, Location loc2) {
        return loc1.getWorld().equals(loc2.getWorld()) &&
               loc1.getBlockX() == loc2.getBlockX() &&
               loc1.getBlockY() == loc2.getBlockY() &&
               loc1.getBlockZ() == loc2.getBlockZ();
    }
}