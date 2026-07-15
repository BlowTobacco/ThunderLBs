package exposed.thunder.thunderLBs.animation;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EasingTypeTest {
    @Test
    void allCurvesStartAndEndAtExpectedValues() {
        for (EasingType easing : EasingType.values()) {
            assertEquals(0.0D, easing.apply(0.0D, 1.70158D), 0.000001D, easing.name() + " start");
            assertEquals(1.0D, easing.apply(1.0D, 1.70158D), 0.000001D, easing.name() + " end");
        }
    }

    @Test
    void friendlyNamesAcceptHyphens() {
        assertEquals(EasingType.EASE_OUT_CUBIC,
                EasingType.fromFriendly("ease-out-cubic", EasingType.LINEAR));
    }
}
