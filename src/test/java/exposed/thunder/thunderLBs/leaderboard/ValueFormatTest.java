package exposed.thunder.thunderLBs.leaderboard;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ValueFormatTest {
    @Test
    void formatsCommonNumericValues() {
        assertEquals("1.53K", ValueFormat.SHORT_NUMBER.format("1532"));
        assertEquals("12h", ValueFormat.HOURS_SUFFIX.format("12.9"));
        assertEquals("1m 1s", ValueFormat.TIMESPAN_SECONDS.format("61"));
        assertEquals("1s", ValueFormat.TIME_TICKS.format("20"));
    }

    @Test
    void preservesValuesItCannotParse() {
        assertEquals("---", ValueFormat.SHORT_NUMBER.format("---"));
        assertEquals("player-value", ValueFormat.TIMESPAN.format("player-value"));
        assertEquals("NaN", ValueFormat.SHORT_NUMBER.format("NaN"));
    }
}
