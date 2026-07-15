package exposed.thunder.thunderLBs.leaderboard;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RelativeDisplayManagerTest {
    @Test
    void missingPositionIsDisplayedLiterally() {
        assertEquals("-1", RelativeDisplayManager.displayPosition("-1", true));
    }

    @Test
    void resolvedNumericPositionKeepsRankPrefix() {
        assertEquals("#12", RelativeDisplayManager.displayPosition("12", false));
    }

    @Test
    void providerSpecificPositionTextIsPreserved() {
        assertEquals("unranked", RelativeDisplayManager.displayPosition("unranked", false));
    }

}
