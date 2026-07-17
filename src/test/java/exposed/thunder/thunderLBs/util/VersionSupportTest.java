package exposed.thunder.thunderLBs.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VersionSupportTest {

    @Test
    void parsesThreePartVersions() {
        assertArrayEquals(new int[]{1, 21, 7}, VersionSupport.parse("1.21.7"));
        assertArrayEquals(new int[]{1, 19, 4}, VersionSupport.parse("1.19.4"));
    }

    @Test
    void parsesTwoPartVersions() {
        assertArrayEquals(new int[]{1, 21, 0}, VersionSupport.parse("1.21"));
    }

    @Test
    void parsesVersionsWithSuffix() {
        assertArrayEquals(new int[]{1, 20, 2}, VersionSupport.parse("1.20.2-pre1"));
    }

    @Test
    void unknownVersionsCountAsNewest() {
        assertTrue(VersionSupport.isAtLeast(VersionSupport.parse("garbage"), 1, 21, 7));
        assertTrue(VersionSupport.isAtLeast(VersionSupport.parse(null), 1, 20, 2));
    }

    @Test
    void comparesVersions() {
        assertTrue(VersionSupport.isAtLeast(VersionSupport.parse("1.20.2"), 1, 20, 2));
        assertFalse(VersionSupport.isAtLeast(VersionSupport.parse("1.20.1"), 1, 20, 2));
        assertFalse(VersionSupport.isAtLeast(VersionSupport.parse("1.19.4"), 1, 20, 2));
        assertTrue(VersionSupport.isAtLeast(VersionSupport.parse("1.21.8"), 1, 21, 7));
        assertFalse(VersionSupport.isAtLeast(VersionSupport.parse("1.21"), 1, 21, 7));
        assertTrue(VersionSupport.isAtLeast(VersionSupport.parse("2.0"), 1, 21, 7));
    }
}
