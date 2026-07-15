package exposed.thunder.thunderLBs.config;

import exposed.thunder.thunderLBs.ThunderLBs;
import exposed.thunder.thunderLBs.animation.EasingType;
import exposed.thunder.thunderLBs.leaderboard.BarMode;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public final class PluginConfig {
    public enum RenderMode {
        PACKET,
        ENTITY;

        public static RenderMode fromString(String value, RenderMode fallback) {
            if (value == null || value.isEmpty()) {
                return fallback;
            }
            return switch (value.toLowerCase(Locale.ROOT)) {
                case "packet", "packets" -> PACKET;
                case "entity", "entities" -> ENTITY;
                default -> fallback;
            };
        }
    }

    private final ThunderLBs plugin;
    private RenderMode renderMode;
    private int viewDistance;
    private String providerName;
    private Map<String, Provider> providers;
    private Provider provider;
    private String missingText;
    private String missingPosition;
    private Defaults defaults;
    private Bar bar;
    private Formatting formatting;
    private Sounds sounds;
    private Animation animation;
    private Messages messages;
    private boolean debugPlaceholders;

    public PluginConfig(ThunderLBs plugin) {
        this.plugin = plugin;
        reload(false);
    }

    public void reload(boolean fromDisk) {
        if (fromDisk) {
            plugin.reloadConfig();
        }
        FileConfiguration cfg = plugin.getConfig();
        YamlConfiguration providersCfg = load("providers.yml");
        YamlConfiguration messagesCfg = load("messages.yml");
        this.renderMode = RenderMode.fromString(cfg.getString("render.mode", "packet"), RenderMode.PACKET);
        this.viewDistance = Math.max(4, cfg.getInt("render.view-distance", 48));
        this.providers = loadProviders(providersCfg);
        this.providerName = cfg.getString("provider", "topper").toLowerCase(Locale.ROOT);
        this.provider = providers.getOrDefault(providerName, Provider.topper());
        this.missingText = cfg.getString("missing-text", "---");
        this.missingPosition = Objects.toString(cfg.get("missing-position", "-1"), "-1");
        if (missingPosition.isBlank()) {
            this.missingPosition = "-1";
        }
        this.defaults = Defaults.from(cfg);
        this.bar = Bar.from(cfg.getConfigurationSection("bar"));
        this.formatting = Formatting.from(cfg.getConfigurationSection("formatting"));
        this.sounds = Sounds.from(cfg.getConfigurationSection("sounds"));
        this.animation = Animation.from(cfg.getConfigurationSection("animation"));
        this.messages = Messages.from(messagesCfg);
        this.debugPlaceholders = cfg.getBoolean("debug.placeholders", false);
        if (!providers.containsKey(providerName)) {
            plugin.getLogger().warning("Unknown provider '" + providerName + "', falling back to topper.");
        }
    }

    private YamlConfiguration load(String name) {
        return YamlConfiguration.loadConfiguration(new File(plugin.getDataFolder(), name));
    }

    private Map<String, Provider> loadProviders(ConfigurationSection section) {
        Map<String, Provider> loaded = new LinkedHashMap<>();
        loaded.put("topper", Provider.topper());
        loaded.put("ajleaderboards", Provider.ajLeaderboards());
        if (section != null) {
            for (String key : section.getKeys(false)) {
                ConfigurationSection providerSection = section.getConfigurationSection(key);
                if (providerSection == null) {
                    continue;
                }
                String name = key.toLowerCase(Locale.ROOT);
                Provider base = loaded.getOrDefault(name, Provider.empty());
                loaded.put(name, Provider.from(providerSection, base));
            }
        }
        return Collections.unmodifiableMap(loaded);
    }

    public RenderMode renderMode() {
        return renderMode;
    }

    public int viewDistance() {
        return viewDistance;
    }

    public String providerName() {
        return providerName;
    }

    public Provider provider() {
        return provider;
    }

    public String missingText() {
        return missingText;
    }

    public String missingPosition() {
        return missingPosition;
    }

    public Defaults defaults() {
        return defaults;
    }

    public Bar bar() {
        return bar;
    }

    public Formatting formatting() {
        return formatting;
    }

    public Sounds sounds() {
        return sounds;
    }

    public Animation animation() {
        return animation;
    }

    public Messages messages() {
        return messages;
    }

    public boolean debugPlaceholders() {
        return debugPlaceholders;
    }

    public String prefixedMessage(String key, Map<String, String> placeholders) {
        String content = messages.format(key, placeholders);
        if (content.isEmpty()) {
            return "";
        }
        return messages.prefix() + content;
    }

    public record Provider(
            String name,
            String value,
            String viewerRank,
            String viewerValue,
            String groupName,
            String groupValue,
            String groupViewerRank,
            String groupViewerValue,
            String groupTeam) {

        public static Provider topper() {
            return new Provider(
                    "%topper_%holder%;top_name;%position%%",
                    "%topper_%holder%;top_value_raw;%position%%",
                    "%topper_%holder%;top_rank%",
                    "%topper_%holder%;value_raw%",
                    "%grouptopper_%holder%;top_name;%position%%",
                    "%grouptopper_%holder%;top_value_raw;%position%%",
                    "%grouptopper_%holder%;top_rank%",
                    "%grouptopper_%holder%;value_raw%",
                    "%topper_team%");
        }

        public static Provider ajLeaderboards() {
            return new Provider(
                    "%ajlb_lb_%holder%_%position%_alltime_name%",
                    "%ajlb_lb_%holder%_%position%_alltime_rawvalue%",
                    "%ajlb_position_%holder%_alltime%",
                    "%ajlb_value_%holder%_alltime_raw%",
                    "", "", "", "", "");
        }

        public static Provider empty() {
            return new Provider("", "", "", "", "", "", "", "", "");
        }

        public static Provider from(ConfigurationSection section, Provider base) {
            return new Provider(
                    section.getString("name", base.name()),
                    section.getString("value", base.value()),
                    section.getString("viewer-rank", base.viewerRank()),
                    section.getString("viewer-value", base.viewerValue()),
                    section.getString("group-name", base.groupName()),
                    section.getString("group-value", base.groupValue()),
                    section.getString("group-viewer-rank", base.groupViewerRank()),
                    section.getString("group-viewer-value", base.groupViewerValue()),
                    section.getString("group-team", base.groupTeam()));
        }

        public boolean supportsGroups() {
            return !groupName.isEmpty() && !groupValue.isEmpty();
        }
    }

    public record Defaults(
            int positions,
            long pageDurationTicks,
            long intervalTicks,
            long rowDelayTicks,
            long typingIntervalTicks,
            double rowSpacing,
            double rowStartOffset,
            double barOffsetY,
            double relativeOffset,
            boolean cleanupOnStart) {
        private static Defaults from(FileConfiguration cfg) {
            return new Defaults(
                    cfg.getInt("defaults.positions", 10),
                    cfg.getLong("defaults.page-duration-ticks", 160L),
                    cfg.getLong("defaults.interval-ticks", 200L),
                    cfg.getLong("defaults.row-delay-ticks", 2L),
                    cfg.getLong("defaults.typing-interval-ticks", 1L),
                    cfg.getDouble("defaults.row-spacing", 0.325D),
                    cfg.getDouble("defaults.row-start-offset", -0.8D),
                    cfg.getDouble("defaults.bar-offset-y", -0.5D),
                    cfg.getDouble("defaults.relative-offset", -0.3D),
                    cfg.getBoolean("defaults.cleanup-on-start", true));
        }
    }

    public record Bar(
            BarMode mode,
            String symbol,
            String separator,
            String pendingColor,
            boolean useHolderColor,
            String activeColor,
            String background,
            String foreground) {
        private static Bar from(ConfigurationSection section) {
            if (section == null) {
                return new Bar(BarMode.DOTS, "⏺", " ", "dark_gray", true, "#FFFFFF",
                        "<#4D4D4D><st>                    </st>",
                        "<#CFCFCF><st>                    </st>");
            }
            return new Bar(
                    BarMode.fromString(section.getString("mode", "dots"), BarMode.DOTS),
                    section.getString("symbol", "⏺"),
                    section.getString("separator", " "),
                    section.getString("pending-color", "dark_gray"),
                    section.getBoolean("use-holder-color", true),
                    section.getString("active-color", "#FFFFFF"),
                    section.getString("background", "<#4D4D4D><st>                    </st>"),
                    section.getString("foreground", "<#CFCFCF><st>                    </st>"));
        }
    }

    public record Formatting(
            String title,
            String row,
            String relative,
            List<String> circledNumbers) {
        private static Formatting from(ConfigurationSection section) {
            if (section == null) {
                return new Formatting(defaultTitle(), defaultRow(), defaultRow(), defaultCircled());
            }
            List<String> circled = section.getStringList("circled-numbers");
            if (circled.isEmpty()) {
                circled = defaultCircled();
            }
            return new Formatting(
                    section.getString("title", defaultTitle()),
                    section.getString("row", defaultRow()),
                    section.getString("relative", defaultRow()),
                    Collections.unmodifiableList(circled));
        }

        private static String defaultTitle() {
            return "<color:%color%>%icon%</color> <bold><color:%color%>%title%</color></bold>";
        }

        private static String defaultRow() {
            return "<color:%color%>%position%</color> <white>%player%</white> <dark_gray>→</dark_gray> <color:%color%>%value%</color>";
        }

        private static List<String> defaultCircled() {
            return List.of("①", "②", "③", "④", "⑤", "⑥", "⑦", "⑧", "⑨", "⑩");
        }
    }

    public record Sounds(Typing typing) {
        private static Sounds from(ConfigurationSection section) {
            return new Sounds(Typing.from(section == null ? null : section.getConfigurationSection("typing")));
        }

        public record Typing(boolean enabled, String key, float volume, float pitchMin, float pitchMax, double radius) {
            private static Typing from(ConfigurationSection section) {
                if (section == null) {
                    return new Typing(true, "minecraft:block.note_block.hat", 0.5F, 1.85F, 2.0F, 16.0D);
                }
                return new Typing(
                        section.getBoolean("enabled", true),
                        section.getString("key", "minecraft:block.note_block.hat"),
                        (float) section.getDouble("volume", 0.5D),
                        (float) section.getDouble("pitch-min", 1.85D),
                        (float) section.getDouble("pitch-max", 2.0D),
                        section.getDouble("radius", 16.0D));
            }
        }
    }

    public record Animation(Title title, Row row, Bar bar) {
        private static Animation from(ConfigurationSection section) {
            if (section == null) {
                return new Animation(Title.defaultTitle(), Row.defaultRow(), Bar.defaultBar());
            }
            return new Animation(
                    Title.from(section.getConfigurationSection("title")),
                    Row.from(section.getConfigurationSection("row")),
                    Bar.from(section.getConfigurationSection("bar")));
        }

        public record Title(int inFrames, int outFrames, EasingType inEasing, EasingType outEasing, double scale,
                double overshoot) {
            private static Title from(ConfigurationSection section) {
                if (section == null) {
                    return defaultTitle();
                }
                ConfigurationSection in = section.getConfigurationSection("in");
                ConfigurationSection out = section.getConfigurationSection("out");
                int inFrames = in != null ? in.getInt("frames", 20) : 20;
                int outFrames = out != null ? out.getInt("frames", 20) : 20;
                EasingType inEasing = EasingType.fromConfig(in != null ? in.getString("easing") : null,
                        EasingType.EASE_OUT_BACK);
                EasingType outEasing = EasingType.fromConfig(out != null ? out.getString("easing") : null,
                        EasingType.EASE_IN_SINE);
                double overshoot = in != null ? in.getDouble("overshoot", 4.5D) : 4.5D;
                double scale = in != null ? in.getDouble("scale", 1.5D) : section.getDouble("scale", 1.5D);
                return new Title(inFrames, outFrames, inEasing, outEasing, scale, overshoot);
            }

            private static Title defaultTitle() {
                return new Title(20, 20, EasingType.EASE_OUT_BACK, EasingType.EASE_IN_SINE, 1.5D, 4.5D);
            }
        }

        public record Row(int inFrames, int outFrames, EasingType inEasing, EasingType outEasing, double startOffset,
                double distance) {
            private static Row from(ConfigurationSection section) {
                if (section == null) {
                    return defaultRow();
                }
                ConfigurationSection in = section.getConfigurationSection("in");
                ConfigurationSection out = section.getConfigurationSection("out");
                int inFrames = in != null ? in.getInt("frames", 20) : 20;
                int outFrames = out != null ? out.getInt("frames", 20) : 20;
                EasingType inEasing = EasingType.fromConfig(in != null ? in.getString("easing") : null,
                        EasingType.EASE_OUT_CUBIC);
                EasingType outEasing = EasingType.fromConfig(out != null ? out.getString("easing") : null,
                        EasingType.EASE_IN_CUBIC);
                double startOffset = in != null ? in.getDouble("start-offset", -1.25D) : -1.25D;
                double distance = in != null ? in.getDouble("distance", 1.25D) : 1.25D;
                if (out != null) {
                    distance = out.getDouble("distance", distance);
                }
                return new Row(inFrames, outFrames, inEasing, outEasing, startOffset, distance);
            }

            private static Row defaultRow() {
                return new Row(20, 20, EasingType.EASE_OUT_CUBIC, EasingType.EASE_IN_CUBIC, -1.25D, 1.25D);
            }
        }

        public record Bar(int frames, EasingType easing, double translationStart, double translationEnd) {
            private static Bar from(ConfigurationSection section) {
                if (section == null) {
                    return defaultBar();
                }
                return new Bar(
                        section.getInt("frames", 10),
                        EasingType.fromConfig(section.getString("easing"), EasingType.LINEAR),
                        section.getDouble("translation-start", -1.0D),
                        section.getDouble("translation-end", 0.0D));
            }

            private static Bar defaultBar() {
                return new Bar(10, EasingType.LINEAR, -1.0D, 0.0D);
            }
        }
    }

    public record Messages(
            String prefix,
            Map<String, String> messages) {
        private static Messages from(ConfigurationSection section) {
            if (section == null) {
                return new Messages("&#38BDF8ThunderLBs &8» &7", Map.of());
            }
            String prefix = section.getString("prefix", "&#38BDF8ThunderLBs &8» &7");
            Map<String, String> values = section.getValues(false)
                    .entrySet()
                    .stream()
                    .filter(entry -> !entry.getKey().equalsIgnoreCase("prefix"))
                    .filter(entry -> entry.getValue() instanceof String)
                    .collect(java.util.stream.Collectors.toMap(
                            entry -> entry.getKey().toLowerCase(Locale.ROOT),
                            entry -> (String) entry.getValue()));
            return new Messages(prefix, Collections.unmodifiableMap(values));
        }

        public String message(String key) {
            return messages.getOrDefault(key.toLowerCase(Locale.ROOT), "");
        }

        public String format(String key, Map<String, String> placeholders) {
            String template = message(key);
            if (template.isEmpty()) {
                return "";
            }
            String result = template;
            if (placeholders != null) {
                for (Map.Entry<String, String> entry : placeholders.entrySet()) {
                    result = result.replace("%" + entry.getKey() + "%", entry.getValue());
                }
            }
            return result;
        }
    }
}
