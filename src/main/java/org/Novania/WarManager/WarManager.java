package org.Novania.WarManager;

import org.Novania.WarManager.commands.WarAdminCommand;
import org.Novania.WarManager.commands.WarCommand;
import org.Novania.WarManager.listeners.CaptureZoneListener;
import org.Novania.WarManager.listeners.CaptureZoneProtectionListener;
import org.Novania.WarManager.listeners.PlayerDeathListener;
import org.Novania.WarManager.managers.CaptureZoneManager;
import org.Novania.WarManager.managers.ConfigManager;
import org.Novania.WarManager.managers.WarDataManager;
import org.bukkit.plugin.java.JavaPlugin;

public class WarManager extends JavaPlugin {
    
    private static WarManager instance;
    private WarDataManager warDataManager;
    private ConfigManager configManager;
    private CaptureZoneManager captureZoneManager;
    
    @Override
    public void onEnable() {
        instance = this;
        
        // Vérifier que Towny est présent
        if (getServer().getPluginManager().getPlugin("Towny") == null) {
            getLogger().severe("Towny n'est pas installé ! Désactivation du plugin.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        
        // Initialisation des managers
        this.configManager = new ConfigManager(this);
        this.warDataManager = new WarDataManager(this);
        
        // Chargement de la configuration
        configManager.loadConfig();
        
        // Initialisation de la base de données
        warDataManager.initDatabase();
        
        // Initialisation du système de capture de zone
        this.captureZoneManager = new CaptureZoneManager(this);
        captureZoneManager.initializeCaptureZones();
        
        // Enregistrement des commandes
        getCommand("war").setExecutor(new WarCommand(this));
        getCommand("waradmin").setExecutor(new WarAdminCommand(this));
        
        // Enregistrement des listeners
        getServer().getPluginManager().registerEvents(new PlayerDeathListener(this), this);
        getServer().getPluginManager().registerEvents(new CaptureZoneListener(this), this);
        getServer().getPluginManager().registerEvents(new CaptureZoneProtectionListener(this), this);
        
        getLogger().info("WarManager activé avec succès !");
        getLogger().info("Système de capture de zone initialisé !");
        getLogger().info("Protection des zones de capture activée !");
    }
    
    @Override
    public void onDisable() {
        if (captureZoneManager != null) {
            captureZoneManager.stopAllZoneTasks();
        }
        
        if (warDataManager != null) {
            warDataManager.closeDatabase();
        }
        
        getLogger().info("WarManager désactivé !");
    }
    
    public static WarManager getInstance() {
        return instance;
    }
    
    public WarDataManager getWarDataManager() {
        return warDataManager;
    }
    
    public ConfigManager getConfigManager() {
        return configManager;
    }
    
    public CaptureZoneManager getCaptureZoneManager() {
        return captureZoneManager;
    }
}