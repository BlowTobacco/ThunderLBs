package exposed.thunder.thunderLBs.leaderboard;

import java.util.Locale;

public enum BarMode {
    NONE,
    // ⏺⏺⏺
    DOTS,
    // ----------
    BAR;

    public static BarMode fromString(String value, BarMode fallback) {
        if (value == null || value.isEmpty()) {
            return fallback;
        }
        return switch (value.toLowerCase(Locale.ROOT)) {
            case "none" -> NONE;
            case "dots", "segmented" -> DOTS;
            case "bar", "single", "fill" -> BAR;
            default -> fallback;
        };
    }
}
