package exposed.thunder.thunderLBs.placeholder;

import exposed.thunder.thunderLBs.leaderboard.LeaderboardType;
import org.bukkit.OfflinePlayer;

import java.util.List;

interface NativeLeaderboardProvider extends AutoCloseable {
    boolean supports(String providerName, LeaderboardType leaderboardType);

    String resolve(Request request) throws ReflectiveOperationException;

    default List<String> holderChoices(String providerName, LeaderboardType leaderboardType)
            throws ReflectiveOperationException {
        return List.of();
    }

    default List<String> intervalChoices(String providerName, LeaderboardType leaderboardType) {
        return List.of();
    }

    boolean isAvailable();

    String name();

    @Override
    default void close() {
    }

    record Request(
            String providerName,
            LeaderboardType leaderboardType,
            String holder,
            String interval,
            int position,
            PlaceholderBridge.ProviderValue value,
            OfflinePlayer player) {
    }
}
