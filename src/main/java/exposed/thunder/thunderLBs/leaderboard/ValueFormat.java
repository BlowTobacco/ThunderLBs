package exposed.thunder.thunderLBs.leaderboard;

import exposed.thunder.thunderLBs.util.FormattingUtil;

public enum ValueFormat {
    RAW {
        @Override
        public String format(String raw) {
            return raw;
        }
    },
    SHORT_NUMBER {
        @Override
        public String format(String raw) {
            try {
                double value = Double.parseDouble(raw);
                return FormattingUtil.shortNumber(value, 2);
            } catch (NumberFormatException ex) {
                return raw;
            }
        }
    },
    HOURS_SUFFIX {
        @Override
        public String format(String raw) {
            try {
                double value = Double.parseDouble(raw);
                int hours = (int) Math.floor(value);
                return hours + "h";
            } catch (NumberFormatException ex) {
                return raw;
            }
        }
    },
    TIMESPAN {
        @Override
        public String format(String raw) {
            try {
                double value = Double.parseDouble(raw);
                long millis = (long) (value * 50);
                return FormattingUtil.formatTimespan(millis);
            } catch (NumberFormatException ex) {
                return raw;
            }
        }
    },
    TIMESPAN_SECONDS {
        @Override
        public String format(String raw) {
            try {
                double value = Double.parseDouble(raw);
                long millis = (long) (value * 1000);
                return FormattingUtil.formatTimespan(millis);
            } catch (NumberFormatException ex) {
                return raw;
            }
        }
    },
    TIMESPAN_MILLIS {
        @Override
        public String format(String raw) {
            try {
                double value = Double.parseDouble(raw);
                return FormattingUtil.formatTimespan((long) value);
            } catch (NumberFormatException ex) {
                return raw;
            }
        }
    },
    TIME {
        @Override
        public String format(String raw) {
            try {
                double value = Double.parseDouble(raw);
                long millis = (long) (value * 1000);
                return FormattingUtil.formatTimespan(millis);
            } catch (NumberFormatException ex) {
                return raw;
            }
        }
    },
    TIME_TICKS {
        @Override
        public String format(String raw) {
            try {
                double value = Double.parseDouble(raw);
                long millis = (long) (value * 50);
                return FormattingUtil.formatTimespan(millis);
            } catch (NumberFormatException ex) {
                return raw;
            }
        }
    };

    public abstract String format(String raw);

    public static ValueFormat fromConfig(String raw, ValueFormat fallback) {
        if (raw == null || raw.isEmpty()) {
            return fallback;
        }
        try {
            return ValueFormat.valueOf(raw.toUpperCase(java.util.Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return fallback;
        }
    }
}
