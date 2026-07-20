package exposed.thunder.thunderLBs.config;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConfigMigratorTest {
    @Test
    void upgradesAddNewFlagsAndRemoveStockNativeProviderPatterns() throws Exception {
        YamlConfiguration config = yaml("""
                config-version: 2
                missing-text: "CUSTOM"
                providers:
                  custom:
                    name: "%custom_name%"
                    value: "%custom_value%"
                messages:
                  prefix: "Custom prefix "
                  custom-message: "Keep me"
                """);
        YamlConfiguration providers = yaml("""
                ajleaderboards:
                  name: "%ajlb_lb_%holder%_%position%_alltime_name%"
                  value: "%ajlb_lb_%holder%_%position%_alltime_rawvalue%"
                  viewer-rank: "%ajlb_position_%holder%_alltime%"
                  viewer-value: "%ajlb_value_%holder%_alltime_raw%"
                """);
        YamlConfiguration messages = new YamlConfiguration();

        ConfigMigrator.migrateDocuments(
                config,
                providers,
                messages,
                resource("config.yml"),
                resource("providers.yml"),
                resource("messages.yml"),
                2);

        assertEquals(ConfigMigrator.CURRENT_VERSION, config.getInt("config-version"));
        assertEquals("CUSTOM", config.getString("missing-text"));
        assertEquals("-1", config.getString("missing-position"));
        assertEquals(40L, config.getLong("performance.relative-refresh-ticks"));
        assertTrue(config.isConfigurationSection("formatting.rank-colors"));
        assertTrue(config.getConfigurationSection("formatting.rank-colors").getKeys(false).isEmpty());
        assertFalse(config.isSet("providers"));
        assertFalse(config.isSet("messages"));

        assertFalse(providers.isSet("ajleaderboards"));
        assertFalse(providers.isSet("timedtopper"));
        assertEquals("%custom_name%", providers.getString("custom.name"));

        assertEquals("Custom prefix ", messages.getString("prefix"));
        assertEquals("Keep me", messages.getString("custom-message"));
        assertTrue(messages.isSet("editor-unsupported"));
    }

    @Test
    void customizedBuiltinPatternsBecomeAnExplicitPapiProvider() throws Exception {
        YamlConfiguration config = yaml("""
                config-version: 2
                provider: ajleaderboards
                """);
        YamlConfiguration providers = yaml("""
                ajleaderboards:
                  name: "%my_custom_pattern%"
                  value: "%ajlb_lb_%holder%_%position%_alltime_rawvalue%"
                """);

        ConfigMigrator.migrateDocuments(
                config,
                providers,
                new YamlConfiguration(),
                resource("config.yml"),
                resource("providers.yml"),
                resource("messages.yml"),
                2);

        assertFalse(providers.isSet("ajleaderboards"));
        assertEquals("ajleaderboards-papi", config.getString("provider"));
        assertEquals("%my_custom_pattern%", providers.getString("ajleaderboards-papi.name"));
        assertEquals("%ajlb_lb_%holder%_%position%_alltime_rawvalue%",
                providers.getString("ajleaderboards-papi.value"));
    }

    @Test
    void legacyConfigurationKeepsValuesWhileRenamingFlags() throws Exception {
        YamlConfiguration config = yaml("""
                defaults:
                  progress-offset-y: -0.75
                placeholders:
                  name: "%legacy_name%"
                  value: "%legacy_value%"
                  missing-name: "NONE"
                group-placeholders:
                  name: "%legacy_group_name%"
                  value: "%legacy_group_value%"
                  team: "%legacy_team%"
                formatting:
                  progress:
                    background: "legacy background"
                    foreground: "legacy foreground"
                progress-bar:
                  mode: segmented
                  use-holder-color: false
                messages:
                  prefix: "Legacy prefix "
                """);
        YamlConfiguration providers = new YamlConfiguration();
        YamlConfiguration messages = new YamlConfiguration();

        ConfigMigrator.migrateDocuments(
                config,
                providers,
                messages,
                resource("config.yml"),
                resource("providers.yml"),
                resource("messages.yml"),
                1);

        assertEquals(-0.75D, config.getDouble("defaults.bar-offset-y"));
        assertEquals("dots", config.getString("bar.mode"));
        assertFalse(config.getBoolean("bar.use-holder-color"));
        assertEquals("legacy background", config.getString("bar.background"));
        assertEquals("legacy foreground", config.getString("bar.foreground"));
        assertEquals("NONE", config.getString("missing-text"));
        assertFalse(config.isSet("placeholders"));
        assertFalse(config.isSet("progress-bar"));
        assertFalse(providers.isSet("topper"));
        assertEquals("topper-papi", config.getString("provider"));
        assertEquals("%legacy_name%", providers.getString("topper-papi.name"));
        assertEquals("%legacy_group_value%", providers.getString("topper-papi.group-value"));
        assertEquals("%legacy_team%", providers.getString("topper-papi.group-team"));
        assertEquals("Legacy prefix ", messages.getString("prefix"));
    }

    @Test
    void versionFourLeavesOnlyCustomProviderSections() throws Exception {
        YamlConfiguration config = yaml("""
                config-version: 3
                provider: topper
                """);
        YamlConfiguration providers = yaml("""
                topper:
                  name: "%topper_%holder%;top_name;%position%%"
                  value: "%topper_%holder%;top_value_raw;%position%%"
                  viewer-rank: "%topper_%holder%;top_rank%"
                  viewer-value: "%topper_%holder%;value_raw%"
                timedtopper:
                  name: "%timedtopper_%holder%;top_name;%position%%"
                  value: "%timedtopper_%holder%;top_value_raw;%position%%"
                  viewer-rank: "%timedtopper_%holder%;top_rank%"
                  viewer-value: "%timedtopper_%holder%;value_raw%"
                custom:
                  name: "%custom_name%"
                  value: "%custom_value%"
                """);

        ConfigMigrator.migrateDocuments(
                config,
                providers,
                new YamlConfiguration(),
                resource("config.yml"),
                resource("providers.yml"),
                resource("messages.yml"),
                3);

        assertEquals(4, config.getInt("config-version"));
        assertEquals("topper", config.getString("provider"));
        assertFalse(providers.isSet("topper"));
        assertFalse(providers.isSet("timedtopper"));
        assertEquals("%custom_name%", providers.getString("custom.name"));
    }

    @Test
    void existingLeaderboardPagesReceiveOnlyMissingIntervals() throws Exception {
        YamlConfiguration leaderboard = yaml("""
                pages:
                  page-1:
                    holder: kills
                  page-2:
                    holder: deaths
                    interval: weekly
                """);

        assertTrue(ConfigMigrator.migrateLeaderboardConfiguration(leaderboard));
        assertEquals("alltime", leaderboard.getString("pages.page-1.interval"));
        assertEquals("weekly", leaderboard.getString("pages.page-2.interval"));
        assertFalse(ConfigMigrator.migrateLeaderboardConfiguration(leaderboard));
    }

    private static YamlConfiguration yaml(String input) throws Exception {
        YamlConfiguration configuration = new YamlConfiguration();
        configuration.loadFromString(input);
        return configuration;
    }

    private static YamlConfiguration resource(String name) throws Exception {
        InputStream input = ConfigMigratorTest.class.getClassLoader().getResourceAsStream(name);
        if (input == null) {
            throw new IOException("Missing test resource " + name);
        }
        try (InputStreamReader reader = new InputStreamReader(input, StandardCharsets.UTF_8)) {
            YamlConfiguration configuration = new YamlConfiguration();
            configuration.options().parseComments(true);
            configuration.load(reader);
            return configuration;
        }
    }
}
