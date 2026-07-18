package fr.yohorchestre.itemmagnet.gui;

import fr.yohorchestre.itemmagnet.ItemMagnetPlugin;
import fr.yohorchestre.itemmagnet.manager.MagnetManager;
import org.bukkit.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

public class FilterGUI implements InventoryHolder {

    private final Inventory inventory;
    private final ItemStack magnetItem;
    private final int magnetSlot;
    private final String magnetUUID;
    private final org.bukkit.Location blockLocation;

    public FilterGUI(ItemMagnetPlugin plugin, Player player, ItemStack magnetItem, int magnetSlot) {
        this(plugin, player, magnetItem, magnetSlot, null);
    }

    public FilterGUI(ItemMagnetPlugin plugin, Player player, ItemStack magnetItem, int magnetSlot, org.bukkit.Location blockLocation) {
        this.magnetItem = magnetItem;
        this.magnetSlot = magnetSlot;
        this.blockLocation = blockLocation;
        
        MagnetManager manager = plugin.getMagnetManager();
        this.magnetUUID = manager.getMagnetUUID(magnetItem);
        
        String title = plugin.getConfig().getString("messages.gui-filter-title", "Filtres de l'aimant");
        this.inventory = Bukkit.createInventory(this, 9, ChatColor.translateAlternateColorCodes('&', title));
        
        ItemStack[] contents = manager.getMagnetFilters(magnetItem);
        for (int i = 0; i < contents.length && i < 9; i++) {
            if (contents[i] != null) {
                inventory.setItem(i, contents[i]);
            }
        }
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    public ItemStack getMagnetItem() {
        return magnetItem;
    }

    public int getMagnetSlot() {
        return magnetSlot;
    }

    public String getMagnetUUID() {
        return magnetUUID;
    }

    public org.bukkit.Location getBlockLocation() {
        return blockLocation;
    }

}

