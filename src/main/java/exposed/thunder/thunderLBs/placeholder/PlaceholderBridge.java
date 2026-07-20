package exposed.thunder.thunderLBs.placeholder;

import exposed.thunder.thunderLBs.ThunderLBs;
import exposed.thunder.thunderLBs.leaderboard.LeaderboardType;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import java.util.List;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class PlaceholderBridge implements AutoCloseable {
    private final ThunderLBs plugin;
    private final boolean placeholderApiAvailable;
    private final List<NativeLeaderboardProvider> nativeProviders;
    private final Set<String> warnedNativeResolvers = ConcurrentHashMap.newKeySet();

    public PlaceholderBridge(ThunderLBs plugin) {
        this.plugin = plugin;
        this.placeholderApiAvailable = Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI");
        this.nativeProviders = List.of(new TopperQueryBridge(plugin), new AjLeaderboardsBridge(plugin));
        if (!placeholderApiAvailable) {
            plugin.getLogger().warning("PlaceholderAPI was not detected. Native leaderboard providers will still "
                    + "work, but custom provider placeholders will not be resolved.");
        }
    }

    public boolean isAvailable() {
        return placeholderApiAvailable
                || nativeProviders.stream().anyMatch(NativeLeaderboardProvider::isAvailable);
    }

    public boolean supportsProvider(String providerName, LeaderboardType leaderboardType) {
        return nativeProviders.stream().anyMatch(provider -> provider.supports(providerName, leaderboardType));
    }

    public boolean isNativeProvider(String providerName) {
        return supportsProvider(providerName, LeaderboardType.PLAYER)
                || supportsProvider(providerName, LeaderboardType.GROUP);
    }

    public List<String> holderChoices(String providerName, LeaderboardType leaderboardType) {
        return nativeChoices(providerName, leaderboardType, true);
    }

    public List<String> intervalChoices(String providerName, LeaderboardType leaderboardType) {
        return nativeChoices(providerName, leaderboardType, false);
    }

    public String resolveProvider(
            String providerName,
            LeaderboardType leaderboardType,
            String holder,
            String interval,
            int position,
            ProviderValue value,
            OfflinePlayer player,
            String customPlaceholder) {
        NativeLeaderboardProvider.Request request = new NativeLeaderboardProvider.Request(
                providerName,
                leaderboardType,
                holder,
                interval,
                position,
                value,
                player);
        for (NativeLeaderboardProvider provider : nativeProviders) {
            if (!provider.supports(providerName, leaderboardType)) {
                continue;
            }
            try {
                String resolved = provider.resolve(request);
                return resolved == null ? "" : resolved;
            } catch (ReflectiveOperationException | LinkageError | RuntimeException exception) {
                if (warnedNativeResolvers.add(provider.name())) {
                    plugin.getLogger().warning("Failed to resolve a native " + provider.name()
                            + " query; further failures from this integration will be suppressed: "
                            + rootMessage(exception));
                }
                return "";
            }
        }
        return resolve(customPlaceholder, player);
    }

    public String resolve(String placeholder, OfflinePlayer player) {
        String normalizedPlaceholder = placeholder == null ? "" : placeholder;
        if (!placeholderApiAvailable) {
            return normalizedPlaceholder;
        }
        try {
            return PlaceholderAPI.setPlaceholders(player, normalizedPlaceholder);
        } catch (Throwable throwable) {
            plugin.getLogger().warning("Failed to resolve placeholder '" + normalizedPlaceholder + "': "
                    + throwable.getMessage());
            return normalizedPlaceholder;
        }
    }

    public String resolve(String placeholder) {
        return resolve(placeholder, null);
    }

    private List<String> nativeChoices(
            String providerName,
            LeaderboardType leaderboardType,
            boolean holders) {
        for (NativeLeaderboardProvider provider : nativeProviders) {
            if (!provider.supports(providerName, leaderboardType)) {
                continue;
            }
            try {
                List<String> choices = holders
                        ? provider.holderChoices(providerName, leaderboardType)
                        : provider.intervalChoices(providerName, leaderboardType);
                return normalizeChoices(choices);
            } catch (ReflectiveOperationException | LinkageError | RuntimeException exception) {
                String warningKey = provider.name() + (holders ? "-holders" : "-intervals");
                if (warnedNativeResolvers.add(warningKey)) {
                    plugin.getLogger().warning("Failed to read " + (holders ? "holder" : "interval")
                            + " choices from " + provider.name() + ": " + rootMessage(exception));
                }
                return List.of();
            }
        }
        return List.of();
    }

    static List<String> normalizeChoices(List<String> choices) {
        if (choices == null || choices.isEmpty()) {
            return List.of();
        }
        Map<String, String> unique = new LinkedHashMap<>();
        for (String choice : choices) {
            if (choice == null || choice.isBlank()) {
                continue;
            }
            String trimmed = choice.trim();
            unique.putIfAbsent(trimmed.toLowerCase(Locale.ROOT), trimmed);
        }
        return unique.values().stream()
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .toList();
    }

    @Override
    public void close() {
        for (NativeLeaderboardProvider provider : nativeProviders) {
            provider.close();
        }
        warnedNativeResolvers.clear();
    }

    public enum ProviderValue {
        TOP_NAME,
        TOP_VALUE,
        VIEWER_RANK,
        VIEWER_VALUE
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
