package exposed.thunder.thunderLBs.placeholder;

import exposed.thunder.thunderLBs.leaderboard.LeaderboardType;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NativeProviderRequestTest {
    @Test
    void topperSelectsItsContextFromProviderAndLeaderboardType() {
        assertEquals("topper", TopperQueryBridge.contextName("topper", LeaderboardType.PLAYER));
        assertEquals("grouptopper", TopperQueryBridge.contextName("topper", LeaderboardType.GROUP));
        assertEquals("timedtopper", TopperQueryBridge.contextName("timedtopper", LeaderboardType.PLAYER));
        assertNull(TopperQueryBridge.contextName("timedtopper", LeaderboardType.GROUP));
        assertNull(TopperQueryBridge.contextName("custom", LeaderboardType.PLAYER));
    }

    @Test
    void topperBuildsNativeQueriesWithoutPlaceholderSyntax() {
        assertEquals(
                "money;top_name;12",
                TopperQueryBridge.queryString(request(
                        "topper", LeaderboardType.GROUP, "money", 12,
                        PlaceholderBridge.ProviderValue.TOP_NAME)));
        assertEquals(
                "jump_daily;top_value_raw;3",
                TopperQueryBridge.queryString(request(
                        "timedtopper", LeaderboardType.PLAYER, "jump_daily", 3,
                        PlaceholderBridge.ProviderValue.TOP_VALUE)));
        assertEquals(
                "money;top_rank",
                TopperQueryBridge.queryString(request(
                        "topper", LeaderboardType.PLAYER, "money", 0,
                        PlaceholderBridge.ProviderValue.VIEWER_RANK)));
        assertEquals(
                "money;value_raw",
                TopperQueryBridge.queryString(request(
                        "topper", LeaderboardType.PLAYER, "money", 0,
                        PlaceholderBridge.ProviderValue.VIEWER_VALUE)));
    }

    @Test
    void invalidTopPositionsDoNotProduceQueries() {
        assertNull(TopperQueryBridge.queryString(request(
                "topper", LeaderboardType.PLAYER, "money", 0,
                PlaceholderBridge.ProviderValue.TOP_NAME)));
    }

    @Test
    void ajSupportsOnlyItsPlayerProvider() {
        assertTrue(AjLeaderboardsBridge.supportsProvider("ajleaderboards", LeaderboardType.PLAYER));
        assertFalse(AjLeaderboardsBridge.supportsProvider("ajleaderboards", LeaderboardType.GROUP));
        assertFalse(AjLeaderboardsBridge.supportsProvider("custom", LeaderboardType.PLAYER));
    }

    @Test
    void ajRawScoresMatchTheStockPlaceholderPrecision() {
        assertEquals("1234.57", AjLeaderboardsBridge.rawScore(1234.567D));
        assertEquals("10", AjLeaderboardsBridge.rawScore(10.0D));
        assertEquals("0", AjLeaderboardsBridge.rawScore(-0.0D));
    }

    @Test
    void nativeDialogChoicesAreTrimmedDeduplicatedAndSorted() {
        assertEquals(
                List.of("ALLTIME", "daily", "weekly"),
                PlaceholderBridge.normalizeChoices(Arrays.asList(
                        " weekly ", "daily", null, "", "ALLTIME", "alltime")));
    }

    private static NativeLeaderboardProvider.Request request(
            String provider,
            LeaderboardType type,
            String holder,
            int position,
            PlaceholderBridge.ProviderValue value) {
        return new NativeLeaderboardProvider.Request(
                provider,
                type,
                holder,
                "alltime",
                position,
                value,
                null);
    }
}
