package fr.yohorchestre.itemmagnet.listener;

import fr.yohorchestre.itemmagnet.ItemMagnetPlugin;
import fr.yohorchestre.itemmagnet.gui.FilterGUI;
import fr.yohorchestre.itemmagnet.gui.MagnetGUI;
import fr.yohorchestre.itemmagnet.gui.MainMenuGUI;
import fr.yohorchestre.itemmagnet.manager.MagnetManager;
import fr.yohorchestre.itemmagnet.manager.PlacedMagnetManager;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public class MagnetListener implements Listener {

    private final ItemMagnetPlugin plugin;

    public MagnetListener(ItemMagnetPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;

        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();

        MagnetManager manager = plugin.getMagnetManager();
        Action action = event.getAction();
        if (!manager.isMagnet(item)) {
            if (action == Action.RIGHT_CLICK_BLOCK && event.getClickedBlock() != null) {
                org.bukkit.Location loc = event.getClickedBlock().getLocation();
                var pm = plugin.getPlacedMagnetManager().getPlacedMagnet(loc);
                if (pm != null) {
                    event.setCancelled(true);
                    plugin.getPlacedMagnetManager().updateLastAccessed(loc);
                    if (player.isSneaking() || true) { // Clic droit normal ou sneak ouvre le menu
                        boolean alreadyOpen = false;
                        for (Player p : org.bukkit.Bukkit.getOnlinePlayers()) {
                            if (p.equals(player)) continue;
                            Inventory top = p.getOpenInventory().getTopInventory();
                            if (top != null && top.getHolder() != null) {
                                if (top.getHolder() instanceof MainMenuGUI && loc.equals(((MainMenuGUI) top.getHolder()).getBlockLocation())) alreadyOpen = true;
                                else if (top.getHolder() instanceof MagnetGUI && loc.equals(((MagnetGUI) top.getHolder()).getBlockLocation())) alreadyOpen = true;
                                else if (top.getHolder() instanceof FilterGUI && loc.equals(((FilterGUI) top.getHolder()).getBlockLocation())) alreadyOpen = true;
                            }
                        }
                        
                        if (alreadyOpen) {
                            player.sendMessage(org.bukkit.ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("messages.prefix", "") + "§cUn autre joueur consulte déjà cet aimant !"));
                            return;
                        }

                        ItemStack virtualMagnet = plugin.getMagnetManager().createMagnet();
                        org.bukkit.inventory.meta.ItemMeta meta = virtualMagnet.getItemMeta();
                        meta.getPersistentDataContainer().set(new org.bukkit.NamespacedKey(plugin, "inventory"), org.bukkit.persistence.PersistentDataType.STRING, pm.inventoryBase64);
                        meta.getPersistentDataContainer().set(new org.bukkit.NamespacedKey(plugin, "filters"), org.bukkit.persistence.PersistentDataType.STRING, pm.filtersBase64);
                        meta.getPersistentDataContainer().set(new org.bukkit.NamespacedKey(plugin, "active"), org.bukkit.persistence.PersistentDataType.BYTE, (byte) (pm.active ? 1 : 0));
                        meta.getPersistentDataContainer().set(new org.bukkit.NamespacedKey(plugin, "uuid"), org.bukkit.persistence.PersistentDataType.STRING, pm.uuid);
                        virtualMagnet.setItemMeta(meta);
                        
                        MainMenuGUI gui = new MainMenuGUI(plugin, player, virtualMagnet, -1, loc);
                        player.openInventory(gui.getInventory());
                    }
                }
            }
            return;
        }

        if (action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK) {
            if (action == Action.RIGHT_CLICK_BLOCK && event.getClickedBlock() != null) {
                // Laisser l'événement BlockPlaceEvent gérer la pose
                return;
            }
            event.setCancelled(true);
            if (player.isSneaking()) {
                // Ouvrir le Menu Principal
                MainMenuGUI gui = new MainMenuGUI(plugin, player, item, player.getInventory().getHeldItemSlot());
                player.openInventory(gui.getInventory());
            }
        }
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        ItemStack item = event.getItemInHand();
        if (plugin.getMagnetManager().isMagnet(item)) {
            plugin.getPlacedMagnetManager().addPlacedMagnet(event.getBlock().getLocation(), item);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        if (plugin.getPlacedMagnetManager().getPlacedMagnet(event.getBlock().getLocation()) != null) {
            forceCloseViewers(event.getBlock().getLocation());
            boolean wasMagnet = plugin.getPlacedMagnetManager().removePlacedMagnet(event.getBlock().getLocation(), event.getPlayer());
            if (wasMagnet && event.getPlayer().getGameMode() != GameMode.CREATIVE) {
                event.setDropItems(false); // Annuler le drop vanilla du bloc
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityExplode(org.bukkit.event.entity.EntityExplodeEvent event) {
        handleExplosion(event.blockList());
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockExplode(org.bukkit.event.block.BlockExplodeEvent event) {
        handleExplosion(event.blockList());
    }

    private void handleExplosion(java.util.List<org.bukkit.block.Block> blockList) {
        boolean protect = plugin.getConfig().getBoolean("disable-explosion-damage", true);
        java.util.Iterator<org.bukkit.block.Block> it = blockList.iterator();
        while (it.hasNext()) {
            org.bukkit.block.Block block = it.next();
            if (plugin.getPlacedMagnetManager().getPlacedMagnet(block.getLocation()) != null) {
                if (protect) {
                    it.remove(); // Protège le bloc
                } else {
                    forceCloseViewers(block.getLocation());
                    plugin.getPlacedMagnetManager().removePlacedMagnet(block.getLocation(), null);
                    it.remove();
                    block.setType(Material.AIR);
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPistonExtend(org.bukkit.event.block.BlockPistonExtendEvent event) {
        handlePiston(event.getBlocks(), event);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPistonRetract(org.bukkit.event.block.BlockPistonRetractEvent event) {
        handlePiston(event.getBlocks(), event);
    }

    private void handlePiston(java.util.List<org.bukkit.block.Block> blocks, org.bukkit.event.Cancellable event) {
        boolean protect = plugin.getConfig().getBoolean("disable-piston-break", true);
        for (org.bukkit.block.Block block : blocks) {
            if (plugin.getPlacedMagnetManager().getPlacedMagnet(block.getLocation()) != null) {
                if (protect) {
                    event.setCancelled(true);
                    return;
                } else {
                    forceCloseViewers(block.getLocation());
                    plugin.getPlacedMagnetManager().removePlacedMagnet(block.getLocation(), null);
                    block.setType(Material.AIR);
                }
            }
        }
    }

    private void forceCloseViewers(org.bukkit.Location loc) {
        for (Player p : org.bukkit.Bukkit.getOnlinePlayers()) {
            Inventory top = p.getOpenInventory().getTopInventory();
            if (top != null && top.getHolder() != null) {
                boolean shouldClose = false;
                if (top.getHolder() instanceof MainMenuGUI && loc.equals(((MainMenuGUI) top.getHolder()).getBlockLocation())) shouldClose = true;
                else if (top.getHolder() instanceof MagnetGUI && loc.equals(((MagnetGUI) top.getHolder()).getBlockLocation())) shouldClose = true;
                else if (top.getHolder() instanceof FilterGUI && loc.equals(((FilterGUI) top.getHolder()).getBlockLocation())) shouldClose = true;
                
                if (shouldClose) {
                    p.closeInventory();
                    p.sendMessage(org.bukkit.ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("messages.prefix", "") + "§cCet aimant a été détruit !"));
                }
            }
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        Inventory inv = event.getInventory();
        Player player = (Player) event.getWhoClicked();

        if (inv.getHolder() instanceof fr.yohorchestre.itemmagnet.log.LogGUI) {
            event.setCancelled(true);
            if (event.getClickedInventory() != null && event.getClickedInventory().equals(inv)) {
                fr.yohorchestre.itemmagnet.log.LogGUI gui = (fr.yohorchestre.itemmagnet.log.LogGUI) inv.getHolder();
                if (event.getRawSlot() < 45) {
                    gui.handleRestitution(player, event.getRawSlot());
                } else {
                    gui.handlePagination(player, event.getRawSlot());
                }
            }
            return;
        }

        if (inv.getHolder() instanceof MainMenuGUI) {
            event.setCancelled(true); // Pas de déplacement d'items dans ce menu
            
            MainMenuGUI gui = (MainMenuGUI) inv.getHolder();
            ItemStack currentItem = event.getCurrentItem();
            
            if (currentItem != null && currentItem.getType() != Material.AIR && currentItem.getType() != Material.BLACK_STAINED_GLASS_PANE) {
                int slot = event.getSlot();
                ItemStack magnet = null;
                
                if (gui.getBlockLocation() == null) {
                    magnet = player.getInventory().getItem(gui.getMagnetSlot());
                } else {
                    magnet = gui.getMagnetItem();
                }
                
                if (magnet == null || (!plugin.getMagnetManager().isMagnet(magnet) && gui.getBlockLocation() == null)) {
                    player.closeInventory();
                    return;
                }
                
                if (slot == 2) {
                    // Toggle
                    plugin.getMagnetManager().toggleActive(player, magnet);
                    if (gui.getBlockLocation() != null) {
                        // Mettre à jour l'état dans le PlacedMagnetManager
                        var pm = plugin.getPlacedMagnetManager().getPlacedMagnet(gui.getBlockLocation());
                        if (pm != null) {
                            pm.active = plugin.getMagnetManager().isActive(magnet);
                            plugin.getPlacedMagnetManager().saveData();
                            plugin.getPlacedMagnetManager().updateBlockVisuals(gui.getBlockLocation(), pm, true);
                        }
                    }
                    MainMenuGUI newGui = new MainMenuGUI(plugin, player, magnet, gui.getMagnetSlot(), gui.getBlockLocation());
                    player.openInventory(newGui.getInventory());
                } else if (slot == 4) {
                    // Inventaire
                    if (gui.getBlockLocation() != null) {
                        var pm = plugin.getPlacedMagnetManager().getPlacedMagnet(gui.getBlockLocation());
                        if (pm != null) {
                            org.bukkit.inventory.meta.ItemMeta meta = magnet.getItemMeta();
                            meta.getPersistentDataContainer().set(new org.bukkit.NamespacedKey(plugin, "inventory"), org.bukkit.persistence.PersistentDataType.STRING, pm.inventoryBase64);
                            meta.getPersistentDataContainer().set(new org.bukkit.NamespacedKey(plugin, "filters"), org.bukkit.persistence.PersistentDataType.STRING, pm.filtersBase64);
                            magnet.setItemMeta(meta);
                        }
                    }
                    MagnetGUI magGui = new MagnetGUI(plugin, player, magnet, gui.getMagnetSlot(), gui.getBlockLocation());
                    player.openInventory(magGui.getInventory());
                } else if (slot == 6) {
                    // Filtres
                    if (gui.getBlockLocation() != null) {
                        var pm = plugin.getPlacedMagnetManager().getPlacedMagnet(gui.getBlockLocation());
                        if (pm != null) {
                            org.bukkit.inventory.meta.ItemMeta meta = magnet.getItemMeta();
                            meta.getPersistentDataContainer().set(new org.bukkit.NamespacedKey(plugin, "inventory"), org.bukkit.persistence.PersistentDataType.STRING, pm.inventoryBase64);
                            meta.getPersistentDataContainer().set(new org.bukkit.NamespacedKey(plugin, "filters"), org.bukkit.persistence.PersistentDataType.STRING, pm.filtersBase64);
                            magnet.setItemMeta(meta);
                        }
                    }
                    FilterGUI filGui = new FilterGUI(plugin, player, magnet, gui.getMagnetSlot(), gui.getBlockLocation());
                    player.openInventory(filGui.getInventory());
                }
            }
            return;
        } else if (inv.getHolder() instanceof MagnetGUI) {
            MagnetGUI gui = (MagnetGUI) inv.getHolder();
            
            // Anti-Dupli: prevent clicking the magnet itself
            if (event.getClick().name().contains("NUMBER_KEY") && event.getHotbarButton() == gui.getMagnetSlot()) {
                event.setCancelled(true);
                return;
            }
            
            ItemStack currentItem = event.getCurrentItem();
            if (currentItem != null && plugin.getMagnetManager().isMagnet(currentItem)) {
                // Prevent putting a magnet inside a magnet (Inception) or moving the open magnet
                event.setCancelled(true);
                return;
            }
        } else if (inv.getHolder() instanceof FilterGUI) {
            FilterGUI gui = (FilterGUI) inv.getHolder();
            
            // Anti-Dupli
            if (event.getClick().name().contains("NUMBER_KEY") && event.getHotbarButton() == gui.getMagnetSlot()) {
                event.setCancelled(true);
                return;
            }
            
            ItemStack currentItem = event.getCurrentItem();
            if (currentItem != null && plugin.getMagnetManager().isMagnet(currentItem)) {
                event.setCancelled(true);
                return;
            }
            
            // Comportement des filtres : cloner l'item cliqué
            if (event.getClickedInventory() != null && event.getClickedInventory().equals(inv)) {
                event.setCancelled(true); // On annule le vrai clic
                ItemStack cursor = event.getCursor();
                if (cursor != null && cursor.getType() != Material.AIR) {
                    ItemStack filterItem = cursor.clone();
                    filterItem.setAmount(1);
                    inv.setItem(event.getSlot(), filterItem);
                } else {
                    inv.setItem(event.getSlot(), new ItemStack(Material.AIR));
                }
            } else if (event.isShiftClick() && event.getClickedInventory() != null && event.getClickedInventory().equals(player.getInventory())) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        Inventory inv = event.getInventory();
        if (inv.getHolder() instanceof MainMenuGUI) {
            event.setCancelled(true);
            return;
        }
        if (inv.getHolder() instanceof MagnetGUI || inv.getHolder() instanceof FilterGUI) {
            ItemStack dragged = event.getOldCursor();
            if (plugin.getMagnetManager().isMagnet(dragged)) {
                event.setCancelled(true);
                return;
            }
            if (inv.getHolder() instanceof FilterGUI) {
                // Pas de drag dans les filtres
                for (int slot : event.getRawSlots()) {
                    if (slot < inv.getSize()) {
                        event.setCancelled(true);
                        return;
                    }
                }
            }
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        Inventory inv = event.getInventory();
        Player player = (Player) event.getPlayer();

        if (inv.getHolder() instanceof MagnetGUI) {
            MagnetGUI gui = (MagnetGUI) inv.getHolder();
            
            if (gui.getBlockLocation() != null) {
                var pm = plugin.getPlacedMagnetManager().getPlacedMagnet(gui.getBlockLocation());
                if (pm != null) {
                    org.bukkit.inventory.ItemStack[] oldContents = fr.yohorchestre.itemmagnet.utils.Base64Utils.itemStackArrayFromBase64(pm.inventoryBase64);
                    org.bukkit.inventory.ItemStack[] newContents = inv.getContents();
                    
                    java.util.List<org.bukkit.inventory.ItemStack> added = fr.yohorchestre.itemmagnet.log.InventoryDiffUtil.getAddedItems(oldContents, newContents);
                    java.util.List<org.bukkit.inventory.ItemStack> removed = fr.yohorchestre.itemmagnet.log.InventoryDiffUtil.getRemovedItems(oldContents, newContents);
                    
                    for (org.bukkit.inventory.ItemStack item : added) {
                        plugin.getMagnetLogger().logManualAction(pm.uuid, player, item, true);
                    }
                    for (org.bukkit.inventory.ItemStack item : removed) {
                        plugin.getMagnetLogger().logManualAction(pm.uuid, player, item, false);
                    }

                    pm.inventoryBase64 = fr.yohorchestre.itemmagnet.utils.Base64Utils.itemStackArrayToBase64(newContents);
                    plugin.getPlacedMagnetManager().saveData();
                    plugin.getPlacedMagnetManager().updateHologram(gui.getBlockLocation(), pm);
                }
                // Animation de fermeture
                org.bukkit.block.BlockState state = gui.getBlockLocation().getBlock().getState();
                if (state instanceof org.bukkit.block.ShulkerBox) {
                    ((org.bukkit.block.ShulkerBox) state).close();
                }
            } else {
                org.bukkit.inventory.ItemStack magnet = player.getInventory().getItem(gui.getMagnetSlot());
                if (magnet != null && plugin.getMagnetManager().getMagnetUUID(magnet).equals(gui.getMagnetUUID())) {
                    org.bukkit.inventory.ItemStack[] oldContents = plugin.getMagnetManager().getMagnetInventory(magnet);
                    org.bukkit.inventory.ItemStack[] newContents = inv.getContents();
                    
                    java.util.List<org.bukkit.inventory.ItemStack> added = fr.yohorchestre.itemmagnet.log.InventoryDiffUtil.getAddedItems(oldContents, newContents);
                    java.util.List<org.bukkit.inventory.ItemStack> removed = fr.yohorchestre.itemmagnet.log.InventoryDiffUtil.getRemovedItems(oldContents, newContents);
                    
                    for (org.bukkit.inventory.ItemStack item : added) {
                        plugin.getMagnetLogger().logManualAction(gui.getMagnetUUID(), player, item, true);
                    }
                    for (org.bukkit.inventory.ItemStack item : removed) {
                        plugin.getMagnetLogger().logManualAction(gui.getMagnetUUID(), player, item, false);
                    }

                    plugin.getMagnetManager().saveMagnetInventory(magnet, newContents);
                }
            }
        } else if (inv.getHolder() instanceof FilterGUI) {
            FilterGUI gui = (FilterGUI) inv.getHolder();
            
            if (gui.getBlockLocation() != null) {
                var pm = plugin.getPlacedMagnetManager().getPlacedMagnet(gui.getBlockLocation());
                if (pm != null) {
                    pm.filtersBase64 = fr.yohorchestre.itemmagnet.utils.Base64Utils.itemStackArrayToBase64(inv.getContents());
                    plugin.getPlacedMagnetManager().saveData();
                }
            } else {
                ItemStack magnet = player.getInventory().getItem(gui.getMagnetSlot());
                if (magnet != null && plugin.getMagnetManager().getMagnetUUID(magnet).equals(gui.getMagnetUUID())) {
                    plugin.getMagnetManager().saveMagnetFilters(magnet, inv.getContents());
                }
            }
        }
    }

    @EventHandler
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        ItemStack item = event.getItemDrop().getItemStack();
        Player player = event.getPlayer();
        
        if (plugin.getMagnetManager().isMagnet(item)) {
            Inventory topInv = player.getOpenInventory().getTopInventory();
            if (topInv != null && (topInv.getHolder() instanceof MainMenuGUI || topInv.getHolder() instanceof MagnetGUI || topInv.getHolder() instanceof FilterGUI)) {
                // Empêcher de jeter l'aimant pendant que son interface est ouverte
                event.setCancelled(true);
            }
        }
    }
}
