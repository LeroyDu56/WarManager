// WarAdminCommand.java
package org.Novania.WarManager.commands;

import org.Novania.WarManager.WarManager;
import org.Novania.WarManager.gui.NationSelectionGUI;
import org.Novania.WarManager.gui.WarSelectionGUI;
import org.Novania.WarManager.models.CaptureZone;
import org.Novania.WarManager.models.War;
import org.Novania.WarManager.models.WarSide;
import org.Novania.WarManager.utils.MessageUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.object.Nation;

public class WarAdminCommand implements CommandExecutor {
    
    private final WarManager plugin;
    
    public WarAdminCommand(WarManager plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(MessageUtils.getMessage("commands.player_only"));
            return true;
        }
        
        Player player = (Player) sender;
        
        if (!player.hasPermission("warmanager.admin")) {
            player.sendMessage(MessageUtils.getMessage("commands.no_permission"));
            return true;
        }
        
        if (args.length == 0) {
            sendAdminHelpMessage(player);
            return true;
        }
        
        switch (args[0].toLowerCase()) {
            case "help":
                sendAdminHelpMessage(player);
                break;
                
            case "create":
                if (args.length < 3) {
                    player.sendMessage("§cUsage: /waradmin create <nom> <casus_belli>");
                    showAvailableCasusBelli(player);
                    return true;
                }
                createWar(player, args[1], args[2]);
                break;
                
            case "manage":
                if (args.length < 2) {
                    new WarSelectionGUI(plugin, true).openGUI(player);
                } else {
                    try {
                        int warId = Integer.parseInt(args[1]);
                        War war = plugin.getWarDataManager().getWar(warId);
                        if (war != null) {
                            new NationSelectionGUI(plugin, war).openGUI(player);
                        } else {
                            player.sendMessage(MessageUtils.getMessage("errors.war_not_found").replace("{id}", args[1]));
                        }
                    } catch (NumberFormatException e) {
                        player.sendMessage("§cID de guerre invalide.");
                    }
                }
                break;
                
            case "list":
                listWars(player);
                break;
                
            case "info":
                if (args.length < 2) {
                    player.sendMessage("§cUsage: /waradmin info <id>");
                    return true;
                }
                showWarInfo(player, args[1]);
                break;
                
            case "end":
                if (args.length < 2) {
                    player.sendMessage("§cUsage: /waradmin end <id>");
                    return true;
                }
                endWar(player, args[1]);
                break;
                
            case "pause":
                if (args.length < 2) {
                    player.sendMessage("§cUsage: /waradmin pause <id>");
                    return true;
                }
                pauseWar(player, args[1]);
                break;
                
            case "resume":
                if (args.length < 2) {
                    player.sendMessage("§cUsage: /waradmin resume <id>");
                    return true;
                }
                resumeWar(player, args[1]);
                break;
                
            case "delete":
                if (args.length < 2) {
                    player.sendMessage("§cUsage: /waradmin delete <id>");
                    return true;
                }
                deleteWar(player, args[1]);
                break;
                
            case "addside":
                if (args.length < 4) {
                    player.sendMessage("§cUsage: /waradmin addside <warId> <nom> <couleur>");
                    showAvailableColors(player);
                    return true;
                }
                addSideToWar(player, args[1], args[2], args[3]);
                break;
                
            case "removeside":
                if (args.length < 3) {
                    player.sendMessage("§cUsage: /waradmin removeside <warId> <nom_camp>");
                    return true;
                }
                removeSideFromWar(player, args[1], args[2]);
                break;
                
            case "addnation":
                if (args.length < 4) {
                    player.sendMessage("§cUsage: /waradmin addnation <warId> <camp> <nation>");
                    return true;
                }
                addNationToSide(player, args[1], args[2], args[3]);
                break;
                
            case "removenation":
                if (args.length < 4) {
                    player.sendMessage("§cUsage: /waradmin removenation <warId> <camp> <nation>");
                    return true;
                }
                removeNationFromSide(player, args[1], args[2], args[3]);
                break;
                
            case "setpoints":
                if (args.length < 4) {
                    player.sendMessage("§cUsage: /waradmin setpoints <warId> <camp> <points>");
                    return true;
                }
                setPoints(player, args[1], args[2], args[3]);
                break;
                
            case "addpoints":
                if (args.length < 4) {
                    player.sendMessage("§cUsage: /waradmin addpoints <warId> <camp> <points>");
                    return true;
                }
                addPoints(player, args[1], args[2], args[3]);
                break;
                
            case "reload":
                reloadPlugin(player);
                break;
                
            case "debug":
                debugInfo(player);
                break;
                
            case "forcewin":
                if (args.length < 3) {
                    player.sendMessage("§cUsage: /waradmin forcewin <warId> <camp>");
                    return true;
                }
                forceWin(player, args[1], args[2]);
                break;
                
            // NOUVELLES COMMANDES POUR LES ZONES DE CAPTURE
            case "createzone":
                if (args.length < 3) {
                    player.sendMessage("§cUsage: /waradmin createzone <warId> <nom>");
                    return true;
                }
                createCaptureZone(player, args[1], args[2]);
                break;
                
            case "listzones":
                if (args.length < 2) {
                    player.sendMessage("§cUsage: /waradmin listzones <warId>");
                    return true;
                }
                listCaptureZones(player, args[1]);
                break;
                
            case "zoneinfo":
                if (args.length < 2) {
                    player.sendMessage("§cUsage: /waradmin zoneinfo <zoneId>");
                    return true;
                }
                showZoneInfo(player, args[1]);
                break;
                
            case "deletezone":
                if (args.length < 2) {
                    player.sendMessage("§cUsage: /waradmin deletezone <zoneId>");
                    return true;
                }
                deleteCaptureZone(player, args[1]);
                break;
                
            default:
                player.sendMessage(MessageUtils.getMessage("commands.invalid_usage"));
                sendAdminHelpMessage(player);
                break;
        }
        
        return true;
    }
    
    private void createWar(Player player, String name, String casusBeliType) {
        int requiredPoints = plugin.getConfig().getInt("settings.victory_points." + casusBeliType, -1);
        if (requiredPoints == -1) {
            player.sendMessage("§cCasus belli invalide !");
            showAvailableCasusBelli(player);
            return;
        }
        
        War war = plugin.getWarDataManager().createWar(name, casusBeliType, requiredPoints, player.getUniqueId());
        if (war != null) {
            player.sendMessage(MessageUtils.getMessage("war.created").replace("{id}", String.valueOf(war.getId())));
            plugin.getLogger().info("Guerre créée par " + player.getName() + " : " + name + " (ID: " + war.getId() + ")");
        } else {
            player.sendMessage("§cErreur lors de la création de la guerre.");
        }
    }
    
    private void listWars(Player player) {
        var wars = plugin.getWarDataManager().getActiveWars();
        if (wars.isEmpty()) {
            player.sendMessage("§eAucune guerre active.");
            return;
        }
        
        player.sendMessage("§8§m----§r §c§lGuerres Actives §8§m----§r");
        for (War war : wars.values()) {
            String status = war.isActive() ? "§aActive" : "§cTerminée";
            player.sendMessage("§e#" + war.getId() + " §7- §f" + war.getName() + " " + status);
            player.sendMessage("  §7Type: §f" + war.getCasusBeliType() + " §7| Camps: §f" + war.getSides().size());
        }
        player.sendMessage("§8§m------------------------§r");
    }
    
    private void showWarInfo(Player player, String warIdStr) {
        try {
            int warId = Integer.parseInt(warIdStr);
            War war = plugin.getWarDataManager().getWar(warId);
            if (war == null) {
                player.sendMessage(MessageUtils.getMessage("errors.war_not_found").replace("{id}", warIdStr));
                return;
            }
            
            player.sendMessage("§8§m----§r §c§lInfo Guerre #" + warId + " §8§m----§r");
            player.sendMessage("§eNom: §f" + war.getName());
            player.sendMessage("§eCasus Belli: §f" + war.getCasusBeliType());
            player.sendMessage("§ePoints requis: §f" + war.getRequiredPoints());
            player.sendMessage("§eStatut: " + (war.isActive() ? "§aActive" : "§cTerminée"));
            player.sendMessage("§eDate de début: §f" + war.getStartDate().toLocalDate());
            
            if (!war.getSides().isEmpty()) {
                player.sendMessage("§eCamps:");
                for (WarSide side : war.getSides()) {
                    player.sendMessage("  " + side.getDisplayName() + " §7- §f" + side.getPoints() + "/" + war.getRequiredPoints() + " pts (§f" + side.getKills() + " kills§7)");
                    if (!side.getNations().isEmpty()) {
                        player.sendMessage("    §7Nations: §f" + String.join(", ", side.getNations()));
                    }
                }
            }
            
            WarSide winner = war.getWinner();
            if (winner != null) {
                player.sendMessage("§aVainqueur: " + winner.getDisplayName());
            }
            
            player.sendMessage("§8§m------------------------§r");
            
        } catch (NumberFormatException e) {
            player.sendMessage("§cID de guerre invalide.");
        }
    }
    
    private void endWar(Player player, String warIdStr) {
        try {
            int warId = Integer.parseInt(warIdStr);
            War war = plugin.getWarDataManager().getWar(warId);
            if (war != null) {
                plugin.getWarDataManager().endWar(warId);
                player.sendMessage("§aGuerre #" + warId + " terminée par " + player.getName());
                
                // Annoncer à tous les joueurs
                Bukkit.broadcastMessage(MessageUtils.getMessage("war.ended").replace("{winner}", "Administrateur"));
                plugin.getLogger().info("Guerre #" + warId + " terminée par " + player.getName());
            } else {
                player.sendMessage(MessageUtils.getMessage("errors.war_not_found").replace("{id}", warIdStr));
            }
        } catch (NumberFormatException e) {
            player.sendMessage("§cID de guerre invalide.");
        }
    }
    
    private void pauseWar(Player player, String warIdStr) {
        try {
            int warId = Integer.parseInt(warIdStr);
            War war = plugin.getWarDataManager().getWar(warId);
            if (war != null) {
                if (war.isActive()) {
                    war.setActive(false);
                    player.sendMessage("§aGuerre #" + warId + " mise en pause.");
                    plugin.getLogger().info("Guerre #" + warId + " mise en pause par " + player.getName());
                } else {
                    player.sendMessage("§cCette guerre n'est pas active.");
                }
            } else {
                player.sendMessage(MessageUtils.getMessage("errors.war_not_found").replace("{id}", warIdStr));
            }
        } catch (NumberFormatException e) {
            player.sendMessage("§cID de guerre invalide.");
        }
    }
    
    private void resumeWar(Player player, String warIdStr) {
        try {
            int warId = Integer.parseInt(warIdStr);
            War war = plugin.getWarDataManager().getWar(warId);
            if (war != null) {
                if (!war.isActive()) {
                    war.setActive(true);
                    player.sendMessage("§aGuerre #" + warId + " reprise.");
                    plugin.getLogger().info("Guerre #" + warId + " reprise par " + player.getName());
                } else {
                    player.sendMessage("§cCette guerre est déjà active.");
                }
            } else {
                player.sendMessage(MessageUtils.getMessage("errors.war_not_found").replace("{id}", warIdStr));
            }
        } catch (NumberFormatException e) {
            player.sendMessage("§cID de guerre invalide.");
        }
    }
    
    private void deleteWar(Player player, String warIdStr) {
        try {
            int warId = Integer.parseInt(warIdStr);
            War war = plugin.getWarDataManager().getWar(warId);
            if (war != null) {
                plugin.getWarDataManager().deleteWar(warId);
                player.sendMessage("§aGuerre #" + warId + " supprimée définitivement.");
                plugin.getLogger().info("Guerre #" + warId + " supprimée par " + player.getName());
            } else {
                player.sendMessage(MessageUtils.getMessage("errors.war_not_found").replace("{id}", warIdStr));
            }
        } catch (NumberFormatException e) {
            player.sendMessage("§cID de guerre invalide.");
        }
    }
    
    private void addSideToWar(Player player, String warIdStr, String sideName, String colorCode) {
        try {
            int warId = Integer.parseInt(warIdStr);
            War war = plugin.getWarDataManager().getWar(warId);
            if (war != null) {
                String color = getColorFromCode(colorCode);
                plugin.getWarDataManager().addSideToWar(warId, sideName, color);
                player.sendMessage("§aCamp '" + color + sideName + "§a' ajouté à la guerre #" + warId);
            } else {
                player.sendMessage(MessageUtils.getMessage("errors.war_not_found").replace("{id}", warIdStr));
            }
        } catch (NumberFormatException e) {
            player.sendMessage("§cID de guerre invalide.");
        }
    }
    
    private void removeSideFromWar(Player player, String warIdStr, String sideName) {
        try {
            int warId = Integer.parseInt(warIdStr);
            War war = plugin.getWarDataManager().getWar(warId);
            if (war != null) {
                plugin.getWarDataManager().removeSideFromWar(warId, sideName);
                player.sendMessage("§aCamp '" + sideName + "' retiré de la guerre #" + warId);
            } else {
                player.sendMessage(MessageUtils.getMessage("errors.war_not_found").replace("{id}", warIdStr));
            }
        } catch (NumberFormatException e) {
            player.sendMessage("§cID de guerre invalide.");
        }
    }
    
    private void addNationToSide(Player player, String warIdStr, String sideName, String nationName) {
        try {
            int warId = Integer.parseInt(warIdStr);
            War war = plugin.getWarDataManager().getWar(warId);
            if (war == null) {
                player.sendMessage(MessageUtils.getMessage("errors.war_not_found").replace("{id}", warIdStr));
                return;
            }
            
            Nation nation = TownyAPI.getInstance().getNation(nationName);
            if (nation == null) {
                player.sendMessage(MessageUtils.getMessage("errors.nation_not_found").replace("{nation}", nationName));
                return;
            }
            
            plugin.getWarDataManager().addNationToSide(warId, sideName, nationName);
            player.sendMessage("§aNation '" + nationName + "' ajoutée au camp '" + sideName + "'");
            
        } catch (NumberFormatException e) {
            player.sendMessage("§cID de guerre invalide.");
        }
    }
    
    private void removeNationFromSide(Player player, String warIdStr, String sideName, String nationName) {
        try {
            int warId = Integer.parseInt(warIdStr);
            War war = plugin.getWarDataManager().getWar(warId);
            if (war != null) {
                plugin.getWarDataManager().removeNationFromSide(warId, sideName, nationName);
                player.sendMessage("§aNation '" + nationName + "' retirée du camp '" + sideName + "'");
            } else {
                player.sendMessage(MessageUtils.getMessage("errors.war_not_found").replace("{id}", warIdStr));
            }
        } catch (NumberFormatException e) {
            player.sendMessage("§cID de guerre invalide.");
        }
    }
    
    private void setPoints(Player player, String warIdStr, String sideName, String pointsStr) {
        try {
            int warId = Integer.parseInt(warIdStr);
            int points = Integer.parseInt(pointsStr);
            
            War war = plugin.getWarDataManager().getWar(warId);
            if (war != null) {
                WarSide side = war.getSideByName(sideName);
                if (side != null) {
                    side.setPoints(points);
                    plugin.getWarDataManager().updateSidePoints(warId, sideName, points);
                    player.sendMessage("§aPoints du camp '" + sideName + "' mis à " + points);
                } else {
                    player.sendMessage("§cCamp introuvable : " + sideName);
                }
            } else {
                player.sendMessage(MessageUtils.getMessage("errors.war_not_found").replace("{id}", warIdStr));
            }
        } catch (NumberFormatException e) {
            player.sendMessage("§cValeurs invalides.");
        }
    }
    
    private void addPoints(Player player, String warIdStr, String sideName, String pointsStr) {
        try {
            int warId = Integer.parseInt(warIdStr);
            int pointsToAdd = Integer.parseInt(pointsStr);
            
            War war = plugin.getWarDataManager().getWar(warId);
            if (war != null) {
                WarSide side = war.getSideByName(sideName);
                if (side != null) {
                    side.addPoints(pointsToAdd);
                    plugin.getWarDataManager().updateSidePoints(warId, sideName, side.getPoints());
                    player.sendMessage("§a+" + pointsToAdd + " points ajoutés au camp '" + sideName + "' (Total: " + side.getPoints() + ")");
                } else {
                    player.sendMessage("§cCamp introuvable : " + sideName);
                }
            } else {
                player.sendMessage(MessageUtils.getMessage("errors.war_not_found").replace("{id}", warIdStr));
            }
        } catch (NumberFormatException e) {
            player.sendMessage("§cValeurs invalides.");
        }
    }
    
    private void forceWin(Player player, String warIdStr, String sideName) {
        try {
            int warId = Integer.parseInt(warIdStr);
            War war = plugin.getWarDataManager().getWar(warId);
            if (war != null) {
                WarSide side = war.getSideByName(sideName);
                if (side != null) {
                    side.setPoints(war.getRequiredPoints());
                    plugin.getWarDataManager().updateSidePoints(warId, sideName, war.getRequiredPoints());
                    plugin.getWarDataManager().endWar(warId);
                    
                    String winMessage = "§a§l" + side.getDisplayName() + " §a§la remporté la guerre par décision administrative !";
                    Bukkit.broadcastMessage(winMessage);
                    plugin.getLogger().info("Victoire forcée pour " + sideName + " dans la guerre #" + warId + " par " + player.getName());
                } else {
                    player.sendMessage("§cCamp introuvable : " + sideName);
                }
            } else {
                player.sendMessage(MessageUtils.getMessage("errors.war_not_found").replace("{id}", warIdStr));
            }
        } catch (NumberFormatException e) {
            player.sendMessage("§cValeurs invalides.");
        }
    }
    
    private void reloadPlugin(Player player) {
        try {
            plugin.getConfigManager().reloadConfig();
            plugin.getWarDataManager().reloadActiveWars();
            
            player.sendMessage("§a✅ WarManager rechargé avec succès !");
            plugin.getLogger().info("Plugin rechargé par " + player.getName());
            
        } catch (Exception e) {
            player.sendMessage("§c❌ Erreur lors du rechargement : " + e.getMessage());
            plugin.getLogger().severe("Erreur lors du rechargement : " + e.getMessage());
        }
    }
    
    private void debugInfo(Player player) {
        var wars = plugin.getWarDataManager().getActiveWars();
        var zones = plugin.getCaptureZoneManager().getActiveZones();
        
        player.sendMessage("§8§m----§r §c§lDébug WarManager §8§m----§r");
        player.sendMessage("§eGuerres en mémoire: §f" + wars.size());
        player.sendMessage("§eZones de capture actives: §f" + zones.size());
        player.sendMessage("§eBase de données: §f" + (plugin.getWarDataManager().isDatabaseConnected() ? "§aConnectée" : "§cDéconnectée"));
        player.sendMessage("§ePlugin actif: §f" + (plugin.isEnabled() ? "§aOui" : "§cNon"));
        
        if (!wars.isEmpty()) {
            player.sendMessage("§eGuerres actives:");
            wars.values().forEach(war -> {
                player.sendMessage("  §7#" + war.getId() + " - " + war.getName() + " (" + war.getSides().size() + " camps)");
            });
        }
        
        if (!zones.isEmpty()) {
            player.sendMessage("§eZones actives:");
            zones.values().forEach(zone -> {
                String controller = zone.getCurrentController() != null ? zone.getCurrentController() : "Aucun";
                player.sendMessage("  §7#" + zone.getId() + " - " + zone.getName() + " (Contrôlée par: " + controller + ")");
            });
        }
        player.sendMessage("§8§m------------------------§r");
    }
    
    // NOUVELLES MÉTHODES POUR LES ZONES DE CAPTURE
    
    private void createCaptureZone(Player player, String warIdStr, String zoneName) {
        try {
            int warId = Integer.parseInt(warIdStr);
            War war = plugin.getWarDataManager().getWar(warId);
            if (war == null) {
                player.sendMessage("§cGuerre introuvable avec l'ID " + warId);
                return;
            }
            
            if (!war.isActive()) {
                player.sendMessage("§cLa guerre doit être active pour créer une zone de capture");
                return;
            }
            
            Location playerLocation = player.getLocation();
            CaptureZone zone = plugin.getCaptureZoneManager().createCaptureZone(
                warId, zoneName, playerLocation, player.getUniqueId()
            );
            
            if (zone != null) {
                player.sendMessage("§a✓ Zone de capture créée: " + zoneName + " (ID: " + zone.getId() + ")");
                player.sendMessage("§7Position: " + playerLocation.getBlockX() + ", " + 
                                 playerLocation.getBlockY() + ", " + playerLocation.getBlockZ());
                player.sendMessage("§7Taille: 3x3 chunks (rayon de 1 chunk)");
                player.sendMessage("§7Durée: 3 heures");
                player.sendMessage("§7Récompense: 20 points pour le camp vainqueur");
                
                plugin.getLogger().info("Zone de capture créée par " + player.getName() + 
                                      ": " + zoneName + " pour la guerre #" + warId);
            } else {
                player.sendMessage("§cErreur lors de la création de la zone");
            }
            
        } catch (NumberFormatException e) {
            player.sendMessage("§cID de guerre invalide");
        }
    }
    
    private void listCaptureZones(Player player, String warIdStr) {
        try {
            int warId = Integer.parseInt(warIdStr);
            War war = plugin.getWarDataManager().getWar(warId);
            if (war == null) {
                player.sendMessage("§cGuerre introuvable avec l'ID " + warId);
                return;
            }
            
            var zones = plugin.getCaptureZoneManager().getActiveZones();
            var warZones = zones.values().stream()
                               .filter(zone -> zone.getWarId() == warId)
                               .toList();
            
            if (warZones.isEmpty()) {
                player.sendMessage("§eAucune zone de capture pour la guerre #" + warId);
                return;
            }
            
            player.sendMessage("§8§m----§r §6Zones de Capture - Guerre #" + warId + " §8§m----§r");
            for (CaptureZone zone : warZones) {
                String status = zone.isActive() ? "§aActive" : "§cTerminée";
                String controller = zone.getCurrentController() != null ? zone.getCurrentController() : "§7Aucun";
                long remainingMinutes = zone.getRemainingMinutes();
                
                player.sendMessage("§e#" + zone.getId() + " §7- §f" + zone.getName() + " " + status);
                player.sendMessage("  §7Contrôlée par: §f" + controller);
                player.sendMessage("  §7Temps restant: §f" + remainingMinutes + " minutes");
            }
            player.sendMessage("§8§m--------------------------------§r");
            
        } catch (NumberFormatException e) {
            player.sendMessage("§cID de guerre invalide");
        }
    }
    
    private void showZoneInfo(Player player, String zoneIdStr) {
        try {
            int zoneId = Integer.parseInt(zoneIdStr);
            CaptureZone zone = plugin.getCaptureZoneManager().getCaptureZone(zoneId);
            if (zone == null) {
                player.sendMessage("§cZone introuvable avec l'ID " + zoneId);
                return;
            }
            
            War war = plugin.getWarDataManager().getWar(zone.getWarId());
            
            player.sendMessage("§8§m----§r §6Zone #" + zoneId + ": " + zone.getName() + " §8§m----§r");
            player.sendMessage("§7Guerre: §f#" + zone.getWarId() + (war != null ? " - " + war.getName() : ""));
            player.sendMessage("§7Position: §f" + zone.getCenterLocation().getBlockX() + ", " + 
                             zone.getCenterLocation().getBlockY() + ", " + zone.getCenterLocation().getBlockZ());
            player.sendMessage("§7Monde: §f" + zone.getCenterLocation().getWorld().getName());
            player.sendMessage("§7Statut: " + (zone.isActive() ? "§aActive" : "§cTerminée"));
            player.sendMessage("§7Temps restant: §f" + zone.getRemainingMinutes() + " minutes");
            
            if (zone.getCurrentController() != null) {
                player.sendMessage("§7Contrôlée par: §f" + zone.getCurrentController());
                if (zone.getCaptureStartTime() != null) {
                    long currentControlMinutes = java.time.Duration.between(
                        zone.getCaptureStartTime(), java.time.LocalDateTime.now()
                    ).toMinutes();
                    player.sendMessage("§7Temps de contrôle actuel: §f" + currentControlMinutes + " minutes");
                }
            } else {
                player.sendMessage("§7Contrôlée par: §cAucun camp");
            }
            
            player.sendMessage("§7Statistiques de contrôle total:");
            if (war != null) {
                for (WarSide side : war.getSides()) {
                    long controlTime = zone.getTotalControlTime(side.getName());
                    if (controlTime > 0) {
                        player.sendMessage("  " + side.getDisplayName() + "§7: §f" + zone.formatControlTime(controlTime));
                    }
                }
            }
            
            player.sendMessage("§8§m--------------------------------§r");
            
        } catch (NumberFormatException e) {
            player.sendMessage("§cID de zone invalide");
        }
    }
    
    private void deleteCaptureZone(Player player, String zoneIdStr) {
        try {
            int zoneId = Integer.parseInt(zoneIdStr);
            CaptureZone zone = plugin.getCaptureZoneManager().getCaptureZone(zoneId);
            if (zone == null) {
                player.sendMessage("§cZone introuvable avec l'ID " + zoneId);
                return;
            }
            
            plugin.getCaptureZoneManager().deleteZone(zoneId);
            player.sendMessage("§a✓ Zone #" + zoneId + " (" + zone.getName() + ") supprimée");
            plugin.getLogger().info("Zone #" + zoneId + " supprimée par " + player.getName());
            
        } catch (NumberFormatException e) {
            player.sendMessage("§cID de zone invalide");
        }
    }
    
    private String getColorFromCode(String code) {
        switch (code.toLowerCase()) {
            case "r": case "red": return "§c";
            case "b": case "blue": return "§9";
            case "g": case "green": return "§a";
            case "y": case "yellow": return "§e";
            case "o": case "orange": return "§6";
            case "p": case "purple": return "§5";
            case "c": case "cyan": return "§b";
            case "w": case "white": return "§f";
            case "k": case "black": return "§0";
            case "gray": case "grey": return "§7";
            default: return "§f";
        }
    }
    
    private void showAvailableCasusBelli(Player player) {
        player.sendMessage("§eTypes de casus belli disponibles:");
        if (plugin.getConfig().getConfigurationSection("settings.victory_points") != null) {
            plugin.getConfig().getConfigurationSection("settings.victory_points").getKeys(false)
                    .forEach(key -> {
                        int points = plugin.getConfig().getInt("settings.victory_points." + key);
                        player.sendMessage("§7- §f" + key + " §7(" + points + " points)");
                    });
        }
    }
    
    private void showAvailableColors(Player player) {
        player.sendMessage("§eCouleurs disponibles:");
        player.sendMessage("§7- §cr §7(rouge), §9b §7(bleu), §ag §7(vert), §ey §7(jaune)");
        player.sendMessage("§7- §6o §7(orange), §5p §7(violet), §bc §7(cyan), §fw §7(blanc)");
        player.sendMessage("§7- §0k §7(noir), §7gray §7(gris)");
    }
    
    private void sendAdminHelpMessage(Player player) {
        player.sendMessage("§8§m----§r §c§lWarManager Admin §8§m----§r");
        player.sendMessage("§6§lGestion des guerres:");
        player.sendMessage("§e/waradmin create <nom> <casus_belli> §8- §7Créer une guerre");
        player.sendMessage("§e/waradmin list §8- §7Lister toutes les guerres");
        player.sendMessage("§e/waradmin info <id> §8- §7Infos détaillées d'une guerre");
        player.sendMessage("§e/waradmin manage [id] §8- §7Interface de gestion");
        player.sendMessage("§e/waradmin end <id> §8- §7Terminer une guerre");
        player.sendMessage("§e/waradmin pause <id> §8- §7Mettre en pause");
        player.sendMessage("§e/waradmin resume <id> §8- §7Reprendre une guerre");
        player.sendMessage("§e/waradmin delete <id> §8- §7Supprimer définitivement");
        player.sendMessage("");
        player.sendMessage("§6§lGestion des camps:");
        player.sendMessage("§e/waradmin addside <id> <nom> <couleur> §8- §7Ajouter un camp");
        player.sendMessage("§e/waradmin removeside <id> <camp> §8- §7Supprimer un camp");
        player.sendMessage("§e/waradmin addnation <id> <camp> <nation> §8- §7Ajouter une nation");
        player.sendMessage("§e/waradmin removenation <id> <camp> <nation> §8- §7Retirer une nation");
        player.sendMessage("");
        player.sendMessage("§6§lGestion des points:");
        player.sendMessage("§e/waradmin setpoints <id> <camp> <points> §8- §7Définir les points");
        player.sendMessage("§e/waradmin addpoints <id> <camp> <points> §8- §7Ajouter des points");
        player.sendMessage("§e/waradmin forcewin <id> <camp> §8- §7Forcer la victoire");
        player.sendMessage("");
        player.sendMessage("§6§lZones de Capture:");
        player.sendMessage("§e/waradmin createzone <warId> <nom> §8- §7Créer une zone (3x3 chunks)");
        player.sendMessage("§e/waradmin listzones <warId> §8- §7Lister les zones d'une guerre");
        player.sendMessage("§e/waradmin zoneinfo <zoneId> §8- §7Infos détaillées d'une zone");
        player.sendMessage("§e/waradmin deletezone <zoneId> §8- §7Supprimer une zone");
        player.sendMessage("");
        player.sendMessage("§6§lUtilitaires:");
        player.sendMessage("§e/waradmin reload §8- §7Recharger le plugin");
        player.sendMessage("§e/waradmin debug §8- §7Informations de debug");
        player.sendMessage("§e/waradmin help §8- §7Afficher cette aide");
        player.sendMessage("§8§m--------------------------------§r");
    }
}