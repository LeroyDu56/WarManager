// MessageUtils.java
package org.Novania.WarManager.utils;

import org.Novania.WarManager.WarManager;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class MessageUtils {
    
    private static FileConfiguration messagesConfig;
    private static File messagesFile;
    
    public static void loadMessages(WarManager plugin) {
        messagesFile = new File(plugin.getDataFolder(), "messages.yml");
        
        if (!messagesFile.exists()) {
            plugin.saveResource("messages.yml", false);
        }
        
        messagesConfig = YamlConfiguration.loadConfiguration(messagesFile);
        
        // Charger les valeurs par défaut
        InputStream defConfigStream = plugin.getResource("messages.yml");
        if (defConfigStream != null) {
            YamlConfiguration defConfig = YamlConfiguration.loadConfiguration(new InputStreamReader(defConfigStream));
            messagesConfig.setDefaults(defConfig);
        }
    }
    
    public static String getMessage(String path) {
        if (messagesConfig == null) {
            return "§cErreur: Messages non chargés";
        }
        
        String message = messagesConfig.getString(path);
        if (message == null) {
            return "§cMessage manquant: " + path;
        }
        
        String prefix = messagesConfig.getString("prefix", "");
        return ChatColor.translateAlternateColorCodes('&', prefix + message);
    }
    
    public static String getMessageRaw(String path) {
        if (messagesConfig == null) {
            return "Erreur: Messages non chargés";
        }
        
        String message = messagesConfig.getString(path);
        if (message == null) {
            return "Message manquant: " + path;
        }
        
        return ChatColor.translateAlternateColorCodes('&', message);
    }
    
    public static void reloadMessages() {
        if (messagesFile != null) {
            messagesConfig = YamlConfiguration.loadConfiguration(messagesFile);
        }
    }
    
    public static void saveMessages() {
        if (messagesConfig != null && messagesFile != null) {
            try {
                messagesConfig.save(messagesFile);
            } catch (IOException e) {
                WarManager.getInstance().getLogger().severe("Impossible de sauvegarder messages.yml");
            }
        }
    }
}