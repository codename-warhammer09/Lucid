package Lucid;

public final class Transform {
    public final Vec3d position;
    public final Vec3d rotation;
    public final Vec3d scale;

    public Transform() {
        this(new Vec3d(), new Vec3d(), new Vec3d(1.0, 1.0, 1.0));
    }

    public Transform(Vec3d position, Vec3d rotation, Vec3d scale) {
        this.position = position;
        this.rotation = rotation;
        this.scale = scale;
        validate();
    }

    public Transform copy() {
        return new Transform(
                new Vec3d(position.x, position.y, position.z),
                new Vec3d(rotation.x, rotation.y, rotation.z),
                new Vec3d(scale.x, scale.y, scale.z));
    }

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
