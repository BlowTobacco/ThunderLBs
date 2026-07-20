package exposed.thunder.thunderLBs.config;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PluginConfigPerformanceTest {
    @Test
    void relativeRefreshUsesPerformanceOrientedDefault() {
        assertEquals(40L, PluginConfig.Performance.from(null).relativeRefreshTicks());
    }

    @Test
    void relativeRefreshIsClampedToSafeSchedulerValues() {
        assertEquals(1L, parseRefreshTicks(0L));
        assertEquals(72_000L, parseRefreshTicks(Long.MAX_VALUE));
        assertEquals(100L, parseRefreshTicks(100L));
    }

    private static long parseRefreshTicks(long value) {
        YamlConfiguration configuration = new YamlConfiguration();
        ConfigurationSection section = configuration.createSection("performance");
        section.set("relative-refresh-ticks", value);
        return PluginConfig.Performance.from(section).relativeRefreshTicks();
    }
}
