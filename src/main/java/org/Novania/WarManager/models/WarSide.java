package org.Novania.WarManager.models;

import java.util.ArrayList;
import java.util.List;

public class WarSide {
    private String name;
    private List<String> nations;
    private int points;
    private int kills;
    private String color; // Code couleur pour l'affichage
    
    public WarSide(String name, String color) {
        this.name = name;
        this.color = color;
        this.nations = new ArrayList<>();
        this.points = 0;
        this.kills = 0;
    }
    
    // Getters et Setters
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public List<String> getNations() { return nations; }
    public void setNations(List<String> nations) { this.nations = nations; }
    
    public int getPoints() { return points; }
    public void setPoints(int points) { this.points = points; }
    
    public int getKills() { return kills; }
    public void setKills(int kills) { this.kills = kills; }
    
    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }
    
    // Méthodes utilitaires
    public void addNation(String nationName) {
        if (!nations.contains(nationName)) {
            nations.add(nationName);
        }
    }
    
    public boolean removeNation(String nationName) {
        return nations.remove(nationName);
    }
    
    public boolean hasNation(String nationName) {
        return nations.contains(nationName);
    }
    
    public void addPoints(int points) {
        this.points += points;
    }
    
    // FIX: Ne plus ajouter automatiquement de points dans addKill()
    public void addKill() {
        this.kills++;
        // SUPPRIMÉ: this.points++; pour éviter le double comptage
    }
    
    public String getDisplayName() {
        return color + name;
    }
}