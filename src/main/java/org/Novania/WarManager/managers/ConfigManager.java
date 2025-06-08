// ConfigManager.java
package org.Novania.WarManager.managers;

import org.Novania.WarManager.WarManager;
import org.Novania.WarManager.utils.MessageUtils;

public class ConfigManager {
    
    private final WarManager plugin;
    
    public ConfigManager(WarManager plugin) {
        this.plugin = plugin;
    }
    
    public void loadConfig() {
        // Sauvegarder la config par défaut si elle n'existe pas
        plugin.saveDefaultConfig();
        
        // Créer les fichiers de ressources par défaut
        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdirs();
        }
        
        // Sauvegarder messages.yml s'il n'existe pas
        plugin.saveResource("messages.yml", false);
        
        // Charger les messages
        MessageUtils.loadMessages(plugin);
        
        plugin.getLogger().info("Configuration chargée avec succès !");
        
        // Log de la configuration de la base de données
        String dbType = plugin.getConfig().getString("database.type", "SQLITE");
        plugin.getLogger().info("Type de base de données configuré: " + dbType);
    }
    
    public void reloadConfig() {
        try {
            // Recharger config.yml
            plugin.reloadConfig();
            
            // Recharger messages.yml
            MessageUtils.reloadMessages();
            
            plugin.getLogger().info("Configuration rechargée avec succès !");
            
        } catch (Exception e) {
            plugin.getLogger().severe("Erreur lors du rechargement de la configuration: " + e.getMessage());
            throw e;
        }
    }
    
    public int getVictoryPoints(String casusBeliType) {
        return plugin.getConfig().getInt("settings.victory_points." + casusBeliType, 50);
    }
    
    public int getKillPoints() {
        return plugin.getConfig().getInt("settings.points.kill", 1);
    }
    
    public int getCaptureLeaderPoints() {
        return plugin.getConfig().getInt("settings.points.capture_leader", 10);
    }
    
    public int getGroupBattlePoints() {
        return plugin.getConfig().getInt("settings.points.group_battle", 15);
    }
    
    public int getSiegeSuccessPoints() {
        return plugin.getConfig().getInt("settings.points.siege_success", 20);
    }
    
    public int getMaxWarDuration() {
        return plugin.getConfig().getInt("settings.max_war_duration", 21);
    }
    
    public int getMinNationsPerSide() {
        return plugin.getConfig().getInt("settings.restrictions.min_nations_per_side", 1);
    }
    
    public int getMaxNationsPerSide() {
        return plugin.getConfig().getInt("settings.restrictions.max_nations_per_side", 10);
    }
    
    public int getCooldownBetweenWars() {
        return plugin.getConfig().getInt("settings.restrictions.cooldown_between_wars", 7);
    }
    
    public boolean isKillBroadcastEnabled() {
        return plugin.getConfig().getBoolean("settings.notifications.kill_broadcast", true);
    }
    
    public boolean isWarUpdatesEnabled() {
        return plugin.getConfig().getBoolean("settings.notifications.war_updates", true);
    }
    
    public boolean isDailySummaryEnabled() {
        return plugin.getConfig().getBoolean("settings.notifications.daily_summary", true);
    }
    
    // Méthodes pour accéder aux configurations de base de données
    public String getDatabaseType() {
        return plugin.getConfig().getString("database.type", "SQLITE");
    }
    
    public String getMysqlHost() {
        return plugin.getConfig().getString("database.mysql.host", "localhost");
    }
    
    public int getMysqlPort() {
        return plugin.getConfig().getInt("database.mysql.port", 3306);
    }
    
    public String getMysqlDatabase() {
        return plugin.getConfig().getString("database.mysql.database", "warmanager");
    }
    
    public String getMysqlUsername() {
        return plugin.getConfig().getString("database.mysql.username", "root");
    }
    
    public String getMysqlPassword() {
        return plugin.getConfig().getString("database.mysql.password", "password");
    }
    
    /**
     * Valide la configuration actuelle
     * @return true si la configuration est valide
     */
    public boolean validateConfig() {
        boolean valid = true;
        
        // Vérifier les points de victoire
        if (!plugin.getConfig().isConfigurationSection("settings.victory_points")) {
            plugin.getLogger().warning("Section 'settings.victory_points' manquante dans config.yml");
            valid = false;
        }
        
        // Vérifier les points
        if (!plugin.getConfig().isConfigurationSection("settings.points")) {
            plugin.getLogger().warning("Section 'settings.points' manquante dans config.yml");
            valid = false;
        }
        
        // Vérifier la durée maximale
        int maxDuration = getMaxWarDuration();
        if (maxDuration <= 0) {
            plugin.getLogger().warning("Durée maximale de guerre invalide: " + maxDuration);
            valid = false;
        }
        
        // Vérifier les restrictions
        int minNations = getMinNationsPerSide();
        int maxNations = getMaxNationsPerSide();
        if (minNations > maxNations || minNations < 1) {
            plugin.getLogger().warning("Configuration des nations par camp invalide: min=" + minNations + ", max=" + maxNations);
            valid = false;
        }
        
        // Vérifier la configuration de base de données
        String dbType = getDatabaseType();
        if (!dbType.equalsIgnoreCase("SQLITE") && !dbType.equalsIgnoreCase("MYSQL")) {
            plugin.getLogger().warning("Type de base de données invalide: " + dbType + " (doit être SQLITE ou MYSQL)");
            valid = false;
        }
        
        if (dbType.equalsIgnoreCase("MYSQL")) {
            if (getMysqlHost().isEmpty()) {
                plugin.getLogger().warning("Host MySQL non configuré");
                valid = false;
            }
            if (getMysqlDatabase().isEmpty()) {
                plugin.getLogger().warning("Nom de base de données MySQL non configuré");
                valid = false;
            }
        }
        
        return valid;
    }
    
    /**
     * Affiche un résumé de la configuration actuelle
     */
    public void logConfigSummary() {
        plugin.getLogger().info("=== Configuration WarManager ===");
        plugin.getLogger().info("Base de données: " + getDatabaseType());
        plugin.getLogger().info("Durée max guerre: " + getMaxWarDuration() + " jours");
        plugin.getLogger().info("Nations par camp: " + getMinNationsPerSide() + "-" + getMaxNationsPerSide());
        plugin.getLogger().info("Cooldown entre guerres: " + getCooldownBetweenWars() + " jours");
        plugin.getLogger().info("Points par kill: " + getKillPoints());
        plugin.getLogger().info("Notifications kills: " + (isKillBroadcastEnabled() ? "activées" : "désactivées"));
        
        // Afficher les types de casus belli disponibles
        if (plugin.getConfig().isConfigurationSection("settings.victory_points")) {
            plugin.getLogger().info("Types de guerre disponibles: " + 
                plugin.getConfig().getConfigurationSection("settings.victory_points").getKeys(false).size());
        }
        plugin.getLogger().info("================================");
    }
}