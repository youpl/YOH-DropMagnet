package fr.yohorchestre.itemmagnet.log;

import fr.yohorchestre.itemmagnet.ItemMagnetPlugin;
import org.bukkit.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class LogGUI implements InventoryHolder {

    private final Inventory inventory;
    private final String magnetId;
    private final List<MagnetLogger.LogEntry> logs;
    private final int page;
    private final int maxPages;

    public LogGUI(String magnetId, List<MagnetLogger.LogEntry> logs, int page) {
        this.magnetId = magnetId;
        this.logs = logs;
        this.page = page;
        this.maxPages = Math.max(1, (int) Math.ceil(logs.size() / 45.0));
        
        this.inventory = Bukkit.createInventory(this, 54, ChatColor.DARK_GRAY + "Logs: #" + magnetId + " (P." + page + "/" + maxPages + ")");
        
        initItems();
    }

    private void initItems() {
        int startIndex = (page - 1) * 45;
        int endIndex = Math.min(startIndex + 45, logs.size());
        
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");

        for (int i = startIndex; i < endIndex; i++) {
            MagnetLogger.LogEntry log = logs.get(i);
            ItemStack display = log.item.clone();
            ItemMeta meta = display.getItemMeta();
            if (meta != null) {
                List<String> lore = meta.hasLore() ? meta.getLore() : new ArrayList<>();
                lore.add("");
                lore.add(ChatColor.YELLOW + "--- LOG INFO ---");
                
                ChatColor actionColor = ChatColor.GRAY;
                if ("ASPIRÉ".equals(log.action)) actionColor = ChatColor.AQUA;
                if ("INSÉRÉ".equals(log.action)) actionColor = ChatColor.GREEN;
                if ("RETIRÉ".equals(log.action)) actionColor = ChatColor.RED;
                
                lore.add(ChatColor.GRAY + "Action: " + actionColor + log.action);
                lore.add(ChatColor.GRAY + "Date: " + ChatColor.WHITE + sdf.format(new Date(log.time)));
                lore.add(ChatColor.GRAY + "Source: " + ChatColor.WHITE + log.source);
                lore.add(ChatColor.GRAY + "Quantité: " + ChatColor.WHITE + log.item.getAmount());
                lore.add("");
                lore.add(ChatColor.GOLD + "Clic pour restituer l'item");
                
                meta.setLore(lore);
                display.setItemMeta(meta);
            }
            inventory.setItem(i - startIndex, display);
        }

        // Pagination row (45-53)
        if (page > 1) {
            ItemStack prev = new ItemStack(Material.ARROW);
            ItemMeta pMeta = prev.getItemMeta();
            pMeta.setDisplayName(ChatColor.YELLOW + "Page Précédente");
            prev.setItemMeta(pMeta);
            inventory.setItem(45, prev);
        }

        if (page < maxPages) {
            ItemStack next = new ItemStack(Material.ARROW);
            ItemMeta nMeta = next.getItemMeta();
            nMeta.setDisplayName(ChatColor.YELLOW + "Page Suivante");
            next.setItemMeta(nMeta);
            inventory.setItem(53, next);
        }
    }

    public void handleRestitution(Player player, int rawSlot) {
        if (rawSlot >= 0 && rawSlot < 45) {
            int logIndex = (page - 1) * 45 + rawSlot;
            if (logIndex < logs.size()) {
                MagnetLogger.LogEntry log = logs.get(logIndex);
                player.getInventory().addItem(log.item.clone());
                player.sendMessage(ChatColor.GREEN + "L'item a été restitué avec succès !");
            }
        }
    }

    public void handlePagination(Player player, int rawSlot) {
        if (rawSlot == 45 && page > 1) {
            player.openInventory(new LogGUI(magnetId, logs, page - 1).getInventory());
        } else if (rawSlot == 53 && page < maxPages) {
            player.openInventory(new LogGUI(magnetId, logs, page + 1).getInventory());
        }
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
