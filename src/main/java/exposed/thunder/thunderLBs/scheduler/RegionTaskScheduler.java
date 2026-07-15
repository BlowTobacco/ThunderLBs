package exposed.thunder.thunderLBs.scheduler;

import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;

import java.util.Objects;
import java.util.function.Consumer;

/**
 * Small adapter around Paper's region scheduler. Paper implements this API on
 * traditional servers too, which lets the rest of the plugin use one scheduling
 * model on both Paper and Folia.
 */
public final class RegionTaskScheduler {
    private final Plugin plugin;

    public RegionTaskScheduler(Plugin plugin) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
    }

    public void execute(Location location, Runnable action) {
        Location anchor = requireWorld(location);
        if (Bukkit.isOwnedByCurrentRegion(anchor)) {
            action.run();
            return;
        }
        if (!plugin.isEnabled()) {
            return;
        }
        Bukkit.getRegionScheduler().execute(plugin, anchor, action);
    }

    public ScheduledTask run(Location location, Consumer<ScheduledTask> action) {
        return Bukkit.getRegionScheduler().run(plugin, requireWorld(location), action);
    }

    public ScheduledTask runDelayed(Location location, Consumer<ScheduledTask> action, long delayTicks) {
        Location anchor = requireWorld(location);
        if (delayTicks <= 0L) {
            return Bukkit.getRegionScheduler().run(plugin, anchor, action);
        }
        return Bukkit.getRegionScheduler().runDelayed(plugin, anchor, action, delayTicks);
    }

    public ScheduledTask runAtFixedRate(Location location, Consumer<ScheduledTask> action,
                                        long initialDelayTicks, long periodTicks) {
        return Bukkit.getRegionScheduler().runAtFixedRate(
                plugin,
                requireWorld(location),
                action,
                Math.max(1L, initialDelayTicks),
                Math.max(1L, periodTicks)
        );
    }

    public void execute(Entity entity, Runnable action, Runnable retired) {
        if (Bukkit.isOwnedByCurrentRegion(entity)) {
            action.run();
            return;
        }
        if (!plugin.isEnabled()) {
            return;
        }
        entity.getScheduler().execute(plugin, action, retired, 1L);
    }

    private static Location requireWorld(Location location) {
        Objects.requireNonNull(location, "location");
        World world = Objects.requireNonNull(location.getWorld(), "location world");
        return new Location(world, location.getX(), location.getY(), location.getZ());
    }
}
