package exposed.thunder.thunderLBs.leaderboard;

public enum LeaderboardType {
    PLAYER,
    GROUP;

    public static LeaderboardType fromString(String value, LeaderboardType def) {
        if (value == null || value.isEmpty()) {
            return def;
        }
        try {
            return valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            return def;
        }
    }
}
