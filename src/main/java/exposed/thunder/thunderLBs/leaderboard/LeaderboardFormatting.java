package exposed.thunder.thunderLBs.leaderboard;

import exposed.thunder.thunderLBs.config.PluginConfig;
import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.List;

public final class LeaderboardFormatting {
    private final String titlePattern;
    private final String rowPattern;
    private final String relativePattern;
    private final String barBackground;
    private final String barForeground;
    private final Template titleTemplate;
    private final Template rowTemplate;
    private final Template relativeTemplate;

    public LeaderboardFormatting(String titlePattern,
            String rowPattern,
            String relativePattern,
            String barBackground,
            String barForeground) {
        this.titlePattern = titlePattern;
        this.rowPattern = rowPattern;
        this.relativePattern = relativePattern;
        this.barBackground = barBackground;
        this.barForeground = barForeground;
        this.titleTemplate = new Template(titlePattern, "%color%", "%icon%", "%title%");
        this.rowTemplate = new Template(rowPattern, "%color%", "%position%", "%player%", "%value%", "%icon%");
        this.relativeTemplate = new Template(relativePattern,
                "%color%", "%position%", "%rank%", "%player%", "%value%", "%icon%");
    }

    public static LeaderboardFormatting from(ConfigurationSection section, PluginConfig config) {
        PluginConfig.Formatting defaults = config.formatting();
        PluginConfig.Bar barDefaults = config.bar();
        if (section == null) {
            return new LeaderboardFormatting(
                    defaults.title(),
                    defaults.row(),
                    defaults.relative(),
                    barDefaults.background(),
                    barDefaults.foreground());
        }
        ConfigurationSection bar = section.getConfigurationSection("bar");
        if (bar == null) {
            bar = section.getConfigurationSection("progress");
        }
        return new LeaderboardFormatting(
                section.getString("title", defaults.title()),
                section.getString("row", defaults.row()),
                section.getString("relative", defaults.relative()),
                bar != null ? bar.getString("background", barDefaults.background()) : barDefaults.background(),
                bar != null ? bar.getString("foreground", barDefaults.foreground()) : barDefaults.foreground());
    }

    public void serialize(ConfigurationSection section) {
        section.set("title", titlePattern);
        section.set("row", rowPattern);
        section.set("relative", relativePattern);
        ConfigurationSection bar = section.createSection("bar");
        bar.set("background", barBackground);
        bar.set("foreground", barForeground);
    }

    public String titlePattern() {
        return titlePattern;
    }

    public String rowPattern() {
        return rowPattern;
    }

    public String relativePattern() {
        return relativePattern;
    }

    public String barBackground() {
        return barBackground;
    }

    public String barForeground() {
        return barForeground;
    }

    public String renderTitle(String color, String icon, String title) {
        return titleTemplate.render3(color, icon, title);
    }

    public String renderRow(String color, String position, String player, String value, String icon) {
        return rowTemplate.render5(color, position, player, value, icon);
    }

    public String renderRelative(String color, String position, String rank, String player, String value,
                                 String icon) {
        return relativeTemplate.render6(color, position, rank, player, value, icon);
    }

    private static final class Template {
        private final String[] literals;
        private final int[] tokenIndexes;
        private final int baseLength;

        private Template(String pattern, String... tokens) {
            List<String> literalParts = new ArrayList<>();
            List<Integer> indexes = new ArrayList<>();
            int cursor = 0;
            while (cursor < pattern.length()) {
                int next = -1;
                int tokenIndex = -1;
                for (int i = 0; i < tokens.length; i++) {
                    int candidate = pattern.indexOf(tokens[i], cursor);
                    if (candidate >= 0 && (next < 0 || candidate < next)) {
                        next = candidate;
                        tokenIndex = i;
                    }
                }
                if (next < 0) {
                    break;
                }
                literalParts.add(pattern.substring(cursor, next));
                indexes.add(tokenIndex);
                cursor = next + tokens[tokenIndex].length();
            }
            literalParts.add(pattern.substring(cursor));
            this.literals = literalParts.toArray(String[]::new);
            this.tokenIndexes = new int[indexes.size()];
            for (int i = 0; i < indexes.size(); i++) {
                tokenIndexes[i] = indexes.get(i);
            }
            this.baseLength = pattern.length();
        }

        private String render3(String v0, String v1, String v2) {
            int capacity = baseLength + v0.length() + v1.length() + v2.length();
            StringBuilder result = new StringBuilder(capacity);
            for (int i = 0; i < tokenIndexes.length; i++) {
                result.append(literals[i]).append(switch (tokenIndexes[i]) {
                    case 0 -> v0;
                    case 1 -> v1;
                    default -> v2;
                });
            }
            return result.append(literals[literals.length - 1]).toString();
        }

        private String render5(String v0, String v1, String v2, String v3, String v4) {
            int capacity = baseLength + v0.length() + v1.length() + v2.length() + v3.length() + v4.length();
            StringBuilder result = new StringBuilder(capacity);
            for (int i = 0; i < tokenIndexes.length; i++) {
                result.append(literals[i]).append(switch (tokenIndexes[i]) {
                    case 0 -> v0;
                    case 1 -> v1;
                    case 2 -> v2;
                    case 3 -> v3;
                    default -> v4;
                });
            }
            return result.append(literals[literals.length - 1]).toString();
        }

        private String render6(String v0, String v1, String v2, String v3, String v4, String v5) {
            int capacity = baseLength + v0.length() + v1.length() + v2.length() + v3.length() + v4.length()
                    + v5.length();
            StringBuilder result = new StringBuilder(capacity);
            for (int i = 0; i < tokenIndexes.length; i++) {
                result.append(literals[i]).append(switch (tokenIndexes[i]) {
                    case 0 -> v0;
                    case 1 -> v1;
                    case 2 -> v2;
                    case 3 -> v3;
                    case 4 -> v4;
                    default -> v5;
                });
            }
            return result.append(literals[literals.length - 1]).toString();
        }
    }
}
