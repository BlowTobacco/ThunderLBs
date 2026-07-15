package exposed.thunder.thunderLBs.leaderboard;

import org.bukkit.configuration.ConfigurationSection;

public final class LeaderboardPage {
    private final String holderId;
    private final String title;
    private final String color;
    private final String icon;
    private final ValueFormat valueFormat;
    private final String prefix;
    private final String suffix;

    public LeaderboardPage(String holderId, String title, String color, String icon, ValueFormat valueFormat,
            String prefix, String suffix) {
        this.holderId = holderId;
        this.title = title;
        this.color = color;
        this.icon = icon;
        this.valueFormat = valueFormat;
        this.prefix = sanitizeSuffix(prefix);
        this.suffix = sanitizeSuffix(suffix);
    }

    private String sanitizeSuffix(String suffix) {
        if (suffix == null) {
            return "";
        }
        String trimmed = suffix.trim();
        if (trimmed.equalsIgnoreCase("none")) {
            return "";
        }
        return trimmed;
    }

    public String holderId() {
        return holderId;
    }

    public String title() {
        return title;
    }

    public String color() {
        return color;
    }

    public String icon() {
        return icon;
    }

    public ValueFormat valueFormat() {
        return valueFormat;
    }

    public String prefix() {
        return prefix;
    }

    public String suffix() {
        return suffix;
    }

    public boolean hasSuffix() {
        return !suffix.isEmpty();
    }

    public boolean hasPrefix() {
        return !prefix.isEmpty();
    }

    public static LeaderboardPage from(ConfigurationSection section) {
        if (section == null) {
            return new LeaderboardPage("kills", "MOST KILLS", "#FF2929", "*", ValueFormat.SHORT_NUMBER, "", "");
        }
        String holder = section.getString("holder", "kills");
        String title = section.getString("title", "MOST KILLS");
        String color = section.getString("color", "#FF2929");
        String icon = section.getString("icon", "*");
        ValueFormat format = ValueFormat.fromConfig(section.getString("value-format"), ValueFormat.SHORT_NUMBER);
        String prefix = section.getString("prefix", "");
        String suffix = section.getString("suffix", "");
        return new LeaderboardPage(holder, title, color, icon, format, prefix, suffix);
    }

    public void serialize(ConfigurationSection section) {
        section.set("holder", holderId);
        section.set("title", title);
        section.set("color", color);
        section.set("icon", icon);
        section.set("value-format", valueFormat.name());
        section.set("prefix", prefix);
        section.set("suffix", suffix);
    }

    @Override
    public String toString() {
        return holderId + " (" + title + ")";
    }
}
