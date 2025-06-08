// WarCommand.java
package org.Novania.WarManager.commands;

import org.Novania.WarManager.WarManager;
import org.Novania.WarManager.gui.WarSelectionGUI;
import org.Novania.WarManager.gui.WarStatsGUI;
import org.Novania.WarManager.models.War;
import org.Novania.WarManager.utils.MessageUtils;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class WarCommand implements CommandExecutor {
    
    private final WarManager plugin;
    
    public WarCommand(WarManager plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(MessageUtils.getMessage("commands.player_only"));
            return true;
        }
        
        Player player = (Player) sender;
        
        if (!player.hasPermission("warmanager.use")) {
            player.sendMessage(MessageUtils.getMessage("commands.no_permission"));
            return true;
        }
        
        if (args.length == 0) {
            sendHelpMessage(player);
            return true;
        }
        
        switch (args[0].toLowerCase()) {
            case "help":
                sendHelpMessage(player);
                break;
                
            case "list":
                // CORRECTION: S'assurer d'utiliser le mode joueur (false)
                plugin.getLogger().info("Commande /war list par " + player.getName() + " - Mode joueur");
                new WarSelectionGUI(plugin, false).openGUI(player);
                break;
                
            case "stats":
                if (args.length < 2) {
                    player.sendMessage(MessageUtils.getMessage("commands.invalid_usage"));
                    return true;
                }
                try {
                    int warId = Integer.parseInt(args[1]);
                    War war = plugin.getWarDataManager().getWar(warId);
                    if (war != null) {
                        new WarStatsGUI(plugin, war).openGUI(player);
                    } else {
                        player.sendMessage(MessageUtils.getMessage("errors.war_not_found").replace("{id}", String.valueOf(warId)));
                    }
                } catch (NumberFormatException e) {
                    player.sendMessage(MessageUtils.getMessage("commands.invalid_usage"));
                }
                break;
                
            default:
                player.sendMessage(MessageUtils.getMessage("commands.invalid_usage"));
                break;
        }
        
        return true;
    }
    
    private void sendHelpMessage(Player player) {
        player.sendMessage("§8§m----§r §c§lWarManager §8§m----§r");
        player.sendMessage("§e/war list §8- §7Voir les guerres actives");
        player.sendMessage("§e/war stats <id> §8- §7Statistiques d'une guerre");
        player.sendMessage("§e/war help §8- §7Afficher cette aide");
        player.sendMessage("§8§m------------------------§r");
    }
}