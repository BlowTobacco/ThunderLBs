package exposed.thunder.thunderLBs.leaderboard;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LeaderboardFilesTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void acceptsAndNormalizesSafeIds() {
        assertTrue(LeaderboardFiles.isValidId("spawn-kills_2"));
        assertTrue(LeaderboardFiles.isValidId("Spawn-Kills"));
        assertEquals("spawn-kills", LeaderboardFiles.normalizeId(" Spawn-Kills "));
    }

    @Test
    void rejectsTraversalAndUnsafeIds() {
        assertFalse(LeaderboardFiles.isValidId("../config"));
        assertFalse(LeaderboardFiles.isValidId("folder/board"));
        assertFalse(LeaderboardFiles.isValidId("folder\\board"));
        assertFalse(LeaderboardFiles.isValidId("has spaces"));
        assertFalse(LeaderboardFiles.isValidId(""));
        assertThrows(IllegalArgumentException.class,
                () -> LeaderboardFiles.configFile(temporaryDirectory.toFile(), "../config"));
    }

    @Test
    void atomicSaveReplacesCompleteYaml() throws Exception {
        File target = LeaderboardFiles.configFile(temporaryDirectory.toFile(), "test");
        YamlConfiguration first = new YamlConfiguration();
        first.set("value", "before");
        LeaderboardFiles.atomicSave(first, target);

        YamlConfiguration second = new YamlConfiguration();
        second.set("value", "after");
        second.set("complete", true);
        LeaderboardFiles.atomicSave(second, target);

        YamlConfiguration loaded = YamlConfiguration.loadConfiguration(target);
        assertEquals("after", loaded.getString("value"));
        assertTrue(loaded.getBoolean("complete"));
        assertEquals(1, temporaryDirectory.toFile().listFiles((dir, name) -> name.endsWith(".yml")).length);
    }
}
