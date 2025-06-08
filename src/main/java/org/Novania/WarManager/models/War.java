// War.java
package org.Novania.WarManager.models;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class War {
    private int id;
    private String name;
    private String casusBeliType;
    private int requiredPoints;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private boolean isActive;
    private List<WarSide> sides;
    private UUID createdBy;
    
    public War(int id, String name, String casusBeliType, int requiredPoints, UUID createdBy) {
        this.id = id;
        this.name = name;
        this.casusBeliType = casusBeliType;
        this.requiredPoints = requiredPoints;
        this.createdBy = createdBy;
        this.startDate = LocalDateTime.now();
        this.isActive = true;
        this.sides = new ArrayList<>();
    }
    
    // Getters et Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public String getCasusBeliType() { return casusBeliType; }
    public void setCasusBeliType(String casusBeliType) { this.casusBeliType = casusBeliType; }
    
    public int getRequiredPoints() { return requiredPoints; }
    public void setRequiredPoints(int requiredPoints) { this.requiredPoints = requiredPoints; }
    
    public LocalDateTime getStartDate() { return startDate; }
    public void setStartDate(LocalDateTime startDate) { this.startDate = startDate; }
    
    public LocalDateTime getEndDate() { return endDate; }
    public void setEndDate(LocalDateTime endDate) { this.endDate = endDate; }
    
    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }
    
    public List<WarSide> getSides() { return sides; }
    public void setSides(List<WarSide> sides) { this.sides = sides; }
    
    public UUID getCreatedBy() { return createdBy; }
    public void setCreatedBy(UUID createdBy) { this.createdBy = createdBy; }
    
    // Méthodes utilitaires
    public void addSide(WarSide side) {
        this.sides.add(side);
    }
    
    public WarSide getSideByName(String sideName) {
        return sides.stream()
                .filter(side -> side.getName().equals(sideName))
                .findFirst()
                .orElse(null);
    }
    
    public WarSide getWinner() {
        return sides.stream()
                .filter(side -> side.getPoints() >= requiredPoints)
                .findFirst()
                .orElse(null);
    }
    
    public boolean hasWinner() {
        return getWinner() != null;
    }
}