package exposed.thunder.thunderLBs.render.packet;

import exposed.thunder.thunderLBs.ThunderLBs;
import exposed.thunder.thunderLBs.leaderboard.Leaderboard;
import exposed.thunder.thunderLBs.render.BoardDisplay;
import exposed.thunder.thunderLBs.render.DisplayOptions;
import exposed.thunder.thunderLBs.render.RenderBackend;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import io.github.retrooper.packetevents.util.SpigotReflectionUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class PacketRenderBackend implements RenderBackend, Listener {
    private static final long VIEWER_REFRESH_TICKS = 20L;
    private static final long JOIN_SYNC_DELAY_TICKS = 1L;

    private final ThunderLBs plugin;
    private final Map<Leaderboard, Channel> channels = new ConcurrentHashMap<>();
    private ScheduledTask viewerTask;

    public PacketRenderBackend(ThunderLBs plugin) {
        this.plugin = plugin;
        Bukkit.getPluginManager().registerEvents(this, plugin);
        this.viewerTask = Bukkit.getGlobalRegionScheduler().runAtFixedRate(
                plugin,
                task -> refreshAllViewers(),
                VIEWER_REFRESH_TICKS,
                VIEWER_REFRESH_TICKS
        );
    }

    @Override
    public String name() {
        return "packet";
    }

    @Override
    public void register(Leaderboard board) {
        Channel channel = new Channel(board);
        channels.put(board, channel);
        Bukkit.getGlobalRegionScheduler().run(plugin, task -> refreshAllViewers());
    }

    @Override
    public void unregister(Leaderboard board) {
        Channel channel = channels.remove(board);
        if (channel != null) {
            channel.destroyAll();
        }
    }

    @Override
    public BoardDisplay spawn(Leaderboard board, Location location, DisplayOptions options) {
        Channel channel = options.viewer() == null ? channels.get(board) : null;
        PacketBoardDisplay display = new PacketBoardDisplay(
                this,
                SpigotReflectionUtil.generateEntityId(),
                UUID.randomUUID(),
                channel,
                location,
                options);
        if (options.viewer() != null) {
            display.sendFullSpawn(options.viewer());
        } else if (channel != null) {
            channel.displays.add(display);
            for (Player player : channel.viewerPlayers()) {
                display.sendFullSpawn(player);
            }
        }
        return display;
    }

    void forget(PacketBoardDisplay display) {
        Channel channel = display.channel();
        if (channel != null) {
            channel.displays.remove(display);
        }
    }

    @Override
    public void shutdown() {
        for (Channel channel : channels.values()) {
            channel.destroyAll();
        }
        channels.clear();
        if (viewerTask != null) {
            viewerTask.cancel();
            viewerTask = null;
        }
        HandlerList.unregisterAll(this);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        for (Channel channel : channels.values()) {
            channel.viewers.remove(uuid);
            channel.board.removeViewer(uuid);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        player.getScheduler().runDelayed(plugin, task -> synchronizeViewer(player), null, JOIN_SYNC_DELAY_TICKS);
    }

    @EventHandler
    public void onWorldChange(PlayerChangedWorldEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        for (Channel channel : channels.values()) {
            channel.viewers.remove(uuid);
            channel.board.removeViewer(uuid);
        }
        Player player = event.getPlayer();
        player.getScheduler().runDelayed(plugin, task -> synchronizeViewer(player), null, JOIN_SYNC_DELAY_TICKS);
    }

    private void refreshAllViewers() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.getScheduler().execute(plugin, () -> synchronizeViewer(player), null, 1L);
        }
    }

    private void synchronizeViewer(Player player) {
        if (!player.isOnline()) {
            return;
        }
        for (Channel channel : channels.values()) {
            channel.synchronizeViewer(player);
        }
    }

    final class Channel {
        private final Leaderboard board;
        final Map<UUID, Player> viewers = new ConcurrentHashMap<>();
        final List<PacketBoardDisplay> displays = new CopyOnWriteArrayList<>();

        private Channel(Leaderboard board) {
            this.board = board;
        }

        Collection<Player> viewerPlayers() {
            return viewers.values();
        }

        void synchronizeViewer(Player player) {
            Location origin = board.origin();
            World world = board.world();
            UUID uuid = player.getUniqueId();
            if (world == null || !world.equals(player.getWorld())) {
                removeViewer(uuid, player);
                return;
            }

            if (player.getLocation().distanceSquared(origin) > board.rangeSquared()) {
                removeViewer(uuid, player);
                return;
            }

            if (viewers.putIfAbsent(uuid, player) != null) {
                return;
            }
            int displayCount = displays.size();
            for (int i = 0; i < displayCount; i++) {
                displays.get(i).sendFullSpawn(player);
            }
            board.synchronizeViewer(player);
        }

        private void removeViewer(UUID uuid, Player player) {
            if (viewers.remove(uuid) == null) {
                return;
            }
            for (PacketBoardDisplay display : displays) {
                display.sendDestroy(player);
            }
            board.removeViewer(uuid);
        }

        private void dropAllViewers() {
            for (Player player : viewerPlayers()) {
                for (PacketBoardDisplay display : displays) {
                    display.sendDestroy(player);
                }
            }
            viewers.clear();
        }

        private void destroyAll() {
            while (!displays.isEmpty()) {
                displays.get(displays.size() - 1).remove();
            }
            viewers.clear();
        }
    }
}
