package exposed.thunder.thunderLBs.animation;

public enum EasingType {
    LINEAR {
        @Override
        public double apply(double progress, double overshoot) {
            return progress;
        }
    },
    EASE_IN {
        @Override
        public double apply(double progress, double overshoot) {
            return progress * progress;
        }
    },
    EASE_OUT {
        @Override
        public double apply(double progress, double overshoot) {
            double inv = 1.0D - progress;
            return 1.0D - inv * inv;
        }
    },
    EASE_IN_OUT {
        @Override
        public double apply(double progress, double overshoot) {
            if (progress < 0.5D) {
                return 2.0D * progress * progress;
            }
            double inv = -2.0D * progress + 2.0D;
            return 1.0D - (inv * inv) / 2.0D;
        }
    },
    EASE_OUT_BACK {
        @Override
        public double apply(double progress, double overshoot) {
            double t = progress - 1.0D;
            return 1.0D + (overshoot + 1.0D) * t * t * t + overshoot * t * t;
        }
    },
    EASE_IN_SINE {
        @Override
        public double apply(double progress, double overshoot) {
            return 1.0D - Math.cos(progress * Math.PI / 2.0D);
        }
    },
    EASE_IN_CUBIC {
        @Override
        public double apply(double progress, double overshoot) {
            return progress * progress * progress;
        }
    },
    EASE_OUT_CUBIC {
        @Override
        public double apply(double progress, double overshoot) {
            double n = 1.0D - progress;
            return 1.0D - n * n * n;
        }
    };

    public abstract double apply(double progress, double overshoot);

    public static EasingType fromConfig(String raw, EasingType fallback) {
        if (raw == null || raw.isEmpty()) {
            return fallback;
        }
        try {
            return EasingType.valueOf(raw.toUpperCase());
        } catch (IllegalArgumentException ex) {
            return fallback;
        }
    }

    public static EasingType fromFriendly(String raw, EasingType fallback) {
        if (raw == null || raw.isEmpty()) {
            return fallback;
        }
        String normalized = raw.trim().toUpperCase().replace('-', '_');
        try {
            return EasingType.valueOf(normalized);
        } catch (IllegalArgumentException ignored) {
            return fallback;
        }
    }
}
