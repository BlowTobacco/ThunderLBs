package exposed.thunder.thunderLBs.scheduler;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.Objects;
import java.util.function.Consumer;

final class BukkitTaskScheduler implements RegionTaskScheduler {
    private final Plugin plugin;

    BukkitTaskScheduler(Plugin plugin) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
    }

    @Override
    public void execute(Location location, Runnable action) {
        executeOnMainThread(action);
    }

    @Override
    public TaskHandle run(Location location, Consumer<TaskHandle> action) {
        return schedule(action, 0L);
    }

    @Override
    public TaskHandle runDelayed(Location location, Consumer<TaskHandle> action, long delayTicks) {
        return schedule(action, delayTicks);
    }

    @Override
    public TaskHandle runAtFixedRate(Location location, Consumer<TaskHandle> action,
                                     long initialDelayTicks, long periodTicks) {
        Handle handle = new Handle();
        handle.attach(Bukkit.getScheduler().runTaskTimer(plugin,
                () -> action.accept(handle),
                Math.max(1L, initialDelayTicks),
                Math.max(1L, periodTicks)));
        return handle;
    }

    @Override
    public void execute(World world, int chunkX, int chunkZ, Runnable action) {
        executeOnMainThread(action);
    }

    @Override
    public void execute(Entity entity, Runnable action, Runnable retired) {
        if (Bukkit.isPrimaryThread()) {
            action.run();
            return;
        }
        if (!plugin.isEnabled()) {
            return;
        }
        Bukkit.getScheduler().runTask(plugin, () -> runIfValid(entity, action, retired));
    }

    @Override
    public void executeDelayed(Entity entity, Runnable action, Runnable retired, long delayTicks) {
        if (!plugin.isEnabled()) {
            return;
        }
        Bukkit.getScheduler().runTaskLater(plugin, () -> runIfValid(entity, action, retired),
                Math.max(1L, delayTicks));
    }

    @Override
    public void runGlobal(Runnable action) {
        if (!plugin.isEnabled()) {
            return;
        }
        Bukkit.getScheduler().runTask(plugin, action);
    }

    @Override
    public TaskHandle runGlobalAtFixedRate(Runnable action, long initialDelayTicks, long periodTicks) {
        Handle handle = new Handle();
        handle.attach(Bukkit.getScheduler().runTaskTimer(plugin, action,
                Math.max(1L, initialDelayTicks), Math.max(1L, periodTicks)));
        return handle;
    }

    private void executeOnMainThread(Runnable action) {
        if (Bukkit.isPrimaryThread()) {
            action.run();
            return;
        }
        if (!plugin.isEnabled()) {
            return;
        }
        Bukkit.getScheduler().runTask(plugin, action);
    }

    private TaskHandle schedule(Consumer<TaskHandle> action, long delayTicks) {
        Handle handle = new Handle();
        if (delayTicks <= 0L) {
            handle.attach(Bukkit.getScheduler().runTask(plugin, () -> action.accept(handle)));
        } else {
            handle.attach(Bukkit.getScheduler().runTaskLater(plugin, () -> action.accept(handle), delayTicks));
        }
        return handle;
    }

    private static void runIfValid(Entity entity, Runnable action, Runnable retired) {
        if (entity.isValid() || (entity instanceof Player player && player.isOnline())) {
            action.run();
        } else if (retired != null) {
            retired.run();
        }
    }

    private static final class Handle implements TaskHandle {
        private BukkitTask task;
        private boolean cancelled;

        private synchronized void attach(BukkitTask task) {
            this.task = task;
            if (cancelled) {
                task.cancel();
            }
        }

        @Override
        public synchronized void cancel() {
            cancelled = true;
            if (task != null) {
                task.cancel();
            }
        }

        @Override
        public synchronized boolean isCancelled() {
            return cancelled;
        }
    }
}
