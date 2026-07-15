package exposed.thunder.thunderLBs.leaderboard;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LeaderboardFormattingTest {
    private final LeaderboardFormatting formatting = new LeaderboardFormatting(
            "<%color%>%icon% %title%",
            "%position% <%color%>%player%: %value% %icon%",
            "%position%/%rank% <%color%>%player%: %value% %icon%",
            "background",
            "foreground");

    @Test
    void rendersCompiledTemplates() {
        assertEquals("<#fff>* Kills", formatting.renderTitle("#fff", "*", "Kills"));
        assertEquals("1 <#fff>Alice: 42 *", formatting.renderRow("#fff", "1", "Alice", "42", "*"));
        assertEquals("#2/2 <#fff>Alice: 42 *",
                formatting.renderRelative("#fff", "#2", "2", "Alice", "42", "*"));
    }

    @Test
    void leavesUnknownPlaceholdersUntouched() {
        LeaderboardFormatting custom = new LeaderboardFormatting(
                "%title% %custom%", "%player%", "%player%", "", "");
        assertEquals("Title %custom%", custom.renderTitle("#fff", "", "Title"));
    }
}
