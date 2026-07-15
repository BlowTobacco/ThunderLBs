package exposed.thunder.thunderLBs.render;

import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.joml.Vector3f;

public interface BoardDisplay {
    void text(Component component);
    void opacity(byte opacity);
    void interpolation(int delayTicks, int durationTicks);
    void transform(Vector3f translation, Vector3f scale);
    default void transform(float translationX, float translationY, float translationZ,
                           float scaleX, float scaleY, float scaleZ) {
        transform(new Vector3f(translationX, translationY, translationZ),
                new Vector3f(scaleX, scaleY, scaleZ));
    }
    default void transformAndOpacity(float translationX, float translationY, float translationZ,
                                     float scaleX, float scaleY, float scaleZ, byte opacity) {
        transform(translationX, translationY, translationZ, scaleX, scaleY, scaleZ);
        opacity(opacity);
    }
    Vector3f translation();
    Vector3f scale();
    void teleport(Location location);
    Location location();
    boolean isRemoved();
    void remove();
}
