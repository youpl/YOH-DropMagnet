package fr.yohorchestre.itemmagnet.command;

import fr.yohorchestre.itemmagnet.ItemMagnetPlugin;
import org.bukkit.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class MagnetCommand implements CommandExecutor {

    private final ItemMagnetPlugin plugin;

    public MagnetCommand(ItemMagnetPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("itemmagnet.admin")) {
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("messages.no-permission", "§cVous n'avez pas la permission.")));
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage("§cUtilisation: /magnet <give|reload> [joueur]");
            return true;
        }

        if (args[0].equalsIgnoreCase("reload")) {
            plugin.reloadConfig();
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("messages.prefix", "") + "§aConfiguration rechargée."));
            return true;
        }

        if (args[0].equalsIgnoreCase("give")) {
            Player target = null;
            if (args.length > 1) {
                target = Bukkit.getPlayer(args[1]);
            } else if (sender instanceof Player) {
                target = (Player) sender;
            }

            if (target == null) {
                sender.sendMessage(ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("messages.player-not-found", "§cJoueur introuvable.")));
                return true;
            }

            ItemStack magnet = plugin.getMagnetManager().createMagnet();
            target.getInventory().addItem(magnet);
            
            String msg = plugin.getConfig().getString("messages.magnet-given", "§aAimant donné à %player%.");
            msg = msg.replace("%player%", target.getName());
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("messages.prefix", "") + msg));
            return true;
        }

        return false;
    }
}
