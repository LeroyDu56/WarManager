package org.Novania.WarManager.managers;

import org.Novania.WarManager.WarManager;
import org.Novania.WarManager.models.CaptureZone;
import org.Novania.WarManager.models.War;
import org.Novania.WarManager.models.WarSide;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class CaptureZoneManager {
    
    private final WarManager plugin;
    private final Map<Integer, CaptureZone> activeZones;
    private final Map<Integer, BukkitTask> zoneTasks; // Tâches pour les zones
    
    public CaptureZoneManager(WarManager plugin) {
        this.plugin = plugin;
        this.activeZones = new ConcurrentHashMap<>();
        this.zoneTasks = new HashMap<>();
    }
    
    public void initializeCaptureZones() {
        createCaptureZoneTables();
        loadActiveZones();
        plugin.getLogger().info("Système de capture de zone initialisé avec " + activeZones.size() + " zone(s) active(s)");
    }
    
    private void createCaptureZoneTables() {
        try {
            Connection connection = plugin.getWarDataManager().getConnection();
            
            String createZonesTable = """
                CREATE TABLE IF NOT EXISTS capture_zones (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    war_id INTEGER NOT NULL,
                    name VARCHAR(100) NOT NULL,
                    world_name VARCHAR(100) NOT NULL,
                    center_x DOUBLE NOT NULL,
                    center_y DOUBLE NOT NULL,
                    center_z DOUBLE NOT NULL,
                    flag_x DOUBLE NOT NULL,
                    flag_y DOUBLE NOT NULL,
                    flag_z DOUBLE NOT NULL,
                    radius INTEGER NOT NULL,
                    current_controller VARCHAR(50),
                    capture_start_time TEXT,
                    zone_end_time TEXT NOT NULL,
                    is_active BOOLEAN DEFAULT TRUE,
                    created_by VARCHAR(36) NOT NULL,
                    FOREIGN KEY (war_id) REFERENCES wars(id)
                )
            """;
            
            String createZoneControlTable = """
                CREATE TABLE IF NOT EXISTS zone_control_time (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    zone_id INTEGER NOT NULL,
                    side_name VARCHAR(50) NOT NULL,
                    control_time BIGINT NOT NULL,
                    FOREIGN KEY (zone_id) REFERENCES capture_zones(id)
                )
            """;
            
            connection.createStatement().execute(createZonesTable);
            connection.createStatement().execute(createZoneControlTable);
            
            plugin.getLogger().info("Tables de zones de capture créées");
            
        } catch (SQLException e) {
            plugin.getLogger().severe("Erreur lors de la création des tables de zones: " + e.getMessage());
        }
    }
    
    public CaptureZone createCaptureZone(int warId, String name, Location centerLocation, UUID createdBy) {
        try {
            Connection connection = plugin.getWarDataManager().getConnection();
            
            String sql = """
                INSERT INTO capture_zones (war_id, name, world_name, center_x, center_y, center_z, 
                                         flag_x, flag_y, flag_z, radius, zone_end_time, created_by) 
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;
            
            PreparedStatement stmt = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            stmt.setInt(1, warId);
            stmt.setString(2, name);
            stmt.setString(3, centerLocation.getWorld().getName());
            stmt.setDouble(4, centerLocation.getX());
            stmt.setDouble(5, centerLocation.getY());
            stmt.setDouble(6, centerLocation.getZ());
            stmt.setDouble(7, centerLocation.getX());
            stmt.setDouble(8, centerLocation.getY() + 1);
            stmt.setDouble(9, centerLocation.getZ());
            stmt.setInt(10, 1); // Rayon de 1 chunk (3x3)
            stmt.setString(11, LocalDateTime.now().plusHours(3).toString());
            stmt.setString(12, createdBy.toString());
            
            stmt.executeUpdate();
            ResultSet keys = stmt.getGeneratedKeys();
            
            if (keys.next()) {
                int zoneId = keys.getInt(1);
                CaptureZone zone = new CaptureZone(zoneId, warId, name, centerLocation, createdBy);
                activeZones.put(zoneId, zone);
                
                // Placer le drapeau dans le monde
                zone.placeFlagInWorld();
                plugin.getLogger().info("Drapeau placé à: " + zone.getFlagLocation().getBlockX() + ", " + 
                                      zone.getFlagLocation().getBlockY() + ", " + zone.getFlagLocation().getBlockZ());
                
                // Démarrer la tâche de surveillance
                startZoneTask(zone);
                
                plugin.getLogger().info("Zone de capture créée: " + name + " (ID: " + zoneId + ")");
                return zone;
            }
            
        } catch (SQLException e) {
            plugin.getLogger().severe("Erreur lors de la création de la zone de capture: " + e.getMessage());
            e.printStackTrace();
        }
        
        return null;
    }
    
    public void captureZone(int zoneId, String sideName, String playerName) {
        CaptureZone zone = activeZones.get(zoneId);
        if (zone == null || !zone.isActive() || zone.isExpired()) {
            plugin.getLogger().warning("Tentative de capture d'une zone invalide #" + zoneId);
            return;
        }
        
        plugin.getLogger().info("=== CAPTURE DE ZONE #" + zoneId + " ===");
        plugin.getLogger().info("Camp: " + sideName + " | Joueur: " + playerName);
        
        // Vérifier que le camp existe dans la guerre
        War war = plugin.getWarDataManager().getWar(zone.getWarId());
        if (war == null) {
            plugin.getLogger().warning("Guerre introuvable pour zone #" + zoneId);
            return;
        }
        
        WarSide side = war.getSideByName(sideName);
        if (side == null) {
            plugin.getLogger().warning("Camp " + sideName + " introuvable dans guerre #" + war.getId());
            return;
        }
        
        // Capturer la zone
        String previousController = zone.getCurrentController();
        plugin.getLogger().info("Contrôleur précédent: " + (previousController != null ? previousController : "Aucun"));
        
        zone.captureForSide(sideName);
        plugin.getLogger().info("Zone capturée par: " + sideName);
        
        // Mettre à jour en base de données
        updateZoneInDatabase(zone);
        
        // IMPORTANT: Changer la couleur du drapeau
        zone.updateFlagColor(side.getColor());
        plugin.getLogger().info("Couleur du drapeau mise à jour: " + side.getColor() + " -> " + zone.getFlagMaterial().name());
        
        // Messages
        String captureMessage = "§6[Zone] " + side.getDisplayName() + " §6a capturé la zone §e" + zone.getName() + " §6!";
        if (previousController != null) {
            captureMessage += " §7(Précédemment contrôlée par " + previousController + ")";
        }
        
        Bukkit.broadcastMessage(captureMessage);
        plugin.getLogger().info("✅ Zone #" + zoneId + " capturée avec succès par " + sideName);
    }
    
    private void startZoneTask(CaptureZone zone) {
        BukkitTask task = new BukkitRunnable() {
            @Override
            public void run() {
                if (!zone.isActive()) {
                    plugin.getLogger().info("Zone #" + zone.getId() + " devenue inactive - arrêt de la tâche");
                    this.cancel();
                    zoneTasks.remove(zone.getId());
                    return;
                }
                
                if (zone.isExpired()) {
                    plugin.getLogger().info("Zone #" + zone.getId() + " expirée - finalisation");
                    // Zone terminée, calculer le vainqueur
                    completeZone(zone);
                    this.cancel();
                    zoneTasks.remove(zone.getId());
                } else {
                    // Mettre à jour périodiquement la base de données
                    updateZoneInDatabase(zone);
                    
                    // Annoncer le temps restant toutes les 30 minutes
                    long remainingMinutes = zone.getRemainingMinutes();
                    if (remainingMinutes > 0 && remainingMinutes % 30 == 0) {
                        String timeMessage = "§7[Zone] Zone §e" + zone.getName() + " §7- Temps restant: §f" + remainingMinutes + " minutes";
                        if (zone.getCurrentController() != null) {
                            timeMessage += " §7- Contrôlée par §f" + zone.getCurrentController();
                        }
                        Bukkit.broadcastMessage(timeMessage);
                        plugin.getLogger().info("Annonce temps zone #" + zone.getId() + ": " + remainingMinutes + " minutes restantes");
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 1200L); // Toutes les minutes (1200 ticks)
        
        zoneTasks.put(zone.getId(), task);
        plugin.getLogger().info("Tâche de surveillance démarrée pour zone #" + zone.getId());
    }
    
    private void completeZone(CaptureZone zone) {
        plugin.getLogger().info("=== FINALISATION ZONE #" + zone.getId() + " ===");
        
        zone.setActive(false);
        String winningSide = zone.getWinningSide();
        
        plugin.getLogger().info("Camp vainqueur: " + (winningSide != null ? winningSide : "Aucun"));
        
        if (winningSide != null) {
            // Ajouter 20 points au camp vainqueur
            War war = plugin.getWarDataManager().getWar(zone.getWarId());
            if (war != null) {
                WarSide side = war.getSideByName(winningSide);
                if (side != null) {
                    int oldPoints = side.getPoints();
                    side.addPoints(20);
                    plugin.getWarDataManager().updateSidePoints(war.getId(), winningSide, side.getPoints());
                    
                    plugin.getLogger().info("Points ajoutés: " + oldPoints + " -> " + side.getPoints() + " pour " + winningSide);
                    
                    // Messages de victoire
                    Bukkit.broadcastMessage("§6§l=== ZONE TERMINÉE ===");
                    Bukkit.broadcastMessage("§a" + side.getDisplayName() + " §aa remporté la zone §e" + zone.getName() + " §a!");
                    Bukkit.broadcastMessage("§a+20 points attribués !");
                    
                    // Statistiques de contrôle
                    Bukkit.broadcastMessage("§7Statistiques de contrôle:");
                    for (Map.Entry<String, Long> entry : zone.getControlTimeMap().entrySet()) {
                        String controlTime = zone.formatControlTime(entry.getValue());
                        Bukkit.broadcastMessage("§7" + entry.getKey() + ": §f" + controlTime);
                    }
                    
                    Bukkit.broadcastMessage("§6§l==================");
                    
                    plugin.getLogger().info("Zone #" + zone.getId() + " terminée - Vainqueur: " + winningSide + " (+20 points)");
                    
                    // Vérifier si cette victoire termine la guerre
                    if (side.getPoints() >= war.getRequiredPoints()) {
                        plugin.getLogger().info("🏆 GUERRE TERMINÉE ! " + winningSide + " a atteint les points requis");
                        war.setActive(false);
                        plugin.getWarDataManager().endWar(war.getId());
                        
                        Bukkit.broadcastMessage("§6§l=== VICTOIRE GUERRE ! ===");
                        Bukkit.broadcastMessage("§a" + side.getDisplayName() + " §aa remporté la guerre !");
                        Bukkit.broadcastMessage("§6§l====================");
                    }
                }
            }
        } else {
            Bukkit.broadcastMessage("§7Zone §e" + zone.getName() + " §7terminée sans vainqueur (aucun contrôle)");
            plugin.getLogger().info("Zone #" + zone.getId() + " terminée sans vainqueur");
        }
        
        // Retirer le drapeau
        zone.removeFlagFromWorld();
        plugin.getLogger().info("Drapeau retiré de zone #" + zone.getId());
        
        // Sauvegarder en base
        updateZoneInDatabase(zone);
        
        // Retirer de la liste active
        activeZones.remove(zone.getId());
        plugin.getLogger().info("Zone #" + zone.getId() + " retirée des zones actives");
    }
    
    private void updateZoneInDatabase(CaptureZone zone) {
        try {
            Connection connection = plugin.getWarDataManager().getConnection();
            
            // Mettre à jour la zone
            String updateZone = """
                UPDATE capture_zones SET current_controller = ?, capture_start_time = ?, is_active = ? 
                WHERE id = ?
            """;
            
            PreparedStatement stmt = connection.prepareStatement(updateZone);
            stmt.setString(1, zone.getCurrentController());
            stmt.setString(2, zone.getCaptureStartTime() != null ? zone.getCaptureStartTime().toString() : null);
            stmt.setBoolean(3, zone.isActive());
            stmt.setInt(4, zone.getId());
            stmt.executeUpdate();
            
            // Sauvegarder les temps de contrôle
            for (Map.Entry<String, Long> entry : zone.getControlTimeMap().entrySet()) {
                // D'abord supprimer l'ancienne entrée
                String deleteControl = "DELETE FROM zone_control_time WHERE zone_id = ? AND side_name = ?";
                PreparedStatement deleteStmt = connection.prepareStatement(deleteControl);
                deleteStmt.setInt(1, zone.getId());
                deleteStmt.setString(2, entry.getKey());
                deleteStmt.executeUpdate();
                
                // Puis insérer la nouvelle
                String insertControl = "INSERT INTO zone_control_time (zone_id, side_name, control_time) VALUES (?, ?, ?)";
                PreparedStatement insertStmt = connection.prepareStatement(insertControl);
                insertStmt.setInt(1, zone.getId());
                insertStmt.setString(2, entry.getKey());
                insertStmt.setLong(3, entry.getValue());
                insertStmt.executeUpdate();
            }
            
        } catch (SQLException e) {
            plugin.getLogger().severe("Erreur lors de la mise à jour de la zone: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void loadActiveZones() {
        try {
            Connection connection = plugin.getWarDataManager().getConnection();
            
            String sql = "SELECT * FROM capture_zones WHERE is_active = TRUE";
            ResultSet rs = connection.createStatement().executeQuery(sql);
            
            while (rs.next()) {
                int id = rs.getInt("id");
                int warId = rs.getInt("war_id");
                String name = rs.getString("name");
                String worldName = rs.getString("world_name");
                double centerX = rs.getDouble("center_x");
                double centerY = rs.getDouble("center_y");
                double centerZ = rs.getDouble("center_z");
                UUID createdBy = UUID.fromString(rs.getString("created_by"));
                
                if (Bukkit.getWorld(worldName) == null) {
                    plugin.getLogger().warning("Monde " + worldName + " introuvable pour zone #" + id);
                    continue;
                }
                
                Location centerLocation = new Location(Bukkit.getWorld(worldName), centerX, centerY, centerZ);
                CaptureZone zone = new CaptureZone(id, warId, name, centerLocation, createdBy);
                
                // Charger les données supplémentaires
                zone.setCurrentController(rs.getString("current_controller"));
                if (rs.getString("capture_start_time") != null) {
                    zone.setCaptureStartTime(LocalDateTime.parse(rs.getString("capture_start_time")));
                }
                zone.setZoneEndTime(LocalDateTime.parse(rs.getString("zone_end_time")));
                
                // Charger les temps de contrôle
                loadZoneControlTimes(zone);
                
                // Replacer le drapeau avec la bonne couleur
                War war = plugin.getWarDataManager().getWar(warId);
                if (war != null && zone.getCurrentController() != null) {
                    WarSide controller = war.getSideByName(zone.getCurrentController());
                    if (controller != null) {
                        zone.updateFlagColor(controller.getColor());
                    }
                } else {
                    zone.placeFlagInWorld(); // Drapeau blanc si pas de contrôleur
                }
                
                activeZones.put(id, zone);
                
                // Redémarrer la tâche si pas expirée
                if (!zone.isExpired()) {
                    startZoneTask(zone);
                } else {
                    plugin.getLogger().info("Zone #" + id + " expirée au chargement - finalisation");
                    completeZone(zone);
                }
                
                plugin.getLogger().info("Zone chargée: #" + id + " - " + name + " (Contrôleur: " + 
                                      (zone.getCurrentController() != null ? zone.getCurrentController() : "Aucun") + ")");
            }
            
        } catch (SQLException e) {
            plugin.getLogger().severe("Erreur lors du chargement des zones: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void loadZoneControlTimes(CaptureZone zone) {
        try {
            Connection connection = plugin.getWarDataManager().getConnection();
            
            String sql = "SELECT side_name, control_time FROM zone_control_time WHERE zone_id = ?";
            PreparedStatement stmt = connection.prepareStatement(sql);
            stmt.setInt(1, zone.getId());
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                zone.getControlTimeMap().put(rs.getString("side_name"), rs.getLong("control_time"));
            }
            
        } catch (SQLException e) {
            plugin.getLogger().severe("Erreur lors du chargement des temps de contrôle: " + e.getMessage());
        }
    }
    
    public CaptureZone getCaptureZone(int zoneId) {
        return activeZones.get(zoneId);
    }
    
    public Map<Integer, CaptureZone> getActiveZones() {
        return new HashMap<>(activeZones);
    }
    
    public CaptureZone getCaptureZoneAtLocation(Location location) {
        for (CaptureZone zone : activeZones.values()) {
            if (zone.isActive() && !zone.isExpired() && zone.isInZone(location)) {
                return zone;
            }
        }
        return null;
    }
    
    public void stopAllZoneTasks() {
        plugin.getLogger().info("Arrêt de toutes les tâches de zones (" + zoneTasks.size() + ")");
        for (BukkitTask task : zoneTasks.values()) {
            task.cancel();
        }
        zoneTasks.clear();
    }
    
    public void deleteZone(int zoneId) {
        CaptureZone zone = activeZones.get(zoneId);
        if (zone != null) {
            // Arrêter la tâche
            BukkitTask task = zoneTasks.get(zoneId);
            if (task != null) {
                task.cancel();
                zoneTasks.remove(zoneId);
            }
            
            // Retirer le drapeau
            zone.removeFlagFromWorld();
            
            // Supprimer de la base
            try {
                Connection connection = plugin.getWarDataManager().getConnection();
                connection.createStatement().execute("DELETE FROM zone_control_time WHERE zone_id = " + zoneId);
                connection.createStatement().execute("DELETE FROM capture_zones WHERE id = " + zoneId);
            } catch (SQLException e) {
                plugin.getLogger().severe("Erreur lors de la suppression de la zone: " + e.getMessage());
            }
            
            activeZones.remove(zoneId);
            plugin.getLogger().info("Zone #" + zoneId + " supprimée");
        }
    }
}