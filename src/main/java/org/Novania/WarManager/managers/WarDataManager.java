package org.Novania.WarManager.managers;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.Novania.WarManager.WarManager;
import org.Novania.WarManager.models.War;
import org.Novania.WarManager.models.WarSide;

public class WarDataManager {
    
    private final WarManager plugin;
    private Connection connection;
    private Map<Integer, War> activeWars;
    
    public WarDataManager(WarManager plugin) {
        this.plugin = plugin;
        this.activeWars = new ConcurrentHashMap<>();
    }
    
    public void initDatabase() {
        try {
            String dbType = plugin.getConfigManager().getDatabaseType();
            
            if (dbType.equalsIgnoreCase("MYSQL")) {
                initMySQLConnection();
            } else {
                initSQLiteConnection();
            }
            
            createTables();
            loadActiveWars();
            
            plugin.getLogger().info("Base de données initialisée avec succès (" + dbType + ")");
            
        } catch (Exception e) {
            plugin.getLogger().severe("Erreur lors de l'initialisation de la base de données: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void initSQLiteConnection() throws SQLException {
        String url = "jdbc:sqlite:" + plugin.getDataFolder().getAbsolutePath() + "/warmanager.db";
        connection = DriverManager.getConnection(url);
        plugin.getLogger().info("Connexion SQLite établie");
    }
    
    private void initMySQLConnection() throws SQLException {
        String host = plugin.getConfigManager().getMysqlHost();
        int port = plugin.getConfigManager().getMysqlPort();
        String database = plugin.getConfigManager().getMysqlDatabase();
        String username = plugin.getConfigManager().getMysqlUsername();
        String password = plugin.getConfigManager().getMysqlPassword();
        
        String url = "jdbc:mysql://" + host + ":" + port + "/" + database + "?useSSL=false&allowPublicKeyRetrieval=true";
        connection = DriverManager.getConnection(url, username, password);
        plugin.getLogger().info("Connexion MySQL établie");
    }
    
    private void createTables() throws SQLException {
        // Table des guerres
        String createWarsTable = """
            CREATE TABLE IF NOT EXISTS wars (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                name VARCHAR(100) NOT NULL,
                casus_belli_type VARCHAR(50) NOT NULL,
                required_points INTEGER NOT NULL,
                start_date TEXT NOT NULL,
                end_date TEXT,
                is_active BOOLEAN DEFAULT TRUE,
                created_by VARCHAR(36) NOT NULL
            )
        """;
        
        // Table des camps
        String createSidesTable = """
            CREATE TABLE IF NOT EXISTS war_sides (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                war_id INTEGER NOT NULL,
                name VARCHAR(50) NOT NULL,
                color VARCHAR(10) NOT NULL,
                points INTEGER DEFAULT 0,
                kills INTEGER DEFAULT 0,
                FOREIGN KEY (war_id) REFERENCES wars(id)
            )
        """;
        
        // Table des nations
        String createNationsTable = """
            CREATE TABLE IF NOT EXISTS side_nations (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                side_id INTEGER NOT NULL,
                nation_name VARCHAR(100) NOT NULL,
                FOREIGN KEY (side_id) REFERENCES war_sides(id)
            )
        """;
        
        // Table des kills
        String createKillsTable = """
            CREATE TABLE IF NOT EXISTS war_kills (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                war_id INTEGER NOT NULL,
                killer_uuid VARCHAR(36) NOT NULL,
                victim_uuid VARCHAR(36) NOT NULL,
                killer_nation VARCHAR(100) NOT NULL,
                victim_nation VARCHAR(100) NOT NULL,
                kill_time TEXT NOT NULL,
                FOREIGN KEY (war_id) REFERENCES wars(id)
            )
        """;
        
        connection.createStatement().execute(createWarsTable);
        connection.createStatement().execute(createSidesTable);
        connection.createStatement().execute(createNationsTable);
        connection.createStatement().execute(createKillsTable);
        
        plugin.getLogger().info("Tables de base de données créées");
    }
    
    public War createWar(String name, String casusBeliType, int requiredPoints, UUID createdBy) {
        try {
            String sql = "INSERT INTO wars (name, casus_belli_type, required_points, start_date, created_by) VALUES (?, ?, ?, ?, ?)";
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
                return war;
            }
            
        } catch (SQLException e) {
            plugin.getLogger().severe("Erreur lors de la création de la guerre: " + e.getMessage());
        }
        
        return null;
    }
    
    public War getWar(int warId) {
        return activeWars.get(warId);
    }
    
    public Map<Integer, War> getActiveWars() {
        return new ConcurrentHashMap<>(activeWars);
    }
    
    public void addSideToWar(int warId, String sideName, String color) {
        try {
            String sql = "INSERT INTO war_sides (war_id, name, color) VALUES (?, ?, ?)";
            PreparedStatement stmt = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            stmt.setInt(1, warId);
            stmt.setString(2, sideName);
            stmt.setString(3, color);
            
            stmt.executeUpdate();
            ResultSet keys = stmt.getGeneratedKeys();
            
            if (keys.next()) {
                int sideId = keys.getInt(1);
                War war = activeWars.get(warId);
                if (war != null) {
                    WarSide side = new WarSide(sideName, color);
                    war.addSide(side);
                }
            }
            
        } catch (SQLException e) {
            plugin.getLogger().severe("Erreur lors de l'ajout du camp: " + e.getMessage());
        }
    }
    
    public void removeSideFromWar(int warId, String sideName) {
        try {
            // Supprimer les nations d'abord
            String deleteNations = "DELETE FROM side_nations WHERE side_id IN (SELECT id FROM war_sides WHERE war_id = ? AND name = ?)";
            PreparedStatement stmt1 = connection.prepareStatement(deleteNations);
            stmt1.setInt(1, warId);
            stmt1.setString(2, sideName);
            stmt1.executeUpdate();
            
            // Supprimer le camp
            String deleteSide = "DELETE FROM war_sides WHERE war_id = ? AND name = ?";
            PreparedStatement stmt2 = connection.prepareStatement(deleteSide);
            stmt2.setInt(1, warId);
            stmt2.setString(2, sideName);
            stmt2.executeUpdate();
            
            // Mettre à jour en mémoire
            War war = activeWars.get(warId);
            if (war != null) {
                war.getSides().removeIf(side -> side.getName().equals(sideName));
            }
            
        } catch (SQLException e) {
            plugin.getLogger().severe("Erreur lors de la suppression du camp: " + e.getMessage());
        }
    }
    
    public void addNationToSide(int warId, String sideName, String nationName) {
        try {
            // Récupérer l'ID du camp
            String getSideId = "SELECT id FROM war_sides WHERE war_id = ? AND name = ?";
            PreparedStatement stmt1 = connection.prepareStatement(getSideId);
            stmt1.setInt(1, warId);
            stmt1.setString(2, sideName);
            ResultSet rs = stmt1.executeQuery();
            
            if (rs.next()) {
                int sideId = rs.getInt(1);
                
                // Ajouter la nation
                String addNation = "INSERT INTO side_nations (side_id, nation_name) VALUES (?, ?)";
                PreparedStatement stmt2 = connection.prepareStatement(addNation);
                stmt2.setInt(1, sideId);
                stmt2.setString(2, nationName);
                stmt2.executeUpdate();
                
                // Mettre à jour en mémoire
                War war = activeWars.get(warId);
                if (war != null) {
                    WarSide side = war.getSideByName(sideName);
                    if (side != null) {
                        side.addNation(nationName);
                    }
                }
            }
            
        } catch (SQLException e) {
            plugin.getLogger().severe("Erreur lors de l'ajout de la nation: " + e.getMessage());
        }
    }
    
    public void removeNationFromSide(int warId, String sideName, String nationName) {
        try {
            String sql = """
                DELETE FROM side_nations 
                WHERE nation_name = ? 
                AND side_id IN (SELECT id FROM war_sides WHERE war_id = ? AND name = ?)
            """;
            PreparedStatement stmt = connection.prepareStatement(sql);
            stmt.setString(1, nationName);
            stmt.setInt(2, warId);
            stmt.setString(3, sideName);
            stmt.executeUpdate();
            
            // Mettre à jour en mémoire
            War war = activeWars.get(warId);
            if (war != null) {
                WarSide side = war.getSideByName(sideName);
                if (side != null) {
                    side.removeNation(nationName);
                }
            }
            
        } catch (SQLException e) {
            plugin.getLogger().severe("Erreur lors de la suppression de la nation: " + e.getMessage());
        }
    }
    
    public void recordKill(int warId, UUID killerUuid, UUID victimUuid, String killerNation, String victimNation) {
        try {
            String sql = "INSERT INTO war_kills (war_id, killer_uuid, victim_uuid, killer_nation, victim_nation, kill_time) VALUES (?, ?, ?, ?, ?, ?)";
            PreparedStatement stmt = connection.prepareStatement(sql);
            stmt.setInt(1, warId);
            stmt.setString(2, killerUuid.toString());
            stmt.setString(3, victimUuid.toString());
            stmt.setString(4, killerNation);
            stmt.setString(5, victimNation);
            stmt.setString(6, LocalDateTime.now().toString());
            
            stmt.executeUpdate();
            
            // Mettre à jour les kills en mémoire
            War war = activeWars.get(warId);
            if (war != null) {
                for (WarSide side : war.getSides()) {
                    if (side.hasNation(killerNation)) {
                        side.addKill();
                        break;
                    }
                }
            }
            
        } catch (SQLException e) {
            plugin.getLogger().severe("Erreur lors de l'enregistrement du kill: " + e.getMessage());
        }
    }
    
    public void updateSidePoints(int warId, String sideName, int newPoints) {
        try {
            String sql = "UPDATE war_sides SET points = ? WHERE war_id = ? AND name = ?";
            PreparedStatement stmt = connection.prepareStatement(sql);
            stmt.setInt(1, newPoints);
            stmt.setInt(2, warId);
            stmt.setString(3, sideName);
            stmt.executeUpdate();
            
        } catch (SQLException e) {
            plugin.getLogger().severe("Erreur lors de la mise à jour des points: " + e.getMessage());
        }
    }
    
    public void endWar(int warId) {
        try {
            String sql = "UPDATE wars SET is_active = FALSE, end_date = ? WHERE id = ?";
            PreparedStatement stmt = connection.prepareStatement(sql);
            stmt.setString(1, LocalDateTime.now().toString());
            stmt.setInt(2, warId);
            stmt.executeUpdate();
            
            // Mettre à jour en mémoire
            War war = activeWars.get(warId);
            if (war != null) {
                war.setActive(false);
                war.setEndDate(LocalDateTime.now());
            }
            
        } catch (SQLException e) {
            plugin.getLogger().severe("Erreur lors de la fin de guerre: " + e.getMessage());
        }
    }
    
    public void deleteWar(int warId) {
        try {
            // Supprimer dans l'ordre : nations -> camps -> kills -> guerre
            connection.createStatement().execute("DELETE FROM side_nations WHERE side_id IN (SELECT id FROM war_sides WHERE war_id = " + warId + ")");
            connection.createStatement().execute("DELETE FROM war_sides WHERE war_id = " + warId);
            connection.createStatement().execute("DELETE FROM war_kills WHERE war_id = " + warId);
            connection.createStatement().execute("DELETE FROM wars WHERE id = " + warId);
            
            activeWars.remove(warId);
            
        } catch (SQLException e) {
            plugin.getLogger().severe("Erreur lors de la suppression de guerre: " + e.getMessage());
        }
    }
    
    public void loadActiveWars() {
        try {
            String sql = "SELECT * FROM wars WHERE is_active = TRUE";
            ResultSet rs = connection.createStatement().executeQuery(sql);
            
            while (rs.next()) {
                int id = rs.getInt("id");
                String name = rs.getString("name");
                String casusBeliType = rs.getString("casus_belli_type");
                int requiredPoints = rs.getInt("required_points");
                UUID createdBy = UUID.fromString(rs.getString("created_by"));
                
                War war = new War(id, name, casusBeliType, requiredPoints, createdBy);
                war.setStartDate(LocalDateTime.parse(rs.getString("start_date")));
                
                // Charger les camps
                loadWarSides(war);
                
                activeWars.put(id, war);
            }
            
            plugin.getLogger().info(activeWars.size() + " guerre(s) active(s) chargée(s)");
            
        } catch (SQLException e) {
            plugin.getLogger().severe("Erreur lors du chargement des guerres: " + e.getMessage());
        }
    }
    
    private void loadWarSides(War war) throws SQLException {
        String sql = "SELECT * FROM war_sides WHERE war_id = ?";
        PreparedStatement stmt = connection.prepareStatement(sql);
        stmt.setInt(1, war.getId());
        ResultSet rs = stmt.executeQuery();
        
        while (rs.next()) {
            int sideId = rs.getInt("id");
            String sideName = rs.getString("name");
            String color = rs.getString("color");
            int points = rs.getInt("points");
            int kills = rs.getInt("kills");
            
            WarSide side = new WarSide(sideName, color);
            side.setPoints(points);
            side.setKills(kills);
            
            // Charger les nations du camp
            loadSideNations(side, sideId);
            
            war.addSide(side);
        }
    }
    
    private void loadSideNations(WarSide side, int sideId) throws SQLException {
        String sql = "SELECT nation_name FROM side_nations WHERE side_id = ?";
        PreparedStatement stmt = connection.prepareStatement(sql);
        stmt.setInt(1, sideId);
        ResultSet rs = stmt.executeQuery();
        
        while (rs.next()) {
            side.addNation(rs.getString("nation_name"));
        }
    }
    
    public void reloadActiveWars() {
        activeWars.clear();
        loadActiveWars();
    }
    
    public boolean isDatabaseConnected() {
        try {
            return connection != null && !connection.isClosed();
        } catch (SQLException e) {
            return false;
        }
    }
    
    public Connection getConnection() {
        return connection;
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