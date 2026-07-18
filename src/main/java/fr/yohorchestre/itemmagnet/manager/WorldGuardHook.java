package fr.yohorchestre.itemmagnet.manager;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.flags.Flags;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import com.sk89q.worldguard.protection.regions.RegionQuery;
import org.bukkit.Location;
import org.bukkit.entity.Player;

public class WorldGuardHook {

    private boolean worldGuardFound = false;

    public void setWorldGuardFound(boolean found) {
        this.worldGuardFound = found;
    }

    public boolean canPickup(Player player, Location itemLocation) {
        if (!worldGuardFound) return true;

        try {
            RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
            RegionQuery query = container.createQuery();
            
            com.sk89q.worldedit.util.Location weLoc = BukkitAdapter.adapt(itemLocation);
            
            // Check ITEM_PICKUP flag
            return query.testState(weLoc, WorldGuardPlugin.inst().wrapPlayer(player), Flags.ITEM_PICKUP);
        } catch (Exception e) {
            // S'il y a une erreur d'API, par sécurité on autorise ou on log
            return true;
        }
    }

    public boolean canBlockPickup(Location blockLocation, Location itemLocation) {
        if (!worldGuardFound) return true;

        try {
            RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
            RegionQuery query = container.createQuery();
            
            com.sk89q.worldedit.util.Location weLocItem = BukkitAdapter.adapt(itemLocation);
            
            // For block magnet, we can't easily check player permission unless we store the player who placed it.
            // For now, we just check if ITEM_PICKUP is globally denied there.
            // If it's a claim, usually build is denied. We can just test state without LocalPlayer.
            return query.testState(weLocItem, null, Flags.ITEM_PICKUP);
        } catch (Exception e) {
            return true;
        }
    }
}
