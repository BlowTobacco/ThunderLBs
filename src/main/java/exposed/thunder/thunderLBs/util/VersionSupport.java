package exposed.thunder.thunderLBs.util;

import org.bukkit.Bukkit;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class VersionSupport {
    private static final Pattern VERSION_PATTERN = Pattern.compile("(\\d+)\\.(\\d+)(?:\\.(\\d+))?");

    private static volatile int[] version;

    private VersionSupport() {
    }

    public static boolean teleportDurationSupported() {
        return isAtLeast(1, 20, 2);
    }

    public static boolean dialogsSupported() {
        return isAtLeast(1, 21, 7) && hasClass("io.papermc.paper.dialog.Dialog");
    }

    public static boolean foliaSchedulersAvailable() {
        return hasClass("io.papermc.paper.threadedregions.scheduler.RegionScheduler");
    }

    public static boolean isAtLeast(int major, int minor, int patch) {
        return isAtLeast(minecraftVersion(), major, minor, patch);
    }

    static boolean isAtLeast(int[] current, int major, int minor, int patch) {
        if (current[0] != major) {
            return current[0] > major;
        }
        if (current[1] != minor) {
            return current[1] > minor;
        }
        return current[2] >= patch;
    }

    private static int[] minecraftVersion() {
        int[] current = version;
        if (current == null) {
            current = parse(Bukkit.getMinecraftVersion());
            version = current;
        }
        return current;
    }

    static int[] parse(String minecraftVersion) {
        if (minecraftVersion != null) {
            Matcher matcher = VERSION_PATTERN.matcher(minecraftVersion);
            if (matcher.find()) {
                return new int[]{
                        Integer.parseInt(matcher.group(1)),
                        Integer.parseInt(matcher.group(2)),
                        matcher.group(3) == null ? 0 : Integer.parseInt(matcher.group(3))
                };
            }
        }
        return new int[]{Integer.MAX_VALUE, 0, 0};
    }

    private static boolean hasClass(String name) {
        try {
            Class.forName(name, false, VersionSupport.class.getClassLoader());
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
}
