package org.Novania.WarManager.managers;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.Novania.WarManager.WarManager;
import org.Novania.WarManager.models.War;
import org.Novania.WarManager.models.WarSide;

public class WarDataManager {
    
    private final WarManager plugin;
    private Connection connection;
    private final Map<Integer, War> activeWars;
    
    public WarDataManager(WarManager plugin) {
        this.plugin = plugin;
        this.activeWars = new ConcurrentHashMap<>();
    }
    
    public void initDatabase() {
        try {
            String dbType = plugin.getConfig().getString("database.type", "SQLITE");
            
            if (dbType.equalsIgnoreCase("SQLITE")) {
                File dataFolder = plugin.getDataFolder();
                if (!dataFolder.exists()) {
                    dataFolder.mkdirs();
                }
                
                String url = "jdbc:sqlite:" + dataFolder + "/wars.db";
                connection = DriverManager.getConnection(url);
                plugin.getLogger().info("Base de données SQLite initialisée: " + dataFolder + "/wars.db");
            } else {
                // Configuration MySQL
                String host = plugin.getConfig().getString("database.mysql.host");
                int port = plugin.getConfig().getInt("database.mysql.port");
                String database = plugin.getConfig().getString("database.mysql.database");
                String username = plugin.getConfig().getString("database.mysql.username");
                String password = plugin.getConfig().getString("database.mysql.password");
                
                String url = "jdbc:mysql://" + host + ":" + port + "/" + database;
                connection = DriverManager.getConnection(url, username, password);
                plugin.getLogger().info("Base de données MySQL connectée");
            }
            
            createTables();
            loadActiveWars();
            
            plugin.getLogger().info("Base de données initialisée avec " + activeWars.size() + " guerre(s) active(s)");
            
        } catch (SQLException e) {
            plugin.getLogger().severe("Erreur lors de l'initialisation de la base de données: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    public boolean isDatabaseConnected() {
        try {
            return connection != null && !connection.isClosed();
        } catch (SQLException e) {
            return false;
        }
    }
    
    // NOUVEAU: Getter pour la connexion (nécessaire pour CaptureZoneManager)
    public Connection getConnection() {
        return connection;
    }
    
    private void createTables() throws SQLException {
        String createWarsTable = """
            CREATE TABLE IF NOT EXISTS wars (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                name VARCHAR(100) NOT NULL,
                casus_beli_type VARCHAR(50) NOT NULL,
                required_points INTEGER NOT NULL,
                start_date TEXT NOT NULL,
                end_date TEXT,
                is_active BOOLEAN DEFAULT TRUE,
                created_by VARCHAR(36) NOT NULL
            )
        """;
        
        String createWarSidesTable = """
            CREATE TABLE IF NOT EXISTS war_sides (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                war_id INTEGER NOT NULL,
                side_name VARCHAR(50) NOT NULL,
                color VARCHAR(10) NOT NULL,
                points INTEGER DEFAULT 0,
                kills INTEGER DEFAULT 0,
                FOREIGN KEY (war_id) REFERENCES wars(id)
            )
        """;
        
        String createWarNationsTable = """
            CREATE TABLE IF NOT EXISTS war_nations (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                side_id INTEGER NOT NULL,
                nation_name VARCHAR(100) NOT NULL,
                FOREIGN KEY (side_id) REFERENCES war_sides(id)
            )
        """;
        
        String createWarKillsTable = """
            CREATE TABLE IF NOT EXISTS war_kills (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                war_id INTEGER NOT NULL,
                killer_uuid VARCHAR(36) NOT NULL,
                victim_uuid VARCHAR(36) NOT NULL,
                killer_nation VARCHAR(100),
                victim_nation VARCHAR(100),
                kill_date TEXT NOT NULL,
                points_awarded INTEGER DEFAULT 1,
                FOREIGN KEY (war_id) REFERENCES wars(id)
            )
        """;
        
        connection.createStatement().execute(createWarsTable);
        connection.createStatement().execute(createWarSidesTable);
        connection.createStatement().execute(createWarNationsTable);
        connection.createStatement().execute(createWarKillsTable);
        
        plugin.getLogger().info("Tables de base de données créées avec succès");
    }
    
    public War createWar(String name, String casusBeliType, int requiredPoints, UUID createdBy) {
        try {
            String sql = "INSERT INTO wars (name, casus_beli_type, required_points, start_date, created_by) VALUES (?, ?, ?, ?, ?)";
            PreparedStatement stmt = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            stmt.setString(1, name);
            stmt.setString(2, casusBeliType);
            stmt.setInt(3, requiredPoints);
            stmt.setString(4, LocalDateTime.now().toString());
            stmt.setString(5, createdBy.toString());
            
            stmt.executeUpdate();
            ResultSet keys = stmt.getGeneratedKeys();
            
            if (keys.next()) {
                int warId = keys.getInt(1);
                War war = new War(warId, name, casusBeliType, requiredPoints, createdBy);
                activeWars.put(warId, war);
                plugin.getLogger().info("Guerre créée avec l'ID: " + warId);
                return war;
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Erreur lors de la création de la guerre: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }
    
    public void addSideToWar(int warId, String sideName, String color) {
        try {
            String sql = "INSERT INTO war_sides (war_id, side_name, color) VALUES (?, ?, ?)";
            PreparedStatement stmt = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            stmt.setInt(1, warId);
            stmt.setString(2, sideName);
            stmt.setString(3, color);
            
            stmt.executeUpdate();
            ResultSet keys = stmt.getGeneratedKeys();
            
            if (keys.next()) {
                War war = activeWars.get(warId);
                if (war != null) {
                    WarSide side = new WarSide(sideName, color);
                    war.addSide(side);
                    plugin.getLogger().info("Camp ajouté: " + sideName + " à la guerre " + warId);
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Erreur lors de l'ajout du camp: " + e.getMessage());
        }
    }
    
    public void removeSideFromWar(int warId, String sideName) {
        War war = activeWars.get(warId);
        if (war != null) {
            war.getSides().removeIf(side -> side.getName().equals(sideName));
            
            try {
                // Supprimer les nations du camp d'abord
                String sqlNations = "DELETE FROM war_nations WHERE side_id IN (SELECT id FROM war_sides WHERE war_id = ? AND side_name = ?)";
                PreparedStatement stmtNations = connection.prepareStatement(sqlNations);
                stmtNations.setInt(1, warId);
                stmtNations.setString(2, sideName);
                stmtNations.executeUpdate();
                
                // Supprimer le camp
                String sql = "DELETE FROM war_sides WHERE war_id = ? AND side_name = ?";
                PreparedStatement stmt = connection.prepareStatement(sql);
                stmt.setInt(1, warId);
                stmt.setString(2, sideName);
                stmt.executeUpdate();
                
                plugin.getLogger().info("Camp supprimé: " + sideName + " de la guerre " + warId);
            } catch (SQLException e) {
                plugin.getLogger().severe("Erreur lors de la suppression du camp: " + e.getMessage());
            }
        }
    }
    
    public void addNationToSide(int warId, String sideName, String nationName) {
        plugin.getLogger().info("addNationToSide appelé: guerre=" + warId + ", camp=" + sideName + ", nation=" + nationName);
        
        War war = activeWars.get(warId);
        if (war != null) {
            WarSide side = war.getSideByName(sideName);
            if (side != null) {
                plugin.getLogger().info("Camp trouvé. Nations avant: " + side.getNations());
                
                // Vérifier que la nation n'est pas déjà dans ce camp
                if (!side.hasNation(nationName)) {
                    side.addNation(nationName);
                    plugin.getLogger().info("Nation ajoutée en mémoire. Nations après: " + side.getNations());
                    
                    try {
                        String sql = "INSERT INTO war_nations (side_id, nation_name) VALUES ((SELECT id FROM war_sides WHERE war_id = ? AND side_name = ?), ?)";
                        PreparedStatement stmt = connection.prepareStatement(sql);
                        stmt.setInt(1, warId);
                        stmt.setString(2, sideName);
                        stmt.setString(3, nationName);
                        int inserted = stmt.executeUpdate();
                        
                        plugin.getLogger().info("Lignes insérées en BDD: " + inserted + " pour nation " + nationName);
                        
                    } catch (SQLException e) {
                        plugin.getLogger().severe("Erreur lors de l'ajout de la nation " + nationName + ": " + e.getMessage());
                        e.printStackTrace();
                    }
                } else {
                    plugin.getLogger().info("Nation " + nationName + " déjà dans le camp " + sideName);
                }
            } else {
                plugin.getLogger().warning("Camp " + sideName + " introuvable dans la guerre " + warId);
            }
        } else {
            plugin.getLogger().warning("Guerre " + warId + " introuvable");
        }
    }
    
    public void removeNationFromSide(int warId, String sideName, String nationName) {
        plugin.getLogger().info("removeNationFromSide appelé: guerre=" + warId + ", camp=" + sideName + ", nation=" + nationName);
        
        War war = activeWars.get(warId);
        if (war != null) {
            WarSide side = war.getSideByName(sideName);
            if (side != null) {
                plugin.getLogger().info("Camp trouvé. Nations avant: " + side.getNations());
                
                // Retirer SEULEMENT la nation spécifiée
                boolean removed = side.removeNation(nationName);
                plugin.getLogger().info("Nation " + nationName + " retirée: " + removed + ". Nations après: " + side.getNations());
                
                if (removed) {
                    try {
                        // Supprimer SEULEMENT cette nation de la base de données
                        String sql = "DELETE FROM war_nations WHERE side_id = (SELECT id FROM war_sides WHERE war_id = ? AND side_name = ?) AND nation_name = ?";
                        PreparedStatement stmt = connection.prepareStatement(sql);
                        stmt.setInt(1, warId);
                        stmt.setString(2, sideName);
                        stmt.setString(3, nationName);
                        int deleted = stmt.executeUpdate();
                        
                        plugin.getLogger().info("Lignes supprimées en BDD: " + deleted + " pour nation " + nationName);
                        
                    } catch (SQLException e) {
                        plugin.getLogger().severe("Erreur lors de la suppression de la nation " + nationName + ": " + e.getMessage());
                        e.printStackTrace();
                    }
                } else {
                    plugin.getLogger().warning("Nation " + nationName + " n'était pas dans le camp " + sideName);
                }
            } else {
                plugin.getLogger().warning("Camp " + sideName + " introuvable dans la guerre " + warId);
            }
        } else {
            plugin.getLogger().warning("Guerre " + warId + " introuvable");
        }
    }
    
    // CORRIGÉ: Méthode recordKill - Ne gère que l'enregistrement du kill, pas les points
    public void recordKill(int warId, UUID killerUuid, UUID victimUuid, String killerNation, String victimNation) {
        try {
            String sql = "INSERT INTO war_kills (war_id, killer_uuid, victim_uuid, killer_nation, victim_nation, kill_date) VALUES (?, ?, ?, ?, ?, ?)";
            PreparedStatement stmt = connection.prepareStatement(sql);
            stmt.setInt(1, warId);
            stmt.setString(2, killerUuid.toString());
            stmt.setString(3, victimUuid.toString());
            stmt.setString(4, killerNation);
            stmt.setString(5, victimNation);
            stmt.setString(6, LocalDateTime.now().toString());
            stmt.executeUpdate();
            
            plugin.getLogger().info("Kill enregistré en BDD: " + killerNation + " -> " + victimNation);
            
            // Seulement incrémenter le compteur de kills (pas les points)
            War war = activeWars.get(warId);
            if (war != null) {
                for (WarSide side : war.getSides()) {
                    if (side.hasNation(killerNation)) {
                        side.addKill(); // Maintenant cette méthode n'ajoute QUE les kills
                        updateSideKills(warId, side.getName(), side.getKills());
                        plugin.getLogger().info("Kills mis à jour: camp " + side.getName() + " -> " + side.getKills() + " kills");
                        break;
                    }
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Erreur lors de l'enregistrement du kill: " + e.getMessage());
        }
    }
    
    public void updateSidePoints(int warId, String sideName, int points) {
        try {
            // Récupérer les kills actuels
            String selectSql = "SELECT kills FROM war_sides WHERE war_id = ? AND side_name = ?";
            PreparedStatement selectStmt = connection.prepareStatement(selectSql);
            selectStmt.setInt(1, warId);
            selectStmt.setString(2, sideName);
            ResultSet rs = selectStmt.executeQuery();
            
            int currentKills = 0;
            if (rs.next()) {
                currentKills = rs.getInt("kills");
            }
            
            // Mettre à jour les points en gardant les kills
            String sql = "UPDATE war_sides SET points = ? WHERE war_id = ? AND side_name = ?";
            PreparedStatement stmt = connection.prepareStatement(sql);
            stmt.setInt(1, points);
            stmt.setInt(2, warId);
            stmt.setString(3, sideName);
            int updated = stmt.executeUpdate();
            
            plugin.getLogger().info("Points mis à jour en BDD: guerre #" + warId + ", camp " + sideName + " -> " + points + " points (" + updated + " ligne(s) affectée(s))");
            
        } catch (SQLException e) {
            plugin.getLogger().severe("Erreur lors de la mise à jour des points: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    public void updateSideKills(int warId, String sideName, int kills) {
        try {
            String sql = "UPDATE war_sides SET kills = ? WHERE war_id = ? AND side_name = ?";
            PreparedStatement stmt = connection.prepareStatement(sql);
            stmt.setInt(1, kills);
            stmt.setInt(2, warId);
            stmt.setString(3, sideName);
            stmt.executeUpdate();
            
            plugin.getLogger().info("Kills mis à jour en BDD: guerre #" + warId + ", camp " + sideName + " -> " + kills + " kills");
            
        } catch (SQLException e) {
            plugin.getLogger().severe("Erreur lors de la mise à jour des kills: " + e.getMessage());
        }
    }
    
    public void reloadActiveWars() {
        try {
            activeWars.clear();
            loadActiveWars();
            plugin.getLogger().info("Guerres actives rechargées : " + activeWars.size() + " guerre(s)");
        } catch (Exception e) {
            plugin.getLogger().severe("Erreur lors du rechargement des guerres : " + e.getMessage());
        }
    }
    
    private void loadActiveWars() {
        try {
            String sql = "SELECT * FROM wars WHERE is_active = TRUE";
            ResultSet rs = connection.createStatement().executeQuery(sql);
            
            while (rs.next()) {
                int id = rs.getInt("id");
                String name = rs.getString("name");
                String casusBeliType = rs.getString("casus_beli_type");
                int requiredPoints = rs.getInt("required_points");
                UUID createdBy = UUID.fromString(rs.getString("created_by"));
                
                War war = new War(id, name, casusBeliType, requiredPoints, createdBy);
                loadWarSides(war);
                activeWars.put(id, war);
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Erreur lors du chargement des guerres: " + e.getMessage());
        }
    }
    
    private void loadWarSides(War war) {
        try {
            String sql = "SELECT * FROM war_sides WHERE war_id = ?";
            PreparedStatement stmt = connection.prepareStatement(sql);
            stmt.setInt(1, war.getId());
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                String sideName = rs.getString("side_name");
                String color = rs.getString("color");
                int points = rs.getInt("points");
                int kills = rs.getInt("kills");
                
                WarSide side = new WarSide(sideName, color);
                side.setPoints(points);
                side.setKills(kills);
                
                loadSideNations(side, rs.getInt("id"));
                war.addSide(side);
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Erreur lors du chargement des camps: " + e.getMessage());
        }
    }
    
    private void loadSideNations(WarSide side, int sideId) {
        try {
            String sql = "SELECT nation_name FROM war_nations WHERE side_id = ?";
            PreparedStatement stmt = connection.prepareStatement(sql);
            stmt.setInt(1, sideId);
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                side.addNation(rs.getString("nation_name"));
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Erreur lors du chargement des nations: " + e.getMessage());
        }
    }
    
    public Map<Integer, War> getActiveWars() {
        return new HashMap<>(activeWars);
    }
    
    public List<War> getAllWars() {
        List<War> allWars = new ArrayList<>();
        try {
            String sql = "SELECT * FROM wars ORDER BY id DESC";
            ResultSet rs = connection.createStatement().executeQuery(sql);
            
            while (rs.next()) {
                int id = rs.getInt("id");
                String name = rs.getString("name");
                String casusBeliType = rs.getString("casus_beli_type");
                int requiredPoints = rs.getInt("required_points");
                UUID createdBy = UUID.fromString(rs.getString("created_by"));
                boolean isActive = rs.getBoolean("is_active");
                
                War war = new War(id, name, casusBeliType, requiredPoints, createdBy);
                war.setActive(isActive);
                
                if (rs.getString("end_date") != null) {
                    war.setEndDate(LocalDateTime.parse(rs.getString("end_date")));
                }
                
                loadWarSides(war);
                allWars.add(war);
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Erreur lors du chargement de toutes les guerres: " + e.getMessage());
        }
        return allWars;
    }
    
    public War getWar(int warId) {
        War war = activeWars.get(warId);
        if (war != null) {
            return war;
        }
        
        // Si pas dans le cache, chercher en base
        try {
            String sql = "SELECT * FROM wars WHERE id = ?";
            PreparedStatement stmt = connection.prepareStatement(sql);
            stmt.setInt(1, warId);
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                String name = rs.getString("name");
                String casusBeliType = rs.getString("casus_beli_type");
                int requiredPoints = rs.getInt("required_points");
                UUID createdBy = UUID.fromString(rs.getString("created_by"));
                boolean isActive = rs.getBoolean("is_active");
                
                war = new War(warId, name, casusBeliType, requiredPoints, createdBy);
                war.setActive(isActive);
                
                if (rs.getString("end_date") != null) {
                    war.setEndDate(LocalDateTime.parse(rs.getString("end_date")));
                }
                
                loadWarSides(war);
                
                // Remettre en cache si active
                if (isActive) {
                    activeWars.put(warId, war);
                }
                
                return war;
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Erreur lors de la récupération de la guerre " + warId + ": " + e.getMessage());
        }
        
        return null;
    }
    
    public void endWar(int warId) {
        War war = activeWars.get(warId);
        if (war != null) {
            war.setActive(false);
            war.setEndDate(LocalDateTime.now());
            activeWars.remove(warId);
            
            try {
                String sql = "UPDATE wars SET is_active = FALSE, end_date = ? WHERE id = ?";
                PreparedStatement stmt = connection.prepareStatement(sql);
                stmt.setString(1, LocalDateTime.now().toString());
                stmt.setInt(2, warId);
                stmt.executeUpdate();
                plugin.getLogger().info("Guerre #" + warId + " terminée");
            } catch (SQLException e) {
                plugin.getLogger().severe("Erreur lors de la fin de guerre: " + e.getMessage());
            }
        }
    }
    
    public void deleteWar(int warId) {
        War war = getWar(warId);
        if (war != null) {
            activeWars.remove(warId);
            
            try {
                // Supprimer en cascade : kills, nations, sides, puis war
                connection.createStatement().execute("DELETE FROM war_kills WHERE war_id = " + warId);
                connection.createStatement().execute("DELETE FROM war_nations WHERE side_id IN (SELECT id FROM war_sides WHERE war_id = " + warId + ")");
                connection.createStatement().execute("DELETE FROM war_sides WHERE war_id = " + warId);
                connection.createStatement().execute("DELETE FROM wars WHERE id = " + warId);
                
                plugin.getLogger().info("Guerre #" + warId + " supprimée définitivement");
            } catch (SQLException e) {
                plugin.getLogger().severe("Erreur lors de la suppression de la guerre: " + e.getMessage());
            }
        }
    }
    
    public void closeDatabase() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                plugin.getLogger().info("Connexion à la base de données fermée");
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Erreur lors de la fermeture de la base de données: " + e.getMessage());
        }
    }
}