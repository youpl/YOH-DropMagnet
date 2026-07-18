package fr.yohorchestre.itemmagnet.manager;

import fr.yohorchestre.itemmagnet.ItemMagnetPlugin;
import fr.yohorchestre.itemmagnet.utils.Base64Utils;
import org.bukkit.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class MagnetManager {

    private final ItemMagnetPlugin plugin;
    private final NamespacedKey magnetKey;
    private final NamespacedKey activeKey;
    private final NamespacedKey inventoryKey;
    private final NamespacedKey filtersKey;
    private final NamespacedKey uuidKey;
    
    private final Map<UUID, Long> lastFullMessage = new HashMap<>();

    public MagnetManager(ItemMagnetPlugin plugin) {
        this.plugin = plugin;
        this.magnetKey = new NamespacedKey(plugin, "is_magnet");
        this.activeKey = new NamespacedKey(plugin, "active");
        this.inventoryKey = new NamespacedKey(plugin, "inventory");
        this.filtersKey = new NamespacedKey(plugin, "filters");
        this.uuidKey = new NamespacedKey(plugin, "uuid");
    }

    public boolean isMagnet(ItemStack item) {
        if (item == null || item.getType() == Material.AIR || !item.hasItemMeta()) return false;
        return item.getItemMeta().getPersistentDataContainer().has(magnetKey, PersistentDataType.BYTE);
    }

    public boolean isActive(ItemStack item) {
        if (!isMagnet(item)) return false;
        Byte active = item.getItemMeta().getPersistentDataContainer().get(activeKey, PersistentDataType.BYTE);
        return active != null && active == 1;
    }

    public void toggleActive(Player player, ItemStack item) {
        if (!isMagnet(item)) return;
        ItemMeta meta = item.getItemMeta();
        boolean active = isActive(item);
        meta.getPersistentDataContainer().set(activeKey, PersistentDataType.BYTE, (byte) (active ? 0 : 1));
        
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "Actif: " + (!active ? ChatColor.GREEN + "Oui" : ChatColor.RED + "Non"));
        lore.add("");
        lore.add(ChatColor.YELLOW + "Sneak + Clic Droit: " + ChatColor.GRAY + "Ouvrir le Menu");

        String uuid = getMagnetUUID(item);
        if (uuid != null) {
            lore.add("");
            lore.add(ChatColor.DARK_GRAY + "#" + uuid);
        }

        meta.setLore(lore);

        if (!active) {
            meta.setDisplayName(ChatColor.AQUA + "Aimant à Item " + ChatColor.GREEN + "[ACTIF]");
        } else {
            meta.setDisplayName(ChatColor.AQUA + "Aimant à Item " + ChatColor.RED + "[DÉSACTIVÉ]");
        }

        item.setItemMeta(meta);
        if (player != null) {
            player.playSound(player.getLocation(), !active ? Sound.BLOCK_NOTE_BLOCK_PLING : Sound.BLOCK_NOTE_BLOCK_BASS, 1.0f, 1.0f);
        }
    }

    public ItemStack createMagnet() {
        String matName = plugin.getConfig().getString("magnet-item.material", "PURPLE_SHULKER_BOX");
        Material mat = Material.getMaterial(matName.toUpperCase());
        if (mat == null) mat = Material.PURPLE_SHULKER_BOX;
        
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        
        int cmd = plugin.getConfig().getInt("magnet-item.custom-model-data", 0);
        if (cmd > 0) {
            meta.setCustomModelData(cmd);
        }
        
        meta.setDisplayName(ChatColor.AQUA + "Aimant à Item " + ChatColor.RED + "[DÉSACTIVÉ]");
        
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "Actif: " + ChatColor.RED + "Non");
        lore.add("");
        lore.add(ChatColor.YELLOW + "Sneak + Clic Droit: " + ChatColor.GRAY + "Ouvrir le Menu");
        String shortId = generateShortId();
        lore.add("");
        lore.add(ChatColor.DARK_GRAY + "#" + shortId);
        meta.setLore(lore);

        meta.getPersistentDataContainer().set(magnetKey, PersistentDataType.BYTE, (byte) 1);
        meta.getPersistentDataContainer().set(activeKey, PersistentDataType.BYTE, (byte) 0);
        meta.getPersistentDataContainer().set(uuidKey, PersistentDataType.STRING, shortId);
        
        // Init empty inventory 27 slots
        Inventory emptyInv = Bukkit.createInventory(null, 27);
        meta.getPersistentDataContainer().set(inventoryKey, PersistentDataType.STRING, Base64Utils.itemStackArrayToBase64(emptyInv.getContents()));
        
        // Init empty filters 9 slots
        Inventory emptyFilters = Bukkit.createInventory(null, 9);
        meta.getPersistentDataContainer().set(filtersKey, PersistentDataType.STRING, Base64Utils.itemStackArrayToBase64(emptyFilters.getContents()));
        
        item.setItemMeta(meta);
        return item;
    }

    public ItemStack[] getMagnetInventory(ItemStack item) {
        if (!isMagnet(item)) return new ItemStack[27];
        String base64 = item.getItemMeta().getPersistentDataContainer().get(inventoryKey, PersistentDataType.STRING);
        if (base64 == null) return new ItemStack[27];
        return Base64Utils.itemStackArrayFromBase64(base64);
    }

    public void saveMagnetInventory(ItemStack item, ItemStack[] contents) {
        if (!isMagnet(item)) return;
        ItemMeta meta = item.getItemMeta();
        meta.getPersistentDataContainer().set(inventoryKey, PersistentDataType.STRING, Base64Utils.itemStackArrayToBase64(contents));
        item.setItemMeta(meta);
    }

    public ItemStack[] getMagnetFilters(ItemStack item) {
        if (!isMagnet(item)) return new ItemStack[9];
        String base64 = item.getItemMeta().getPersistentDataContainer().get(filtersKey, PersistentDataType.STRING);
        if (base64 == null) return new ItemStack[9];
        return Base64Utils.itemStackArrayFromBase64(base64);
    }

    public void saveMagnetFilters(ItemStack item, ItemStack[] contents) {
        if (!isMagnet(item)) return;
        ItemMeta meta = item.getItemMeta();
        meta.getPersistentDataContainer().set(filtersKey, PersistentDataType.STRING, Base64Utils.itemStackArrayToBase64(contents));
        item.setItemMeta(meta);
    }

    public boolean isAllowedByFilter(ItemStack itemStack, ItemStack[] filters) {
        boolean hasFilter = false;
        for (ItemStack filter : filters) {
            if (filter != null && filter.getType() != Material.AIR) {
                hasFilter = true;
                if (filter.getType() == itemStack.getType()) {
                    return true;
                }
            }
        }
        return !hasFilter; // Si aucun filtre n'est configuré, tout passe
    }

    public String getMagnetUUID(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return null;
        String uuid = item.getItemMeta().getPersistentDataContainer().get(uuidKey, PersistentDataType.STRING);
        if (uuid != null && uuid.length() > 9) {
            String newId = generateShortId();
            ItemMeta meta = item.getItemMeta();
            meta.getPersistentDataContainer().set(uuidKey, PersistentDataType.STRING, newId);
            List<String> lore = meta.getLore();
            if (lore == null) lore = new ArrayList<>();
            lore.add("");
            lore.add(ChatColor.DARK_GRAY + "#" + newId);
            meta.setLore(lore);
            item.setItemMeta(meta);
            return newId;
        }
        return uuid;
    }

    public static String generateShortId() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        StringBuilder sb = new StringBuilder(9);
        java.util.Random random = new java.util.Random();
        for (int i = 0; i < 9; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString();
    }

    public void startPickupTask() {
        int interval = plugin.getConfig().getInt("pickup-task-interval", 10);
        int radius = plugin.getConfig().getInt("radius-inventory", 10);
        
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                // Find magnet in inventory
                for (int i = 0; i < player.getInventory().getSize(); i++) {
                    ItemStack item = player.getInventory().getItem(i);
                    if (isMagnet(item) && isActive(item)) {
                        processPickupForPlayer(player, item, i, radius);
                        break; // Only use the first active magnet found
                    }
                }
            }
        }, interval, interval);
    }

    private void processPickupForPlayer(Player player, ItemStack magnet, int slot, int radius) {
        Location loc = player.getLocation();
        Collection<Entity> entities = loc.getWorld().getNearbyEntities(loc, radius, radius, radius, entity -> entity instanceof Item);
        
        ItemStack[] filters = getMagnetFilters(magnet);
        
        org.bukkit.inventory.Inventory openInv = null;
        if (player.getOpenInventory().getTopInventory().getHolder() instanceof fr.yohorchestre.itemmagnet.gui.MagnetGUI) {
            fr.yohorchestre.itemmagnet.gui.MagnetGUI gui = (fr.yohorchestre.itemmagnet.gui.MagnetGUI) player.getOpenInventory().getTopInventory().getHolder();
            if (gui.getMagnetUUID().equals(getMagnetUUID(magnet))) {
                openInv = player.getOpenInventory().getTopInventory();
            }
        }
        
        ItemStack[] contents = openInv != null ? openInv.getContents() : getMagnetInventory(magnet);
        boolean inventoryChanged = false;
        boolean becameFull = false;

        for (Entity entity : entities) {
            Item itemEntity = (Item) entity;
            if (!itemEntity.isValid() || itemEntity.isDead() || itemEntity.hasMetadata("magnet_removed") || itemEntity.getPickupDelay() > 0) continue;

            ItemStack itemStack = itemEntity.getItemStack();
            
            // Check worldguard
            if (plugin.getConfig().getBoolean("worldguard-checks", true)) {
                if (!plugin.getWorldGuardHook().canPickup(player, itemEntity.getLocation())) {
                    continue;
                }
            }

            if (isMagnet(itemStack)) continue; // Ne pas s'aspirer soi-même
            if (!isAllowedByFilter(itemStack, filters)) continue;

            // Try to add to magnet inventory
            int remaining = itemStack.getAmount();
            for (int i = 0; i < contents.length; i++) {
                if (remaining <= 0) break;
                
                ItemStack slotItem = contents[i];
                if (slotItem == null || slotItem.getType() == Material.AIR) {
                    contents[i] = itemStack.clone();
                    contents[i].setAmount(remaining);
                    remaining = 0;
                    inventoryChanged = true;
                } else if (slotItem.isSimilar(itemStack) && slotItem.getAmount() < slotItem.getMaxStackSize()) {
                    int space = slotItem.getMaxStackSize() - slotItem.getAmount();
                    int toAdd = Math.min(space, remaining);
                    slotItem.setAmount(slotItem.getAmount() + toAdd);
                    remaining -= toAdd;
                    inventoryChanged = true;
                }
            }

            if (remaining > 0) {
                becameFull = true;
            }

            if (remaining < itemStack.getAmount()) {
                inventoryChanged = true;
                ItemStack suckedItem = itemStack.clone();
                suckedItem.setAmount(itemStack.getAmount() - remaining);
                plugin.getMagnetLogger().logSuckedItem(getMagnetUUID(magnet), itemEntity.getLocation(), player, suckedItem);
                if (remaining <= 0) {
                    itemEntity.setMetadata("magnet_removed", new org.bukkit.metadata.FixedMetadataValue(plugin, true));
                    itemEntity.remove();
                } else {
                    itemStack.setAmount(remaining);
                    itemEntity.setItemStack(itemStack);
                }
                player.playSound(player.getLocation(), Sound.ENTITY_ITEM_PICKUP, 0.2f, 2.0f);
            }
        }

        if (becameFull) {
            sendFullMessage(player);
        }

        if (inventoryChanged) {
            if (openInv != null) {
                openInv.setContents(contents);
            }
            saveMagnetInventory(magnet, contents);
            player.getInventory().setItem(slot, magnet);
        }
    }

    private void sendFullMessage(Player player) {
        if (!plugin.getConfig().getBoolean("full-message.enabled", true)) return;
        
        long now = System.currentTimeMillis();
        long last = lastFullMessage.getOrDefault(player.getUniqueId(), 0L);
        if (now - last < 5000) return; // Une fois toutes les 5 secondes max
        
        lastFullMessage.put(player.getUniqueId(), now);
        
        String type = plugin.getConfig().getString("full-message.type", "ACTIONBAR").toUpperCase();
        String text = plugin.getConfig().getString("full-message.text", "&c⚠️ Votre Aimant à Item est PLEIN ! ⚠️");
        String comp = ChatColor.translateAlternateColorCodes('&', text);
        
        switch (type) {
            case "CHAT":
                player.sendMessage(comp);
                break;
            case "TITLE":
                player.sendTitle(comp, "", 10, 70, 20);
                break;
            case "BOSSBAR":
                org.bukkit.boss.BossBar bossBar = Bukkit.createBossBar(comp, org.bukkit.boss.BarColor.RED, org.bukkit.boss.BarStyle.SOLID);
                bossBar.addPlayer(player);
                Bukkit.getScheduler().runTaskLater(plugin, () -> bossBar.removeAll(), 60L); // 3 secondes
                break;
            case "ACTIONBAR":
            default:
                player.spigot().sendMessage(net.md_5.bungee.api.ChatMessageType.ACTION_BAR, net.md_5.bungee.api.chat.TextComponent.fromLegacyText(comp));
                break;
        }
    }
}
