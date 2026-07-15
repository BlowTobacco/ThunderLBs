package exposed.thunder.thunderLBs.leaderboard;

public record ValidationIssue(Severity severity, String path, String message) {
    public enum Severity {
        ERROR,
        WARNING
    }
}
