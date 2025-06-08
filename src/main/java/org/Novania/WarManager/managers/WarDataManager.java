package org.Novania.WarManager.listeners;

import org.Novania.WarManager.WarManager;
import org.Novania.WarManager.models.War;
import org.Novania.WarManager.models.WarSide;
import org.Novania.WarManager.utils.MessageUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;

import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Resident;

import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

public class PlayerDeathListener implements Listener {
    
    private final WarManager plugin;
    private final Map<String, Long> lastKillTime = new ConcurrentHashMap<>();
    private static final long KILL_COOLDOWN = 1000; // 1 seconde entre les kills du même joueur
    
    public PlayerDeathListener(WarManager plugin) {
        this.plugin = plugin;
    }
    
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = false)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player victim = event.getEntity();
        Player killer = victim.getKiller();
        
        // Vérifications rapides
        if (killer == null || !killer.isOnline() || !victim.isOnline()) {
            return;
        }
        
        // Anti-spam des kills
        String killerName = killer.getName();
        long now = System.currentTimeMillis();
        Long lastTime = lastKillTime.get(killerName);
        if (lastTime != null && (now - lastTime) < KILL_COOLDOWN) {
            return;
        }
        lastKillTime.put(killerName, now);
        
        // Traitement asynchrone pour éviter les blocages
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            processWarKill(killer, victim);
        });
    }
    
    private void processWarKill(Player killer, Player victim) {
        try {
            // Obtenir les nations des joueurs
            String killerNation = getNationNameCached(killer);
            String victimNation = getNationNameCached(victim);
            
            if (killerNation == null || victimNation == null || killerNation.equals(victimNation)) {
                return; // Pas de nations ou même nation
            }
            
            // Vérifier les guerres actives (cache local pour optimisation)
            War activeWar = findActiveWarForNations(killerNation, victimNation);
            if (activeWar == null) {
                return;
            }
            
            // Obtenir le camp du tueur
            WarSide killerSide = getPlayerSide(activeWar, killerNation);
            if (killerSide == null) {
                return;
            }
            
            // Traitement du kill
            processKillReward(activeWar, killerSide, killer, victim, killerNation, victimNation);
            
        } catch (Exception e) {
            plugin.getLogger().severe("Erreur dans processWarKill: " + e.getMessage());
        }
    }
    
    private String getNationNameCached(Player player) {
        try {
            Resident resident = TownyAPI.getInstance().getResident(player);
            if (resident != null && resident.hasTown() && resident.getTown().hasNation()) {
                Nation nation = resident.getTown().getNation();
                return nation.getName();
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Erreur lors de la récupération de la nation pour " + player.getName());
        }
        return null;
    }
    
    private War findActiveWarForNations(String nation1, String nation2) {
        // Utiliser une approche optimisée pour trouver la guerre
        for (War war : plugin.getWarDataManager().getActiveWars().values()) {
            if (!war.isActive()) continue;
            
            if (areNationsInOpposingSides(war, nation1, nation2)) {
                return war;
            }
        }
        return null;
    }
    
    private boolean areNationsInOpposingSides(War war, String nation1, String nation2) {
        WarSide side1 = null;
        WarSide side2 = null;
        
        for (WarSide side : war.getSides()) {
            if (side.hasNation(nation1)) {
                side1 = side;
            }
            if (side.hasNation(nation2)) {
                side2 = side;
            }
            
            // Optimisation: arrêter dès qu'on a trouvé les deux
            if (side1 != null && side2 != null) {
                break;
            }
        }
        
        return side1 != null && side2 != null && !side1.equals(side2);
    }
    
    private WarSide getPlayerSide(War war, String nationName) {
        for (WarSide side : war.getSides()) {
            if (side.hasNation(nationName)) {
                return side;
            }
        }
        return null;
    }
    
    private void processKillReward(War war, WarSide killerSide, Player killer, Player victim, 
                                 String killerNation, String victimNation) {
        
        // Enregistrer le kill en base de données de manière asynchrone
        plugin.getWarDataManager().recordKill(
            war.getId(),
            killer.getUniqueId(),
            victim.getUniqueId(),
            killerNation,
            victimNation
        );
        
        // Ajouter les points selon la configuration
        int killPoints = plugin.getConfigManager().getKillPoints();
        int oldPoints = killerSide.getPoints();
        killerSide.addPoints(killPoints);
        int newPoints = killerSide.getPoints();
        
        // Mise à jour en base de données
        plugin.getWarDataManager().updateSidePoints(war.getId(), killerSide.getName(), newPoints);
        
        // Retour sur le thread principal pour les messages et vérifications
        Bukkit.getScheduler().runTask(plugin, () -> {
            handleKillNotifications(war, killerSide, killer, victim, killPoints, newPoints);
        });
    }
    
    private void handleKillNotifications(War war, WarSide killerSide, Player killer, Player victim, 
                                       int killPoints, int newPoints) {
        
        // Message de kill
        if (plugin.getConfig().getBoolean("settings.notifications.kill_broadcast", true)) {
            String message = MessageUtils.getMessage("war.kill_registered")
                    .replace("{killer}", killer.getName())
                    .replace("{victim}", victim.getName())
                    .replace("{points}", String.valueOf(killPoints))
                    .replace("{side}", killerSide.getDisplayName());
            
            Bukkit.broadcastMessage(message);
        }
        
        // Vérifier la victoire
        if (newPoints >= war.getRequiredPoints()) {
            handleWarVictory(war, killerSide);
        } else {
            // Message de progression
            if (plugin.getConfig().getBoolean("settings.notifications.war_updates", true)) {
                int remaining = war.getRequiredPoints() - newPoints;
                String progressMessage = killerSide.getDisplayName() + " §7: §f" + newPoints + 
                                       "§7/§f" + war.getRequiredPoints() + " pts §7(§f" + remaining + " §7restants)";
                
                Bukkit.broadcastMessage("§7[§6Guerre #" + war.getId() + "§7] " + progressMessage);
            }
        }
    }
    
    private void handleWarVictory(War war, WarSide winningSide) {
        war.setActive(false);
        
        String winMessage = MessageUtils.getMessage("war.ended")
                .replace("{winner}", winningSide.getDisplayName());
        
        Bukkit.broadcastMessage("§6§l=== VICTOIRE ! ===");
        Bukkit.broadcastMessage(winMessage);
        Bukkit.broadcastMessage("§6§l================");
        
        // Terminer la guerre de manière asynchrone
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            plugin.getWarDataManager().endWar(war.getId());
            plugin.getLogger().info("Guerre #" + war.getId() + " terminée - Vainqueur: " + winningSide.getName());
        });
    }
    
    /**
     * Nettoyage périodique du cache des kills
     */
    public void cleanupKillCache() {
        long now = System.currentTimeMillis();
        lastKillTime.entrySet().removeIf(entry -> (now - entry.getValue()) > (KILL_COOLDOWN * 10));
    }
}