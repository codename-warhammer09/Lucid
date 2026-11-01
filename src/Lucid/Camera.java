package Lucid;

/**
 * Represents the viewpoint from which the world is observed.
 *
 * The camera keeps track of where it is in the world and in which direction it is
 * facing. This is the core of the engine's navigation system: objects are not
 * rendered in absolute screen space directly, but after being transformed relative
 * to the camera so the projection step can interpret depth and perspective.
 */
public final class Camera {
    // The camera starts a little way back along the Z axis so the world is visible.
    public final Vec3d position = new Vec3d(0.0, 0.0, -5.0);
    public double yaw;
    public double pitch;

    // Resetting returns to the starting look direction and camera position.
    public void reset() {
        position.x = 0.0;
        position.y = 0.0;
        position.z = -5.0;
        yaw = 0.0;
        pitch = 0.0;
    }

    // A camera's forward direction is not a constant vector; it depends on how the
    // camera is rotated. This function converts yaw and pitch into a direction that
    // points in front of the camera.
    public Vec3d forward() {
        return new Vec3d(Math.sin(yaw) * Math.cos(pitch),
                -Math.sin(pitch), Math.cos(yaw) * Math.cos(pitch));
    }

    // The right direction is used for lateral movement: strafe left and right
    // without changing the camera's facing.
    public Vec3d right() {
        return new Vec3d(Math.cos(yaw), 0.0, -Math.sin(yaw));
    }

    // Objects are modeled in local coordinates, then transformed into world space by
    // applying scale, rotation, and translation. This method performs that local-to-
    // world conversion for a single point.
    public Vec3d transformLocalPoint(SceneObject object, Vec3d point) {
        Vec3d scaled = new Vec3d(point.x * object.transform.scale.x,
                point.y * object.transform.scale.y, point.z * object.transform.scale.z);
        Vec3d rotated = rotate(scaled, object.transform.rotation);
        return rotated.add(object.transform.position);
    }

    // A direction vector is different from a position because it has no translation.
    // This helper rotates a local direction by an object transform without moving it
    // to a new origin.
    public Vec3d transformLocalDirection(Vec3d direction, Vec3d rotation) {
        return rotate(direction, rotation);
    }

    // Converts a world-space point to the object's unscaled local coordinates.
    // This inverse transform lets collision and visibility tests use the shared box.
    public Vec3d transformWorldPointToLocal(SceneObject object, Vec3d point) {
        Vec3d unrotated = rotateInverse(point.sub(object.transform.position),
                object.transform.rotation);
        return new Vec3d(unrotated.x / object.transform.scale.x,
                unrotated.y / object.transform.scale.y,
                unrotated.z / object.transform.scale.z);
    }

    // Applies the inverse rotation and scale to a direction without translation.
    public Vec3d transformWorldDirectionToLocal(SceneObject object, Vec3d direction) {
        Vec3d unrotated = rotateInverse(direction, object.transform.rotation);
        return new Vec3d(unrotated.x / object.transform.scale.x,
                unrotated.y / object.transform.scale.y,
                unrotated.z / object.transform.scale.z);
    }

    // Rotation is applied in X, then Y, then Z order. This is a common pattern in
    // 3D graphics: each rotation updates the local axes relative to the previous one.
    private Vec3d rotate(Vec3d point, Vec3d rotation) {
        Vec3d result = Mat4x4.makeRotationX(rotation.x).transformVec3d(point);
        result = Mat4x4.makeRotationY(rotation.y).transformVec3d(result);
        return Mat4x4.makeRotationZ(rotation.z).transformVec3d(result);
    }

    // Reverses the forward Z/Y/X application order with inverse rotations.
    private Vec3d rotateInverse(Vec3d point, Vec3d rotation) {
        Vec3d result = Mat4x4.makeRotationZ(-rotation.z).transformVec3d(point);
        result = Mat4x4.makeRotationY(-rotation.y).transformVec3d(result);
        return Mat4x4.makeRotationX(-rotation.x).transformVec3d(result);
    }
}
