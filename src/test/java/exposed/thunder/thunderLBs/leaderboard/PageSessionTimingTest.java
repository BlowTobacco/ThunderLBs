package exposed.thunder.thunderLBs.leaderboard;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PageSessionTimingTest {
    @Test
    void progressCycleIncludesEntranceBeforeVisiblePageDuration() {
        assertEquals(45L, PageSession.progressCycleTicks(35L, 10));
    }

    @Test
    void interpolationMatchesSpacingBetweenProgressFrames() {
        assertEquals(5, PageSession.progressInterpolationTicks(45L, 10));
    }

    @Test
    void progressInterpolationNeverDropsBelowOneTick() {
        assertEquals(1, PageSession.progressInterpolationTicks(1L, 20));
    }

    @Test
    void barFadeEndsAtPageSwitch() {
        assertEquals(5L, PageSession.barFadeDelayTicks(60L, 45L, 10));
        assertEquals(5L, PageSession.barFadeDelayTicks(100L, 85L, 10));
        assertEquals(5L, PageSession.barFadeDelayTicks(160L, 145L, 10));
    }

    @Test
    void barFadeDelayNeverDropsBelowOneTick() {
        assertEquals(1L, PageSession.barFadeDelayTicks(10L, 10L, 10));
    }
}
