package org.Novania.WarManager.models;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;

public class CaptureZone {
    private int id;
    private int warId;
    private String name;
    private Location centerLocation;
    private Location flagLocation;
    private int radius; // En chunks (3 = 3x3 chunks)
    private String currentController; // Nom du camp qui contrôle
    private LocalDateTime captureStartTime;
    private LocalDateTime zoneEndTime; // Fin de la zone (3h après création)
    private Map<String, Long> controlTimeMap; // Temps de contrôle par camp (en millisecondes)
    private boolean isActive;
    private UUID createdBy;
    private Material flagMaterial;
    
    public CaptureZone(int id, int warId, String name, Location centerLocation, UUID createdBy) {
        this.id = id;
        this.warId = warId;
        this.name = name;
        this.centerLocation = centerLocation;
        this.flagLocation = centerLocation.clone().add(0, 1, 0); // Drapeau 1 bloc au-dessus du centre
        this.radius = 1; // 3x3 chunks (rayon de 1 chunk autour du centre)
        this.currentController = null;
        this.captureStartTime = null;
        this.zoneEndTime = LocalDateTime.now().plusHours(3); // 3h de durée
        this.controlTimeMap = new HashMap<>();
        this.isActive = true;
        this.createdBy = createdBy;
        this.flagMaterial = Material.WHITE_BANNER;
    }
    
    // Getters et Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    
    public int getWarId() { return warId; }
    public void setWarId(int warId) { this.warId = warId; }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public Location getCenterLocation() { return centerLocation; }
    public void setCenterLocation(Location centerLocation) { 
        this.centerLocation = centerLocation;
        this.flagLocation = centerLocation.clone().add(0, 1, 0);
    }
    
    public Location getFlagLocation() { return flagLocation; }
    public void setFlagLocation(Location flagLocation) { this.flagLocation = flagLocation; }
    
    public int getRadius() { return radius; }
    public void setRadius(int radius) { this.radius = radius; }
    
    public String getCurrentController() { return currentController; }
    public void setCurrentController(String currentController) { this.currentController = currentController; }
    
    public LocalDateTime getCaptureStartTime() { return captureStartTime; }
    public void setCaptureStartTime(LocalDateTime captureStartTime) { this.captureStartTime = captureStartTime; }
    
    public LocalDateTime getZoneEndTime() { return zoneEndTime; }
    public void setZoneEndTime(LocalDateTime zoneEndTime) { this.zoneEndTime = zoneEndTime; }
    
    public Map<String, Long> getControlTimeMap() { return controlTimeMap; }
    public void setControlTimeMap(Map<String, Long> controlTimeMap) { this.controlTimeMap = controlTimeMap; }
    
    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }
    
    public UUID getCreatedBy() { return createdBy; }
    public void setCreatedBy(UUID createdBy) { this.createdBy = createdBy; }
    
    public Material getFlagMaterial() { return flagMaterial; }
    public void setFlagMaterial(Material flagMaterial) { this.flagMaterial = flagMaterial; }
    
    // Méthodes utilitaires
    
    /**
     * Capture la zone pour un camp
     */
    public void captureForSide(String sideName) {
        // Sauvegarder le temps de contrôle du camp précédent
        if (currentController != null && captureStartTime != null) {
            long controlTime = java.time.Duration.between(captureStartTime, LocalDateTime.now()).toMillis();
            controlTimeMap.put(currentController, controlTimeMap.getOrDefault(currentController, 0L) + controlTime);
        }
        
        this.currentController = sideName;
        this.captureStartTime = LocalDateTime.now();
    }
    
    /**
     * Vérifie si la zone est terminée (3h écoulées)
     */
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(zoneEndTime);
    }
    
    /**
     * Calcule le temps de contrôle total pour un camp
     */
    public long getTotalControlTime(String sideName) {
        long totalTime = controlTimeMap.getOrDefault(sideName, 0L);
        
        // Ajouter le temps actuel si c'est le contrôleur actuel
        if (sideName.equals(currentController) && captureStartTime != null) {
            totalTime += java.time.Duration.between(captureStartTime, LocalDateTime.now()).toMillis();
        }
        
        return totalTime;
    }
    
    /**
     * Détermine le camp vainqueur (celui qui a contrôlé le plus longtemps)
     */
    public String getWinningSide() {
        // Finaliser le temps du contrôleur actuel
        if (currentController != null && captureStartTime != null) {
            long controlTime = java.time.Duration.between(captureStartTime, LocalDateTime.now()).toMillis();
            controlTimeMap.put(currentController, controlTimeMap.getOrDefault(currentController, 0L) + controlTime);
        }
        
        String winner = null;
        long maxTime = 0;
        
        for (Map.Entry<String, Long> entry : controlTimeMap.entrySet()) {
            if (entry.getValue() > maxTime) {
                maxTime = entry.getValue();
                winner = entry.getKey();
            }
        }
        
        return winner;
    }
    
    /**
     * Vérifie si une location est dans la zone de capture
     */
    public boolean isInZone(Location location) {
        if (!location.getWorld().equals(centerLocation.getWorld())) {
            return false;
        }
        
        int centerChunkX = centerLocation.getChunk().getX();
        int centerChunkZ = centerLocation.getChunk().getZ();
        int locationChunkX = location.getChunk().getX();
        int locationChunkZ = location.getChunk().getZ();
        
        return Math.abs(locationChunkX - centerChunkX) <= radius && 
               Math.abs(locationChunkZ - centerChunkZ) <= radius;
    }
    
    /**
     * Place le drapeau dans le monde
     */
    public void placeFlagInWorld() {
        Block flagBlock = flagLocation.getBlock();
        flagBlock.setType(flagMaterial);
        
        // Ajouter un bloc de support si nécessaire
        Block supportBlock = flagLocation.clone().add(0, -1, 0).getBlock();
        if (supportBlock.getType() == Material.AIR) {
            supportBlock.setType(Material.COBBLESTONE);
        }
    }
    
    /**
     * Retire le drapeau du monde
     */
    public void removeFlagFromWorld() {
        Block flagBlock = flagLocation.getBlock();
        if (flagBlock.getType().name().contains("BANNER")) {
            flagBlock.setType(Material.AIR);
        }
    }
    
    /**
     * Met à jour la couleur du drapeau selon le contrôleur
     */
    public void updateFlagColor(String sideColor) {
        if (sideColor == null) {
            flagMaterial = Material.WHITE_BANNER;
        } else {
            // Changer la couleur selon le camp
            switch (sideColor) {
                case "§c": flagMaterial = Material.RED_BANNER; break;
                case "§9": flagMaterial = Material.BLUE_BANNER; break;
                case "§a": flagMaterial = Material.GREEN_BANNER; break;
                case "§e": flagMaterial = Material.YELLOW_BANNER; break;
                case "§6": flagMaterial = Material.ORANGE_BANNER; break;
                case "§5": flagMaterial = Material.PURPLE_BANNER; break;
                case "§b": flagMaterial = Material.CYAN_BANNER; break;
                case "§0": flagMaterial = Material.BLACK_BANNER; break;
                case "§7": flagMaterial = Material.GRAY_BANNER; break;
                default: flagMaterial = Material.WHITE_BANNER; break;
            }
        }
        
        // Mettre à jour le bloc dans le monde
        placeFlagInWorld();
    }
    
    /**
     * Retourne le temps restant en minutes
     */
    public long getRemainingMinutes() {
        if (isExpired()) return 0;
        return java.time.Duration.between(LocalDateTime.now(), zoneEndTime).toMinutes();
    }
    
    /**
     * Format du temps de contrôle en minutes:secondes
     */
    public String formatControlTime(long milliseconds) {
        long minutes = milliseconds / (1000 * 60);
        long seconds = (milliseconds / 1000) % 60;
        return String.format("%d:%02d", minutes, seconds);
    }
}