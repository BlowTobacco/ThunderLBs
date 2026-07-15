package exposed.thunder.thunderLBs.render.entity;

import exposed.thunder.thunderLBs.render.BoardDisplay;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.entity.TextDisplay;
import org.bukkit.util.Transformation;
import org.joml.Quaternionf;
import org.joml.Vector3f;

final class EntityBoardDisplay implements BoardDisplay {
    private final TextDisplay entity;

    EntityBoardDisplay(TextDisplay entity) {
        this.entity = entity;
    }

    TextDisplay entity() {
        return entity;
    }

    @Override
    public void text(Component component) {
        entity.text(component);
    }

    @Override
    public void opacity(byte opacity) {
        entity.setTextOpacity(opacity);
    }

    @Override
    public void interpolation(int delayTicks, int durationTicks) {
        entity.setInterpolationDelay(delayTicks);
        entity.setInterpolationDuration(durationTicks);
    }

    @Override
    public void transform(Vector3f translation, Vector3f scale) {
        Transformation current = entity.getTransformation();
        entity.setTransformation(new Transformation(
                new Vector3f(translation),
                new Quaternionf(current.getLeftRotation()),
                new Vector3f(scale),
                new Quaternionf(current.getRightRotation())));
    }

    @Override
    public void transform(float translationX, float translationY, float translationZ,
                          float scaleX, float scaleY, float scaleZ) {
        Transformation current = entity.getTransformation();
        entity.setTransformation(new Transformation(
                new Vector3f(translationX, translationY, translationZ),
                new Quaternionf(current.getLeftRotation()),
                new Vector3f(scaleX, scaleY, scaleZ),
                new Quaternionf(current.getRightRotation())));
    }

    @Override
    public void transformAndOpacity(float translationX, float translationY, float translationZ,
                                    float scaleX, float scaleY, float scaleZ, byte opacity) {
        transform(translationX, translationY, translationZ, scaleX, scaleY, scaleZ);
        entity.setTextOpacity(opacity);
    }

    @Override
    public Vector3f translation() {
        return new Vector3f(entity.getTransformation().getTranslation());
    }

    @Override
    public Vector3f scale() {
        return new Vector3f(entity.getTransformation().getScale());
    }

    @Override
    public void teleport(Location location) {
        entity.teleportAsync(location);
    }

    @Override
    public Location location() {
        return entity.getLocation();
    }

    @Override
    public boolean isRemoved() {
        return entity.isDead();
    }

    @Override
    public void remove() {
        if (!entity.isDead()) {
            entity.remove();
        }
    }
}
