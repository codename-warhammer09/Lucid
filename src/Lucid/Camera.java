package Lucid;

public final class Camera {
    public final Vec3d position = new Vec3d(0.0, 0.0, -5.0);
    public double yaw;
    public double pitch;

    public void reset() {
        position.x = 0.0;
        position.y = 0.0;
        position.z = -5.0;
        yaw = 0.0;
        pitch = 0.0;
    }

    public Vec3d forward() {
        return new Vec3d(Math.sin(yaw) * Math.cos(pitch),
                -Math.sin(pitch), Math.cos(yaw) * Math.cos(pitch));
    }

    public Vec3d right() {
        return new Vec3d(Math.cos(yaw), 0.0, -Math.sin(yaw));
    }

    public Vec3d transformLocalPoint(SceneObject object, Vec3d point) {
        Vec3d scaled = new Vec3d(point.x * object.transform.scale.x,
                point.y * object.transform.scale.y, point.z * object.transform.scale.z);
        Vec3d rotated = rotate(scaled, object.transform.rotation);
        return rotated.add(object.transform.position);
    }

    public Vec3d transformLocalDirection(Vec3d direction, Vec3d rotation) {
        return rotate(direction, rotation);
    }

    private Vec3d rotate(Vec3d point, Vec3d rotation) {
        Vec3d result = Mat4x4.makeRotationX(rotation.x).transformVec3d(point);
        result = Mat4x4.makeRotationY(rotation.y).transformVec3d(result);
        return Mat4x4.makeRotationZ(rotation.z).transformVec3d(result);
    }
}
