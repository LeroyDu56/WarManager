package org.Novania.WarManager;

import org.Novania.WarManager.commands.WarAdminCommand;
import org.Novania.WarManager.commands.WarCommand;
import org.Novania.WarManager.gui.GUIManager;
import org.Novania.WarManager.listeners.CaptureZoneListener;
import org.Novania.WarManager.listeners.CaptureZoneProtectionListener;
import org.Novania.WarManager.listeners.PlayerDeathListener;
import org.Novania.WarManager.managers.CaptureZoneManager;
import org.Novania.WarManager.managers.ConfigManager;
import org.Novania.WarManager.managers.WarDataManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

public class WarManager extends JavaPlugin {
    
    private static WarManager instance;
    private WarDataManager warDataManager;
    private ConfigManager configManager;
    private CaptureZoneManager captureZoneManager;
    private GUIManager guiManager;
    
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
        
        // Initialisation de la base de données de manière asynchrone
        new BukkitRunnable() {
            @Override
            public void run() {
                warDataManager.initDatabase();
                
                // Initialiser le système de capture de zone après la DB
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        captureZoneManager = new CaptureZoneManager(WarManager.this);
                        captureZoneManager.initializeCaptureZones();
                        
                        // Initialiser le gestionnaire de GUI
                        guiManager = new GUIManager(WarManager.this);
                        
                        getLogger().info("WarManager initialisé avec succès !");
                    }
                }.runTask(WarManager.this);
            }
        }.runTaskAsynchronously(this);
        
        // Enregistrement des commandes
        getCommand("war").setExecutor(new WarCommand(this));
        getCommand("waradmin").setExecutor(new WarAdminCommand(this));
        
        // Enregistrement des listeners (UN SEUL DE CHAQUE)
        getServer().getPluginManager().registerEvents(new PlayerDeathListener(this), this);
        getServer().getPluginManager().registerEvents(new CaptureZoneListener(this), this);
        getServer().getPluginManager().registerEvents(new CaptureZoneProtectionListener(this), this);
        
        getLogger().info("WarManager activé avec succès !");
    }
    
    @Override
    public void onDisable() {
        if (captureZoneManager != null) {
            captureZoneManager.stopAllZoneTasks();
        }
        
        if (warDataManager != null) {
            warDataManager.closeDatabase();
        }
        
        // Nettoyer le gestionnaire de GUI
        if (guiManager != null) {
            guiManager.cleanup();
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
    
    public GUIManager getGuiManager() {
        return guiManager;
    }
}