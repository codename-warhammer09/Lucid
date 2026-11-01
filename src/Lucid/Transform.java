package Lucid;

/**
 * Describes how an object is placed in the scene.
 *
 * Every game object in the engine is not just a mesh or a color, but a transform:
 * a position in 3D space, a rotation around the axes, and a scale factor. These
 * three values let us represent the same underlying model in many different places
 * in the world while keeping the model definition itself unchanged.
 */
public final class Transform {
    public final Vec3d position;
    public final Vec3d rotation;
    public final Vec3d scale;

    // The default transform keeps the object at the origin and does not distort it.
    public Transform() {
        this(new Vec3d(), new Vec3d(), new Vec3d(1.0, 1.0, 1.0));
    }

    public Transform(Vec3d position, Vec3d rotation, Vec3d scale) {
        this.position = position;
        this.rotation = rotation;
        this.scale = scale;
        validate();
    }

    // Copying is useful when we want to duplicate a transform without sharing the
    // same underlying Vec3d objects.
    public Transform copy() {
        return new Transform(
                new Vec3d(position.x, position.y, position.z),
                new Vec3d(rotation.x, rotation.y, rotation.z),
                new Vec3d(scale.x, scale.y, scale.z));
    }

    // Before an object is accepted into the scene, we check that every number makes
    // sense. A NaN or infinite value would break rendering; a negative or zero scale
    // would distort or collapse the object unexpectedly.
    public void validate() {
        requireFinite(position.x, "position.x");
        requireFinite(position.y, "position.y");
        requireFinite(position.z, "position.z");
        requireFinite(rotation.x, "rotation.x");
        requireFinite(rotation.y, "rotation.y");
        requireFinite(rotation.z, "rotation.z");
        requirePositive(scale.x, "scale.x");
        requirePositive(scale.y, "scale.y");
        requirePositive(scale.z, "scale.z");
    }

    private static void requirePositive(double value, String field) {
        requireFinite(value, field);
        if (value <= 0.0) {
            throw new IllegalArgumentException(field + " must be greater than zero.");
        }
    }

    private static void requireFinite(double value, String field) {
        if (!Double.isFinite(value)) {
            throw new IllegalArgumentException(field + " must be finite.");
        }
    }
}
