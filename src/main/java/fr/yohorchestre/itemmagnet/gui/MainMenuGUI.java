package fr.yohorchestre.itemmagnet.gui;

import fr.yohorchestre.itemmagnet.ItemMagnetPlugin;
import fr.yohorchestre.itemmagnet.manager.MagnetManager;
import org.bukkit.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class MainMenuGUI implements InventoryHolder {

    private final Inventory inventory;
    private final ItemStack magnetItem;
    private final int magnetSlot;
    private final String magnetUUID;
    private final boolean isActive;
    private final org.bukkit.Location blockLocation;

    public MainMenuGUI(ItemMagnetPlugin plugin, Player player, ItemStack magnetItem, int magnetSlot) {
        this(plugin, player, magnetItem, magnetSlot, null);
    }

    public MainMenuGUI(ItemMagnetPlugin plugin, Player player, ItemStack magnetItem, int magnetSlot, org.bukkit.Location blockLocation) {
        this.magnetItem = magnetItem;
        this.magnetSlot = magnetSlot;
        this.blockLocation = blockLocation;
        
        MagnetManager manager = plugin.getMagnetManager();
        this.magnetUUID = manager.getMagnetUUID(magnetItem);
        this.isActive = manager.isActive(magnetItem);
        
        this.inventory = Bukkit.createInventory(this, 9, "Menu de l'Aimant");
        
        // Bouton Activation / Désactivation
        ItemStack toggleItem = new ItemStack(isActive ? Material.LIME_DYE : Material.GRAY_DYE);
        ItemMeta toggleMeta = toggleItem.getItemMeta();
        toggleMeta.setDisplayName(ChatColor.GRAY + "Statut: " + (isActive ? ChatColor.GREEN + "ACTIF" : ChatColor.RED + "INACTIF"));
        List<String> toggleLore = new ArrayList<>();
        toggleLore.add(ChatColor.YELLOW + "Cliquez pour changer le statut.");
        toggleMeta.setLore(toggleLore);
        toggleItem.setItemMeta(toggleMeta);
        
        // Bouton Inventaire
        ItemStack invItem = new ItemStack(Material.CHEST);
        ItemMeta invMeta = invItem.getItemMeta();
        invMeta.setDisplayName(ChatColor.AQUA + "Ouvrir l'Inventaire");
        invItem.setItemMeta(invMeta);
        
        // Bouton Filtres
        ItemStack filterItem = new ItemStack(Material.HOPPER);
        ItemMeta filterMeta = filterItem.getItemMeta();
        filterMeta.setDisplayName(ChatColor.GOLD + "Gérer les Filtres");
        filterItem.setItemMeta(filterMeta);
        
        // Placement
        inventory.setItem(2, toggleItem);
        inventory.setItem(4, invItem);
        inventory.setItem(6, filterItem);
        
        // Remplir le vide
        ItemStack glass = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta glassMeta = glass.getItemMeta();
        glassMeta.setDisplayName(" ");
        glass.setItemMeta(glassMeta);
        for (int i = 0; i < 9; i++) {
            if (inventory.getItem(i) == null) {
                inventory.setItem(i, glass);
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

