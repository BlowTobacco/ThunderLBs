package exposed.thunder.thunderLBs.placeholder;

import exposed.thunder.thunderLBs.ThunderLBs;
import exposed.thunder.thunderLBs.leaderboard.LeaderboardType;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

final class AjLeaderboardsBridge implements NativeLeaderboardProvider {
    private static final List<String> INTERVALS =
            List.of("alltime", "hourly", "daily", "weekly", "monthly", "yearly");

    private final ThunderLBs plugin;
    private Object topManager;
    private Method timedTypeOf;
    private Method getStat;
    private Method getStatEntry;
    private Method getBoards;
    private Method hasPlayer;
    private Method getPlayerName;
    private Method getPosition;
    private Method getScore;
    private boolean available;

    AjLeaderboardsBridge(ThunderLBs plugin) {
        this.plugin = plugin;
        attach();
    }

    @Override
    public boolean supports(String providerName, LeaderboardType leaderboardType) {
        return supportsProvider(providerName, leaderboardType);
    }

    @Override
    public String resolve(Request request) throws ReflectiveOperationException {
        boolean viewerRequest = request.value() == PlaceholderBridge.ProviderValue.VIEWER_RANK
                || request.value() == PlaceholderBridge.ProviderValue.VIEWER_VALUE;
        if (!available || (viewerRequest && request.player() == null)) {
            return null;
        }
        if (!viewerRequest && request.position() < 1) {
            return null;
        }

        String interval = request.interval() == null || request.interval().isBlank()
                ? "alltime"
                : request.interval();
        Object timedType = timedTypeOf.invoke(null, interval);
        if (timedType == null) {
            return null;
        }

        Object entry = viewerRequest
                ? getStatEntry.invoke(topManager, request.player(), request.holder(), timedType)
                : getStat.invoke(topManager, request.position(), request.holder(), timedType);
        if (entry == null || !Boolean.TRUE.equals(hasPlayer.invoke(entry))) {
            return null;
        }

        return switch (request.value()) {
            case TOP_NAME -> stringValue(getPlayerName.invoke(entry));
            case TOP_VALUE, VIEWER_VALUE -> rawScore(getScore.invoke(entry));
            case VIEWER_RANK -> stringValue(getPosition.invoke(entry));
        };
    }

    @Override
    public List<String> holderChoices(String providerName, LeaderboardType leaderboardType)
            throws ReflectiveOperationException {
        if (!available || !supports(providerName, leaderboardType)) {
            return List.of();
        }
        Object boards = getBoards.invoke(topManager);
        if (!(boards instanceof Collection<?> collection)) {
            return List.of();
        }
        return collection.stream()
                .filter(String.class::isInstance)
                .map(String.class::cast)
                .toList();
    }

    @Override
    public List<String> intervalChoices(String providerName, LeaderboardType leaderboardType) {
        return supports(providerName, leaderboardType) ? INTERVALS : List.of();
    }

    @Override
    public boolean isAvailable() {
        return available;
    }

    @Override
    public String name() {
        return "ajLeaderboards";
    }

    private void attach() {
        Plugin ajLeaderboards = plugin.getServer().getPluginManager().getPlugin("ajLeaderboards");
        if (ajLeaderboards == null || !ajLeaderboards.isEnabled()) {
            return;
        }

        try {
            ClassLoader classLoader = ajLeaderboards.getClass().getClassLoader();
            Class<?> timedTypeClass = Class.forName(
                    "us.ajg0702.leaderboards.boards.TimedType", false, classLoader);
            Class<?> statEntryClass = Class.forName(
                    "us.ajg0702.leaderboards.boards.StatEntry", false, classLoader);

            this.topManager = ajLeaderboards.getClass().getMethod("getTopManager").invoke(ajLeaderboards);
            if (topManager == null) {
                throw new ReflectiveOperationException("ajLeaderboards returned a null TopManager");
            }
            Class<?> managerClass = topManager.getClass();
            this.timedTypeOf = timedTypeClass.getMethod("of", String.class);
            this.getStat = managerClass.getMethod(
                    "getStat", int.class, String.class, timedTypeClass);
            this.getStatEntry = managerClass.getMethod(
                    "getStatEntry", OfflinePlayer.class, String.class, timedTypeClass);
            this.getBoards = managerClass.getMethod("getBoards");
            this.hasPlayer = statEntryClass.getMethod("hasPlayer");
            this.getPlayerName = statEntryClass.getMethod("getPlayerName");
            this.getPosition = statEntryClass.getMethod("getPosition");
            this.getScore = statEntryClass.getMethod("getScore");
            this.available = true;
            plugin.getLogger().info("Hooked ajLeaderboards' native TopManager; PlaceholderAPI is not used for "
                    + "stock ajLeaderboards provider queries.");
        } catch (ReflectiveOperationException | LinkageError | RuntimeException exception) {
            clear();
            plugin.getLogger().warning("ajLeaderboards is installed, but its native TopManager could not be hooked: "
                    + rootMessage(exception));
        }
    }

    private void clear() {
        available = false;
        topManager = null;
        timedTypeOf = null;
        getStat = null;
        getStatEntry = null;
        getBoards = null;
        hasPlayer = null;
        getPlayerName = null;
        getPosition = null;
        getScore = null;
    }

    static boolean supportsProvider(String providerName, LeaderboardType leaderboardType) {
        return providerName != null
                && providerName.equalsIgnoreCase("ajleaderboards")
                && leaderboardType == LeaderboardType.PLAYER;
    }

    static String rawScore(Object value) {
        if (!(value instanceof Number number)) {
            return stringValue(value);
        }
        double score = number.doubleValue();
        if (!Double.isFinite(score)) {
            return Double.toString(score);
        }
        if (score == 0.0D) {
            return "0";
        }
        return new DecimalFormat("#.##", DecimalFormatSymbols.getInstance(Locale.ROOT)).format(score);
    }

    private static String stringValue(Object value) {
        return value == null ? null : value.toString();
    }

    private static String rootMessage(Throwable throwable) {
        Throwable current = throwable;
        while (current.getCause() != null) {
            current = current.getCause();
        }
        String message = current.getMessage();
        return message == null || message.isBlank() ? current.getClass().getSimpleName() : message;
    }
}
