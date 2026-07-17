package exposed.thunder.thunderLBs.scheduler;

import exposed.thunder.thunderLBs.util.VersionSupport;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;

import java.util.function.Consumer;

// folia 1.20.1 added region schedulers but this didn't exist before so we have to hack around for 1.19.4-1.20
public interface RegionTaskScheduler {

    static RegionTaskScheduler create(Plugin plugin) {
        if (VersionSupport.foliaSchedulersAvailable()) {
            return new FoliaTaskScheduler(plugin);
        }
        return new BukkitTaskScheduler(plugin);
    }

    void execute(Location location, Runnable action);

    TaskHandle run(Location location, Consumer<TaskHandle> action);

    TaskHandle runDelayed(Location location, Consumer<TaskHandle> action, long delayTicks);

    TaskHandle runAtFixedRate(Location location, Consumer<TaskHandle> action,
                              long initialDelayTicks, long periodTicks);

    void execute(World world, int chunkX, int chunkZ, Runnable action);

    void execute(Entity entity, Runnable action, Runnable retired);

    void executeDelayed(Entity entity, Runnable action, Runnable retired, long delayTicks);

    void runGlobal(Runnable action);

    TaskHandle runGlobalAtFixedRate(Runnable action, long initialDelayTicks, long periodTicks);
}
