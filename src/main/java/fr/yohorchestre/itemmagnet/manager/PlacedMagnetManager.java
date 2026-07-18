package fr.yohorchestre.itemmagnet.manager;

import fr.yohorchestre.itemmagnet.ItemMagnetPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.bukkit.NamespacedKey;

public class PlacedMagnetManager {

    private final ItemMagnetPlugin plugin;
    private final File dataFile;
    private FileConfiguration dataConfig;
    private final NamespacedKey holoKey;
    private int tickCounter = 0;
    
    // Map of Location string to UUID (to uniquely identify placed magnets)
    // Actually, we need to store the Base64 Inventory and Filters, and the state
    private final Map<Location, PlacedMagnet> placedMagnets = new HashMap<>();

    public PlacedMagnetManager(ItemMagnetPlugin plugin) {
        this.plugin = plugin;
        this.dataFile = new File(plugin.getDataFolder(), "placed_magnets.yml");
        this.holoKey = new NamespacedKey(plugin, "magnet_holo");
        loadData();
    }

    public void addPlacedMagnet(Location loc, ItemStack magnetItem) {
        MagnetManager manager = plugin.getMagnetManager();
        PlacedMagnet pm = new PlacedMagnet();
        pm.inventoryBase64 = magnetItem.getItemMeta().getPersistentDataContainer().get(new org.bukkit.NamespacedKey(plugin, "inventory"), org.bukkit.persistence.PersistentDataType.STRING);
        pm.filtersBase64 = magnetItem.getItemMeta().getPersistentDataContainer().get(new org.bukkit.NamespacedKey(plugin, "filters"), org.bukkit.persistence.PersistentDataType.STRING);
        pm.active = manager.isActive(magnetItem);
        pm.uuid = manager.getMagnetUUID(magnetItem);
        pm.lastAccessedTime = System.currentTimeMillis();
        
        placedMagnets.put(loc, pm);
        saveData();
        updateBlockVisuals(loc, pm, false);
    }

    public void updateLastAccessed(Location loc) {
        PlacedMagnet pm = placedMagnets.get(loc);
        if (pm != null) {
            pm.lastAccessedTime = System.currentTimeMillis();
        }
    }

    public boolean removePlacedMagnet(Location loc, Player player) {
        PlacedMagnet pm = placedMagnets.remove(loc);
        if (pm != null) {
            ItemStack item = plugin.getMagnetManager().createMagnet();
            org.bukkit.inventory.meta.ItemMeta meta = item.getItemMeta();
            
            meta.getPersistentDataContainer().set(new org.bukkit.NamespacedKey(plugin, "inventory"), org.bukkit.persistence.PersistentDataType.STRING, pm.inventoryBase64);
            meta.getPersistentDataContainer().set(new org.bukkit.NamespacedKey(plugin, "filters"), org.bukkit.persistence.PersistentDataType.STRING, pm.filtersBase64);
            meta.getPersistentDataContainer().set(new org.bukkit.NamespacedKey(plugin, "active"), org.bukkit.persistence.PersistentDataType.BYTE, (byte) (pm.active ? 1 : 0));
            meta.getPersistentDataContainer().set(new org.bukkit.NamespacedKey(plugin, "uuid"), org.bukkit.persistence.PersistentDataType.STRING, pm.uuid != null ? pm.uuid : UUID.randomUUID().toString());
            item.setItemMeta(meta);

            // Appliquer le bon affichage (lore, nom) via un toggle virtuel (hacky mais fonctionnel pour reset le lore)
            plugin.getMagnetManager().toggleActive(player, item);
            plugin.getMagnetManager().toggleActive(player, item);
            
            if (player == null || player.getGameMode() != org.bukkit.GameMode.CREATIVE) {
                loc.getWorld().dropItemNaturally(loc, item);
            }
            removeHologram(loc);
            saveData();
            return true;
        }
        return false;
    }

    public void startPlacedPickupTask() {
        int interval = plugin.getConfig().getInt("pickup-task-interval", 10);
        boolean wholeChunk = plugin.getConfig().getBoolean("suck-whole-chunk-when-placed", true);
        int radius = plugin.getConfig().getInt("radius-placed", 10);
        int borderInterval = plugin.getConfig().getInt("placed-magnet.border-particle-interval", 2) * 20;

        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            tickCounter += interval;
            boolean drawParticles = (tickCounter % borderInterval == 0);

            for (Map.Entry<Location, PlacedMagnet> entry : placedMagnets.entrySet()) {
                Location loc = entry.getKey();
                PlacedMagnet pm = entry.getValue();

                if (!pm.active) continue;

                // Check chunk is loaded
                if (!loc.getWorld().isChunkLoaded(loc.getBlockX() >> 4, loc.getBlockZ() >> 4)) continue;
                
                // Remove strict LODESTONE check as it can be a custom block
                if (loc.getBlock().getType() == Material.AIR) {
                    continue; // S'il a disparu d'une autre manière (WorldEdit)
                }

                Entity[] entitiesToScan;
                if (wholeChunk) {
                    Chunk chunk = loc.getChunk();
                    entitiesToScan = chunk.getEntities();
                } else {
                    entitiesToScan = loc.getWorld().getNearbyEntities(loc, radius, radius, radius).toArray(new Entity[0]);
                }

                if (drawParticles && (System.currentTimeMillis() - pm.lastAccessedTime) <= 10000) {
                    drawBorder(loc, radius, wholeChunk);
                }

                java.util.List<org.bukkit.inventory.Inventory> openInvs = new java.util.ArrayList<>();
                for (org.bukkit.entity.Player p : Bukkit.getOnlinePlayers()) {
                    if (p.getOpenInventory().getTopInventory().getHolder() instanceof fr.yohorchestre.itemmagnet.gui.MagnetGUI) {
                        fr.yohorchestre.itemmagnet.gui.MagnetGUI gui = (fr.yohorchestre.itemmagnet.gui.MagnetGUI) p.getOpenInventory().getTopInventory().getHolder();
                        if (loc.equals(gui.getBlockLocation())) {
                            openInvs.add(p.getOpenInventory().getTopInventory());
                        }
                    }
                }

                ItemStack[] contents;
                if (!openInvs.isEmpty()) {
                    contents = openInvs.get(0).getContents();
                } else {
                    contents = fr.yohorchestre.itemmagnet.utils.Base64Utils.itemStackArrayFromBase64(pm.inventoryBase64);
                }
                
                ItemStack[] filters = fr.yohorchestre.itemmagnet.utils.Base64Utils.itemStackArrayFromBase64(pm.filtersBase64);
                boolean inventoryChanged = false;

                for (Entity entity : entitiesToScan) {
                    if (!(entity instanceof Item)) continue;
                    Item itemEntity = (Item) entity;
                    if (!itemEntity.isValid() || itemEntity.isDead() || itemEntity.hasMetadata("magnet_removed") || itemEntity.getPickupDelay() > 0) continue;

                    ItemStack itemStack = itemEntity.getItemStack();

                    if (plugin.getConfig().getBoolean("worldguard-checks", true)) {
                        // Assuming no specific player, using block location for WG checks
                        if (!plugin.getWorldGuardHook().canBlockPickup(loc, itemEntity.getLocation())) {
                            continue;
                        }
                    }

                    if (plugin.getMagnetManager().isMagnet(itemStack)) continue;
                    if (!plugin.getMagnetManager().isAllowedByFilter(itemStack, filters)) continue;

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

                    if (remaining < itemStack.getAmount()) {
                        ItemStack suckedItem = itemStack.clone();
                        suckedItem.setAmount(itemStack.getAmount() - remaining);
                        plugin.getMagnetLogger().logSuckedItem(pm.uuid, itemEntity.getLocation(), null, suckedItem);
                        if (remaining <= 0) {
                            itemEntity.setMetadata("magnet_removed", new org.bukkit.metadata.FixedMetadataValue(plugin, true));
                            itemEntity.remove();
                        } else {
                            itemStack.setAmount(remaining);
                            itemEntity.setItemStack(itemStack);
                        }
                        loc.getWorld().playSound(loc, org.bukkit.Sound.ENTITY_ITEM_PICKUP, 0.2f, 2.0f);
                    }
                }

                if (inventoryChanged) {
                    if (!openInvs.isEmpty()) {
                        for (org.bukkit.inventory.Inventory inv : openInvs) {
                            inv.setContents(contents);
                            for (org.bukkit.entity.HumanEntity viewer : inv.getViewers()) {
                                if (viewer instanceof org.bukkit.entity.Player) {
                                    ((org.bukkit.entity.Player) viewer).updateInventory();
                                }
                            }
                        }
                    }
                    pm.inventoryBase64 = fr.yohorchestre.itemmagnet.utils.Base64Utils.itemStackArrayToBase64(contents);
                    updateHologram(loc, pm);
                }
            }
        }, interval, interval);
    }

    private void loadData() {
        if (!dataFile.exists()) {
            try {
                dataFile.getParentFile().mkdirs();
                dataFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        dataConfig = YamlConfiguration.loadConfiguration(dataFile);

        if (dataConfig.contains("magnets")) {
            for (String key : dataConfig.getConfigurationSection("magnets").getKeys(false)) {
                String path = "magnets." + key;
                World world = Bukkit.getWorld(dataConfig.getString(path + ".world"));
                if (world == null) continue;

                Location loc = new Location(
                        world,
                        dataConfig.getDouble(path + ".x"),
                        dataConfig.getDouble(path + ".y"),
                        dataConfig.getDouble(path + ".z")
                );

                PlacedMagnet pm = new PlacedMagnet();
                pm.inventoryBase64 = dataConfig.getString(path + ".inventory");
                pm.filtersBase64 = dataConfig.getString(path + ".filters");
                pm.active = dataConfig.getBoolean(path + ".active");
                pm.uuid = dataConfig.getString(path + ".uuid");
                if (pm.uuid != null && pm.uuid.length() > 9) {
                    pm.uuid = fr.yohorchestre.itemmagnet.manager.MagnetManager.generateShortId();
                    saveData();
                }

                placedMagnets.put(loc, pm);
                updateBlockVisuals(loc, pm, false);
            }
        }
    }

    public PlacedMagnet getPlacedMagnet(Location loc) {
        return placedMagnets.get(loc);
    }

    public void saveData() {
        if (dataConfig == null) {
            dataConfig = new YamlConfiguration();
        }
        dataConfig.set("magnets", null); // Clear old

        int i = 0;
        for (Map.Entry<Location, PlacedMagnet> entry : placedMagnets.entrySet()) {
            Location loc = entry.getKey();
            PlacedMagnet pm = entry.getValue();
            String path = "magnets." + i;
            
            dataConfig.set(path + ".world", loc.getWorld().getName());
            dataConfig.set(path + ".x", loc.getX());
            dataConfig.set(path + ".y", loc.getY());
            dataConfig.set(path + ".z", loc.getZ());
            dataConfig.set(path + ".inventory", pm.inventoryBase64);
            dataConfig.set(path + ".filters", pm.filtersBase64);
            dataConfig.set(path + ".active", pm.active);
            dataConfig.set(path + ".uuid", pm.uuid);
            i++;
        }

        try {
            dataConfig.save(dataFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Impossible de sauvegarder placed_magnets.yml");
            e.printStackTrace();
        }
    }

    public static class PlacedMagnet {
        public String inventoryBase64;
        public String filtersBase64;
        public boolean active;
        public String uuid;
        public long lastAccessedTime = 0;
    }

    public void updateBlockVisuals(Location loc, PlacedMagnet pm, boolean playToggleParticles) {
        if (plugin.getConfig().getBoolean("placed-magnet.change-color", true)) {
            Material target = pm.active ? Material.LIME_SHULKER_BOX : Material.RED_SHULKER_BOX;
            if (loc.getBlock().getType().name().endsWith("SHULKER_BOX") && loc.getBlock().getType() != target) {
                loc.getBlock().setType(target);
            }
        }
        
        if (playToggleParticles) {
            if (pm.active) {
                loc.getWorld().spawnParticle(org.bukkit.Particle.VILLAGER_HAPPY, loc.clone().add(0.5, 1.2, 0.5), 15, 0.3, 0.3, 0.3, 0.1);
            } else {
                loc.getWorld().spawnParticle(org.bukkit.Particle.SMOKE_NORMAL, loc.clone().add(0.5, 1.2, 0.5), 15, 0.3, 0.3, 0.3, 0.05);
            }
        }
        
        updateHologram(loc, pm);
    }

    public void updateHologram(Location loc, PlacedMagnet pm) {
        if (!plugin.getConfig().getBoolean("placed-magnet.hologram-enabled", true)) {
            removeHologram(loc);
            return;
        }
        
        org.bukkit.entity.TextDisplay holo = null;
        if (loc.getWorld().isChunkLoaded(loc.getBlockX() >> 4, loc.getBlockZ() >> 4)) {
            for (org.bukkit.entity.Entity e : loc.getWorld().getNearbyEntities(loc.clone().add(0.5, 1.5, 0.5), 2.0, 2.0, 2.0)) {
                if (e instanceof org.bukkit.entity.TextDisplay && e.getPersistentDataContainer().has(holoKey, org.bukkit.persistence.PersistentDataType.BYTE)) {
                    holo = (org.bukkit.entity.TextDisplay) e;
                    break;
                }
            }
            
            if (holo == null) {
                holo = loc.getWorld().spawn(loc.clone().add(0.5, 1.5, 0.5), org.bukkit.entity.TextDisplay.class);
                holo.getPersistentDataContainer().set(holoKey, org.bukkit.persistence.PersistentDataType.BYTE, (byte) 1);
                holo.setBillboard(org.bukkit.entity.Display.Billboard.CENTER);
                holo.setShadowed(true);
                holo.setAlignment(org.bukkit.entity.TextDisplay.TextAlignment.CENTER);
            }
            
            ItemStack[] contents = fr.yohorchestre.itemmagnet.utils.Base64Utils.itemStackArrayFromBase64(pm.inventoryBase64);
            int totalAmount = 0;
            int maxCapacity = 27 * 64; 
            for (ItemStack it : contents) {
                if (it != null && it.getType() != Material.AIR) {
                    totalAmount += it.getAmount();
                }
            }
            double percent = Math.min(100.0, (totalAmount * 100.0) / maxCapacity);
            String percentStr = String.format("%.1f", percent).replace(",", ".");
            
            String statusStr = pm.active ? "&aActif" : "&cInactif";
            
            List<String> lines = plugin.getConfig().getStringList("placed-magnet.hologram-text");
            StringBuilder finalStr = new StringBuilder();
            for (int i = 0; i < lines.size(); i++) {
                String line = lines.get(i).replace("%status%", statusStr).replace("%percent%", percentStr);
                finalStr.append(org.bukkit.ChatColor.translateAlternateColorCodes('&', line));
                if (i < lines.size() - 1) {
                    finalStr.append("\n");
                }
            }
            holo.setText(finalStr.toString());
        }
    }

    public void removeHologram(Location loc) {
        if (loc.getWorld().isChunkLoaded(loc.getBlockX() >> 4, loc.getBlockZ() >> 4)) {
            for (org.bukkit.entity.Entity e : loc.getWorld().getNearbyEntities(loc.clone().add(0.5, 1.5, 0.5), 2.0, 2.0, 2.0)) {
                if (e instanceof org.bukkit.entity.TextDisplay && e.getPersistentDataContainer().has(holoKey, org.bukkit.persistence.PersistentDataType.BYTE)) {
                    e.remove();
                }
            }
        }
    }

    private void drawBorder(Location center, int radius, boolean wholeChunk) {
        org.bukkit.World w = center.getWorld();
        double y = center.getY() + 1.0;
        double minX, maxX, minZ, maxZ;
        
        if (wholeChunk) {
            org.bukkit.Chunk chunk = center.getChunk();
            minX = chunk.getX() << 4;
            maxX = minX + 16;
            minZ = chunk.getZ() << 4;
            maxZ = minZ + 16;
        } else {
            minX = center.getBlockX() - radius;
            maxX = center.getBlockX() + radius + 1;
            minZ = center.getBlockZ() - radius;
            maxZ = center.getBlockZ() + radius + 1;
        }
        
        org.bukkit.Particle.DustOptions dust = new org.bukkit.Particle.DustOptions(org.bukkit.Color.LIME, 1.0f);
        
        for (double x = minX; x <= maxX; x += 1.0) {
            w.spawnParticle(org.bukkit.Particle.REDSTONE, x, y, minZ, 1, dust);
            w.spawnParticle(org.bukkit.Particle.REDSTONE, x, y, maxZ, 1, dust);
        }
        for (double z = minZ; z <= maxZ; z += 1.0) {
            w.spawnParticle(org.bukkit.Particle.REDSTONE, minX, y, z, 1, dust);
            w.spawnParticle(org.bukkit.Particle.REDSTONE, maxX, y, z, 1, dust);
        }
    }
}
