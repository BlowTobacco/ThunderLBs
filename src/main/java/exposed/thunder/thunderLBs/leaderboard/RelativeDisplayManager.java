package exposed.thunder.thunderLBs.leaderboard;

import exposed.thunder.thunderLBs.ThunderLBs;
import exposed.thunder.thunderLBs.config.PluginConfig;
import exposed.thunder.thunderLBs.placeholder.PlaceholderBridge;
import exposed.thunder.thunderLBs.render.BoardDisplay;
import exposed.thunder.thunderLBs.render.DisplayOptions;
import exposed.thunder.thunderLBs.scheduler.RegionTaskScheduler;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public final class RelativeDisplayManager {
    private static final long REFRESH_TICKS = 20L;
    private static final int GLIDE_TICKS = 10;
    private static final int ANIMATION_INTERPOLATION_TICKS = 1;
    private static final int OPACITY_TRANSPARENT = 0;
    private static final int OPACITY_OPAQUE = 255;

    private final ThunderLBs plugin;
    private final Leaderboard leaderboard;
    private final PlaceholderBridge placeholderBridge;
    private final LeaderboardDefinition definition;
    private final Location origin;
    private final World world;
    private final Location displayLocation;
    private final double rangeSquared;
    private final Map<UUID, BoardDisplay> playerDisplays = new HashMap<>();
    private final Map<UUID, String> lastRenderedContent = new HashMap<>();
    private final List<ScheduledTask> animationTasks = new ArrayList<>();
    private final RegionTaskScheduler scheduler;
    private volatile LeaderboardPage activePage;
    private volatile ContentContext contentContext;
    private ScheduledTask refreshTask;
    private float animationOffset;
    private byte animationOpacity = packOpacity(OPACITY_TRANSPARENT);
    private boolean pageExitComplete;
    private boolean warnedUnsupported;

    public RelativeDisplayManager(ThunderLBs plugin, Leaderboard leaderboard, PlaceholderBridge placeholderBridge) {
        this.plugin = plugin;
        this.leaderboard = leaderboard;
        this.placeholderBridge = placeholderBridge;
        this.definition = leaderboard.definition();
        this.origin = leaderboard.origin();
        this.world = leaderboard.world();
        this.displayLocation = calculateLocation(definition);
        this.rangeSquared = leaderboard.rangeSquared();
        this.scheduler = new RegionTaskScheduler(plugin);
    }

    public void start() {
        stopTask();
        if (!definition.settings().showRelativePosition()) {
            return;
        }
        refreshTask = Bukkit.getGlobalRegionScheduler().runAtFixedRate(
                plugin,
                task -> refreshPlayers(),
                REFRESH_TICKS,
                REFRESH_TICKS
        );
        refresh();
    }

    public void stop() {
        stopTask();
        cancelAnimationTasks();
        activePage = null;
        contentContext = null;
        pageExitComplete = false;
        cleanup();
    }

    private void stopTask() {
        if (refreshTask != null) {
            refreshTask.cancel();
            refreshTask = null;
        }
    }

    private void cancelAnimationTasks() {
        for (ScheduledTask task : animationTasks) {
            if (task != null && !task.isCancelled()) {
                task.cancel();
            }
        }
        animationTasks.clear();
    }

    public void cleanup() {
        for (BoardDisplay display : playerDisplays.values()) {
            if (display != null) {
                leaderboard.untrack(display);
                display.remove();
            }
        }
        playerDisplays.clear();
        lastRenderedContent.clear();
    }

    public void updatePage(LeaderboardPage page) {
        ContentContext nextContext = createContentContext(page);
        activatePage(page, nextContext, entranceDelay());
        refresh();
    }

    private void activatePage(LeaderboardPage page, ContentContext context, long delay) {
        activePage = page;
        contentContext = context;
        pageExitComplete = false;
        startPageAnimation(delay);
    }

    private long entranceDelay() {
        long rowDelay = Math.max(1L, leaderboard.definition().settings().rowDelayTicks());
        return rowDelay * Math.max(1, leaderboard.definition().settings().positions());
    }

    private void startPageAnimation(long entranceDelay) {
        cancelAnimationTasks();
        float[] offsets = leaderboard.animationCache().rowInOffsets();
        float initialOffset = offsets.length == 0 ? 0.0F : offsets[0];
        applyAnimationState(initialOffset, packOpacity(OPACITY_TRANSPARENT));

        ScheduledTask entrance = scheduler.runAtFixedRate(origin, new java.util.function.Consumer<>() {
            private int frame;

            @Override
            public void accept(ScheduledTask task) {
                if (frame >= offsets.length) {
                    task.cancel();
                    float finalOffset = offsets.length == 0 ? 0.0F : offsets[offsets.length - 1];
                    applyAnimationState(finalOffset, packOpacity(OPACITY_OPAQUE));
                    schedulePageExit();
                    return;
                }
                applyAnimationState(
                        offsets[frame],
                        opacityAtFrame(frame, offsets.length, true)
                );
                frame++;
            }
        }, Math.max(1L, entranceDelay), 1L);
        animationTasks.add(entrance);
    }

    private void schedulePageExit() {
        float[] offsets = leaderboard.animationCache().rowOutOffsets();
        long delay = Math.max(1L, leaderboard.definition().settings().pageDurationTicks());
        ScheduledTask exit = scheduler.runAtFixedRate(origin, new java.util.function.Consumer<>() {
            private int frame;

            @Override
            public void accept(ScheduledTask task) {
                if (frame >= offsets.length) {
                    task.cancel();
                    float finalOffset = offsets.length == 0 ? 0.0F : offsets[offsets.length - 1];
                    applyAnimationState(finalOffset, packOpacity(OPACITY_TRANSPARENT));
                    pageExitComplete = true;
                    return;
                }
                applyAnimationState(
                        offsets[frame],
                        opacityAtFrame(frame, offsets.length, false)
                );
                frame++;
            }
        }, delay, 1L);
        animationTasks.add(exit);
    }

    private void applyAnimationState(float offset, byte opacity) {
        this.animationOffset = offset;
        this.animationOpacity = opacity;
        for (BoardDisplay display : playerDisplays.values()) {
            if (display == null || display.isRemoved()) {
                continue;
            }
            display.interpolation(0, ANIMATION_INTERPOLATION_TICKS);
            display.transformAndOpacity(offset, 0.0F, 0.0F, 1.0F, 1.0F, 1.0F, opacity);
        }
    }

    public void refresh() {
        LeaderboardDefinition def = definition;
        if (!def.settings().showRelativePosition()) {
            scheduler.execute(origin, this::cleanup);
            return;
        }
        if (activePage == null) {
            scheduler.execute(origin, this::cleanup);
            return;
        }
        if (contentContext == null || contentContext.rankPlaceholder().isEmpty()) {
            if (!warnedUnsupported) {
                warnedUnsupported = true;
                plugin.getLogger().warning("Provider '" + plugin.getPluginConfig().providerName()
                        + "' has no viewer rank placeholder for leaderboard '" + def.id()
                        + "'; the relative row is disabled.");
            }
            scheduler.execute(origin, this::cleanup);
            return;
        }

        if (world == null) {
            scheduler.execute(origin, this::cleanup);
            return;
        }

        Bukkit.getGlobalRegionScheduler().run(plugin, task -> refreshPlayers());
    }

    private void refreshPlayers() {
        Set<UUID> online = new HashSet<>();
        for (Player player : Bukkit.getOnlinePlayers()) {
            online.add(player.getUniqueId());
            player.getScheduler().execute(plugin, () -> refreshOnPlayer(player), null, 1L);
        }
        scheduler.execute(origin, () -> removeOfflineDisplays(online));
    }

    private void removeOfflineDisplays(Set<UUID> online) {
        Iterator<Map.Entry<UUID, BoardDisplay>> it = playerDisplays.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, BoardDisplay> entry = it.next();
            if (!online.contains(entry.getKey())) {
                BoardDisplay display = entry.getValue();
                if (display != null) {
                    leaderboard.untrack(display);
                    display.remove();
                }
                lastRenderedContent.remove(entry.getKey());
                it.remove();
            }
        }
    }

    private void refreshOnPlayer(Player player) {
        LeaderboardPage page = activePage;
        ContentContext context = contentContext;
        if (!definition.settings().showRelativePosition() || page == null || context == null
                || context.rankPlaceholder().isEmpty()) {
            scheduler.execute(origin, () -> removeDisplay(player.getUniqueId()));
            return;
        }

        if (world == null || !world.equals(player.getWorld())
                || player.getLocation().distanceSquared(origin) > rangeSquared) {
            scheduler.execute(origin, () -> removeDisplay(player.getUniqueId()));
            return;
        }

        String rendered = renderContent(player, page, context);
        scheduler.execute(origin, () -> {
            if (activePage == page && contentContext == context) {
                updatePlayerDisplay(player, rendered);
            }
        });
    }

    void removeViewer(UUID playerId) {
        removeDisplay(playerId);
    }

    private void updatePlayerDisplay(Player player, String rendered) {
        BoardDisplay display = playerDisplays.get(player.getUniqueId());

        if (display == null || display.isRemoved()) {
            display = spawnDisplay(player);
            playerDisplays.put(player.getUniqueId(), display);
        }

        updateContent(display, player.getUniqueId(), rendered);
    }

    private Location calculateLocation(LeaderboardDefinition def) {
        PluginConfig.Defaults defaults = plugin.getPluginConfig().defaults();
        int positions = Math.max(1, def.settings().positions());
        double offsetY = defaults.rowStartOffset()
                - ((positions + 1) * defaults.rowSpacing())
                + defaults.relativeOffset();
        return origin.clone().add(0, offsetY, 0);
    }

    private BoardDisplay spawnDisplay(Player viewer) {
        BoardDisplay display = plugin.renderBackend().spawn(leaderboard, displayLocation, new DisplayOptions()
                .billboard(definition.billboard())
                .shadowed(definition.textShadow())
                .opacity(animationOpacity)
                .interpolationDuration(ANIMATION_INTERPOLATION_TICKS)
                .teleportDuration(GLIDE_TICKS)
                .translation(animationOffset, 0.0F, 0.0F)
                .viewRange(Math.max(0.1F, definition.viewDistance() / 64.0F))
                .viewer(viewer));
        leaderboard.track(display);
        return display;
    }

    private String rankPattern(LeaderboardDefinition def) {
        PluginConfig.Provider provider = plugin.getPluginConfig().provider();
        return def.type() == LeaderboardType.GROUP ? provider.groupViewerRank() : provider.viewerRank();
    }

    private String valuePattern(LeaderboardDefinition def) {
        PluginConfig.Provider provider = plugin.getPluginConfig().provider();
        return def.type() == LeaderboardType.GROUP ? provider.groupViewerValue() : provider.viewerValue();
    }

    private ContentContext createContentContext(LeaderboardPage page) {
        PluginConfig config = plugin.getPluginConfig();
        String holderId = page.holderId();
        String rankPlaceholder = rankPattern(definition).replace("%holder%", holderId);
        String valuePlaceholder = valuePattern(definition).replace("%holder%", holderId);
        String teamPlaceholder = definition.type() == LeaderboardType.GROUP
                ? config.provider().groupTeam() : "";
        return new ContentContext(rankPlaceholder, valuePlaceholder, teamPlaceholder,
                page.color(), page.icon());
    }

    private String renderContent(Player player, LeaderboardPage page, ContentContext context) {
        PluginConfig config = plugin.getPluginConfig();
        String rankPlaceholder = context.rankPlaceholder();
        String valuePlaceholder = context.valuePlaceholder();

        String resolvedRank = placeholderBridge.resolve(rankPlaceholder, player);
        boolean rankMissing = isMissing(resolvedRank, rankPlaceholder);
        String rankStr = rankMissing ? config.missingPosition() : resolvedRank;
        String valueStr = valuePlaceholder.isEmpty() ? config.missingText()
                : sanitize(placeholderBridge.resolve(valuePlaceholder, player), valuePlaceholder, config);

        if (!valueStr.equals(config.missingText())) {
            valueStr = page.valueFormat().format(valueStr);
            if (page.hasPrefix()) {
                valueStr = page.prefix() + valueStr;
            }
            if (page.hasSuffix()) {
                valueStr = valueStr + page.suffix();
            }
        }

        String playerName;
        if (definition.type() == LeaderboardType.GROUP) {
            String teamPlaceholder = context.teamPlaceholder();
            playerName = teamPlaceholder.isEmpty() ? player.getName()
                    : sanitize(placeholderBridge.resolve(teamPlaceholder, player), teamPlaceholder, config);
        } else {
            playerName = player.getName();
        }

        String positionStr = displayPosition(rankStr, rankMissing);

        return definition.formatting().renderRelative(
                context.color(), positionStr, rankStr, playerName, valueStr, context.icon());
    }

    private void updateContent(BoardDisplay display, UUID playerId, String rendered) {
        String previous = lastRenderedContent.get(playerId);
        if (Objects.equals(previous, rendered)) {
            return;
        }

        display.text(plugin.deserializeMiniMessage(rendered));
        lastRenderedContent.put(playerId, rendered);
    }

    private String sanitize(String value, String placeholder, PluginConfig config) {
        return isMissing(value, placeholder) ? config.missingText() : value;
    }

    private static boolean isMissing(String value, String placeholder) {
        return value == null || value.isBlank() || value.equalsIgnoreCase(placeholder) || value.contains("%");
    }

    static String displayPosition(String rank, boolean missing) {
        if (missing) {
            return rank;
        }
        try {
            Integer.parseInt(rank.trim());
            return "#" + rank.trim();
        } catch (NumberFormatException e) {
            return rank;
        }
    }

    private static byte packOpacity(int alpha) {
        int clamped = Math.max(OPACITY_TRANSPARENT, Math.min(OPACITY_OPAQUE, alpha));
        return (byte) clamped;
    }

    private static byte opacityAtFrame(int frame, int totalFrames, boolean fadeIn) {
        if (totalFrames <= 1) {
            return packOpacity(fadeIn ? OPACITY_OPAQUE : OPACITY_TRANSPARENT);
        }
        double progress = (double) frame / (double) (totalFrames - 1);
        double alpha = fadeIn ? progress : (1.0D - progress);
        return packOpacity((int) Math.round(alpha * OPACITY_OPAQUE));
    }

    private void removeDisplay(UUID uuid) {
        BoardDisplay display = playerDisplays.remove(uuid);
        lastRenderedContent.remove(uuid);
        if (display != null) {
            leaderboard.untrack(display);
            display.remove();
        }
    }

    private record ContentContext(String rankPlaceholder, String valuePlaceholder, String teamPlaceholder,
                                  String color, String icon) {
    }
}
