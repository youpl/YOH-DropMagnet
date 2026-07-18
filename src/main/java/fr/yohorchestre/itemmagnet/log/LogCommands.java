package fr.yohorchestre.itemmagnet.log;

import fr.yohorchestre.itemmagnet.ItemMagnetPlugin;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;


import java.util.List;

public class LogCommands implements CommandExecutor {

    private final ItemMagnetPlugin plugin;

    public LogCommands(ItemMagnetPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Commande réservée aux joueurs.");
            return true;
        }

        Player player = (Player) sender;

        if (!player.hasPermission("itemmagnet.admin.logs")) {
            player.sendMessage(ChatColor.RED + "Vous n'avez pas la permission d'exécuter cette commande.");
            return true;
        }

        if (args.length < 1) {
            player.sendMessage(ChatColor.RED + "Utilisation: /magnetlog <ID>");
            return true;
        }

        String magnetId = args[0].replace("#", "");

        List<MagnetLogger.LogEntry> logs = plugin.getMagnetLogger().getLogs(magnetId);

        if (logs.isEmpty()) {
            player.sendMessage(ChatColor.YELLOW + "Aucun log trouvé pour l'aimant #" + magnetId);
            return true;
        }

        LogGUI gui = new LogGUI(magnetId, logs, 1);
        player.openInventory(gui.getInventory());

        return true;
    }
}
