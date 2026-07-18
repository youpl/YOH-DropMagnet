package fr.yohorchestre.itemmagnet.log;

import fr.yohorchestre.itemmagnet.ItemMagnetPlugin;
import fr.yohorchestre.itemmagnet.utils.Base64Utils;
import org.bukkit.Location;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

public class MagnetLogger {

    private final ItemMagnetPlugin plugin;
    private final File logsDir;
    // max logs per magnet to prevent huge files
    private final int maxLogs = 500;

    public MagnetLogger(ItemMagnetPlugin plugin) {
        this.plugin = plugin;
        this.logsDir = new File(plugin.getDataFolder(), "logs");
        if (!logsDir.exists()) {
            logsDir.mkdirs();
        }
    }

    public void logSuckedItem(String magnetId, Location loc, Player player, ItemStack item) {
        if (magnetId == null) return;
        String source = player != null ? "Joueur: " + player.getName() : "Posé: " + loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ();
        writeLogAsync(magnetId, "ASPIRÉ", source, item);
    }

    public void logManualAction(String magnetId, Player player, ItemStack item, boolean inserted) {
        if (magnetId == null) return;
        String action = inserted ? "INSÉRÉ" : "RETIRÉ";
        String source = "Joueur: " + player.getName();
        writeLogAsync(magnetId, action, source, item);
    }

    private void writeLogAsync(String magnetId, String action, String source, ItemStack item) {
        if (item == null || item.getType().isAir()) return;
        ItemStack clone = item.clone();
        
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            synchronized (magnetId.intern()) {
                File logFile = new File(logsDir, magnetId + ".yml");
                YamlConfiguration config = YamlConfiguration.loadConfiguration(logFile);
                
                List<Map<String, Object>> logs = new ArrayList<>();
                if (config.contains("logs")) {
                    for (Map<?, ?> map : config.getMapList("logs")) {
                        logs.add((Map<String, Object>) map);
                    }
                }
                
                Map<String, Object> entry = new HashMap<>();
                entry.put("action", action);
                entry.put("time", System.currentTimeMillis());
                entry.put("source", source);
                entry.put("item_base64", Base64Utils.itemStackArrayToBase64(new ItemStack[]{clone}));
                
                logs.add(0, entry); // Add to the top (newest first)
                
                // Limit size
                if (logs.size() > maxLogs) {
                    logs = logs.subList(0, maxLogs);
                }
                
                config.set("logs", logs);
                try {
                    config.save(logFile);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public List<LogEntry> getLogs(String magnetId) {
        File logFile = new File(logsDir, magnetId + ".yml");
        List<LogEntry> result = new ArrayList<>();
        if (!logFile.exists()) return result;

        YamlConfiguration config = YamlConfiguration.loadConfiguration(logFile);
        if (config.contains("logs")) {
            List<Map<?, ?>> logs = config.getMapList("logs");
            for (Map<?, ?> map : logs) {
                LogEntry entry = new LogEntry();
                entry.action = (String) map.get("action");
                entry.time = (long) map.get("time");
                entry.source = (String) map.get("source");
                ItemStack[] arr = Base64Utils.itemStackArrayFromBase64((String) map.get("item_base64"));
                if (arr.length > 0) {
                    entry.item = arr[0];
                }
                result.add(entry);
            }
        }
        return result;
    }

    public static class LogEntry {
        public String action;
        public long time;
        public String source;
        public ItemStack item;
    }
}
