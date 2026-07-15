package exposed.thunder.thunderLBs.util;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.util.NumberConversions;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

public final class FormattingUtil {
    private static final char[] SUFFIXES = { 'K', 'M', 'B', 'T', 'P', 'E' };
    private static final DecimalFormatSymbols SYMBOLS = DecimalFormatSymbols.getInstance(Locale.US);
    private static final ThreadLocal<DecimalFormat> DECIMAL_FORMAT = ThreadLocal.withInitial(() -> {
        DecimalFormat format = new DecimalFormat();
        format.setDecimalFormatSymbols(SYMBOLS);
        format.setMinimumFractionDigits(0);
        format.setGroupingUsed(false);
        return format;
    });

    private FormattingUtil() {
    }

    public static String shortNumber(double value, int decimals) {
        double absValue = Math.abs(value);
        if (!Double.isFinite(value) || absValue < 1000.0D) {
            return trimDecimals(value, decimals);
        }
        int exp = 0;
        double scaled = value;
        while (absValue >= 1000.0D && exp < SUFFIXES.length) {
            absValue /= 1000.0D;
            scaled /= 1000.0D;
            exp++;
        }
        char suffix = SUFFIXES[exp - 1];
        return trimDecimals(scaled, decimals) + suffix;
    }

    private static String trimDecimals(double value, int decimals) {
        if (decimals <= 0) {
            return String.valueOf(Math.round(value));
        }
        DecimalFormat format = DECIMAL_FORMAT.get();
        format.setMaximumFractionDigits(decimals);
        return format.format(value);
    }

    public static String formatLocation(Location location) {
        if (location == null) {
            return "unknown";
        }
        World world = location.getWorld();
        String worldName = world != null ? world.getName() : "null";
        return worldName
                + " "
                + NumberConversions.round(location.getX())
                + ","
                + NumberConversions.round(location.getY())
                + ","
                + NumberConversions.round(location.getZ());
    }

    public static String formatTimespan(long millis) {
        if (millis < 1000) {
            return millis + "ms";
        }
        long seconds = millis / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;

        if (days > 0) {
            long h = hours % 24;
            return days + "d" + (h > 0 ? " " + h + "h" : "");
        }
        if (hours > 0) {
            long m = minutes % 60;
            return hours + "h" + (m > 0 ? " " + m + "m" : "");
        }
        if (minutes > 0) {
            long s = seconds % 60;
            return minutes + "m" + (s > 0 ? " " + s + "s" : "");
        }
        return seconds + "s";
    }
}
