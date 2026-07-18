package fr.yohorchestre.itemmagnet;

import fr.yohorchestre.itemmagnet.command.MagnetCommand;
import fr.yohorchestre.itemmagnet.listener.MagnetListener;
import fr.yohorchestre.itemmagnet.manager.MagnetManager;
import fr.yohorchestre.itemmagnet.manager.PlacedMagnetManager;
import fr.yohorchestre.itemmagnet.manager.WorldGuardHook;
import org.bukkit.plugin.java.JavaPlugin;

public class ItemMagnetPlugin extends JavaPlugin {

    private static ItemMagnetPlugin instance;
    private MagnetManager magnetManager;
    private PlacedMagnetManager placedMagnetManager;
    private WorldGuardHook worldGuardHook;
    private fr.yohorchestre.itemmagnet.log.MagnetLogger magnetLogger;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();

        this.worldGuardHook = new WorldGuardHook();
        if (getConfig().getBoolean("worldguard-checks", true)) {
            if (getServer().getPluginManager().getPlugin("WorldGuard") != null) {
                getLogger().info("WorldGuard détecté, vérifications activées.");
                worldGuardHook.setWorldGuardFound(true);
            } else {
                getLogger().warning("WorldGuard n'est pas installé, les vérifications sont désactivées.");
            }
        }

        this.magnetManager = new MagnetManager(this);
        this.placedMagnetManager = new PlacedMagnetManager(this);
        this.magnetLogger = new fr.yohorchestre.itemmagnet.log.MagnetLogger(this);

        getCommand("magnet").setExecutor(new MagnetCommand(this));
        getCommand("magnetlog").setExecutor(new fr.yohorchestre.itemmagnet.log.LogCommands(this));
        
        getServer().getPluginManager().registerEvents(new MagnetListener(this), this);

        // Lancer la tâche asynchrone / synchrone pour l'aspiration des items
        this.magnetManager.startPickupTask();
        this.placedMagnetManager.startPlacedPickupTask();

        getLogger().info("ItemMagnetPlugin a été activé !");
    }

    @Override
    public void onDisable() {
        if (placedMagnetManager != null) {
            placedMagnetManager.saveData();
        }
        getLogger().info("ItemMagnetPlugin a été désactivé.");
    }

    public static ItemMagnetPlugin getInstance() {
        return instance;
    }

    public MagnetManager getMagnetManager() {
        return magnetManager;
    }

    public PlacedMagnetManager getPlacedMagnetManager() {
        return placedMagnetManager;
    }

    public fr.yohorchestre.itemmagnet.log.MagnetLogger getMagnetLogger() {
        return magnetLogger;
    }

    public WorldGuardHook getWorldGuardHook() {
        return worldGuardHook;
    }
}
