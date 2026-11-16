package Lucid;

/**
 * Projects a START marker vertically onto the nearest scene geometry below it.
 */
public final class StartMarkerProjection {
    private static final double INTERSECTION_EPSILON = 1e-9;

    private StartMarkerProjection() {}

    public static Transform project(Scene scene, Transform transform) {
        if (scene == null || transform == null) {
            throw new IllegalArgumentException("Scene and marker transform are required.");
        }
        Vec3d origin = transform.position;
        Camera camera = new Camera();
        double nearestDistance = Double.POSITIVE_INFINITY;
        for (SceneObject object : scene.getObjects()) {
            if (!object.isGeometry()) continue;
            for (Triangle triangle : MeshFactory.createMesh(object)) {
                Vec3d a = camera.transformLocalPoint(object, triangle.p[0]);
                Vec3d b = camera.transformLocalPoint(object, triangle.p[1]);
                Vec3d c = camera.transformLocalPoint(object, triangle.p[2]);
                double distance = downwardIntersection(origin, a, b, c);
                if (distance >= 0.0 && distance < nearestDistance) {
                    nearestDistance = distance;
                }
            }
        }
        if (!Double.isFinite(nearestDistance)) {
            throw new IllegalArgumentException("No surface is below the START marker.");
        }
        Vec3d position = new Vec3d(origin.x, origin.y - nearestDistance, origin.z);
        return new Transform(position, transform.rotation, transform.scale);
    }

    private static double downwardIntersection(Vec3d origin, Vec3d a, Vec3d b, Vec3d c) {
        Vec3d edge1 = b.sub(a);
        Vec3d edge2 = c.sub(a);
        Vec3d direction = new Vec3d(0.0, -1.0, 0.0);
        Vec3d p = direction.cross(edge2);
        double determinant = edge1.dot(p);
        if (Math.abs(determinant) < INTERSECTION_EPSILON) return -1.0;
        double inverseDeterminant = 1.0 / determinant;
        Vec3d fromA = origin.sub(a);
        double u = fromA.dot(p) * inverseDeterminant;
        if (u < -INTERSECTION_EPSILON || u > 1.0 + INTERSECTION_EPSILON) return -1.0;
        Vec3d q = fromA.cross(edge1);
        double v = direction.dot(q) * inverseDeterminant;
        if (v < -INTERSECTION_EPSILON || u + v > 1.0 + INTERSECTION_EPSILON) return -1.0;
        double distance = edge2.dot(q) * inverseDeterminant;
        return distance >= -INTERSECTION_EPSILON ? Math.max(0.0, distance) : -1.0;
    }
}
