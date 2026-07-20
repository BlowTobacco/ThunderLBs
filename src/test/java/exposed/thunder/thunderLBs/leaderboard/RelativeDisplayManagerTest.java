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

    @Test
    void lateViewerLinearStateUsesTheCurrentPointInTheTransition() {
        assertEquals(-0.625F, RelativeDisplayManager.interpolateLinear(-1.25F, 0.0F, 10L, 20));
        assertEquals(128, Byte.toUnsignedInt(
                RelativeDisplayManager.interpolateOpacity((byte) 0, (byte) 255, 10L, 20)));
        assertEquals(10, RelativeDisplayManager.remainingLinearTicks(10L, 20));
    }

    @Test
    void lateViewerLinearStateClampsAtBothEndpoints() {
        assertEquals(-1.25F, RelativeDisplayManager.interpolateLinear(-1.25F, 0.0F, -5L, 20));
        assertEquals(0.0F, RelativeDisplayManager.interpolateLinear(-1.25F, 0.0F, 25L, 20));
        assertEquals(0, RelativeDisplayManager.remainingLinearTicks(25L, 20));
    }
}
