package Lucid;

/**
 * A 4x4 matrix used as the engine's workhorse for transforming 3D points.
 *
 * In 3D graphics, transforms are easier to reason about if we treat a point as a
 * vector with an extra homogeneous coordinate. That extra component allows us to
 * combine translation, rotation, and perspective projection into a single matrix
 * multiplication. The matrix is stored in a 4x4 double array, which matches the
 * structure used in many graphics stacks.
 */
public class Mat4x4 {
    // This is the actual matrix data. We store it as a 2D array with 4 rows and 4
    // columns, matching the common linear algebra convention used by most graphics
    // systems: x, y, z, and the extra homogeneous term all live in one place.
    public double[][] m = new double[4][4];

    /**
     * Applies the matrix to a 3D vector and performs a perspective divide.
     *
     * The core idea is that a point in 3D space is transformed by multiplying its
     * coordinates with the matrix. Many transforms (scale, rotate, translate) are
     * expressed in this form. The extra 'w' value is what allows a projection to
     * simulate depth-based shrinking, so objects farther away appear smaller.
     */
    public Vec3d transformVec3d(Vec3d input){
        Vec3d out = new Vec3d(); // The output, transformed 3D vector we will return at the end.
        // Matrix-vector multiplication is done component by component. The matrix
        // entries are arranged so that x, y, and z are influenced by each row and the
        // translation elements are mixed in at the end.
        out.x = input.x * m[0][0] + input.y*m[1][0] + input.z*m[2][0] + m[3][0];
        out.y = input.x * m[0][1] + input.y*m[1][1] + input.z*m[2][1] + m[3][1];
        out.z = input.x * m[0][2] + input.y*m[1][2] + input.z*m[2][2] + m[3][2];
        // W represents the homogeneous coordinate. In a perspective projection,
        // this carries the depth information that is used to shrink points as they
        // move farther from the camera.
        double w = input.x*m[0][3] + input.y*m[1][3] + input.z*m[2][3] + m[3][3];
        // Perspective divide rescales x, y, and z by w so the projected image looks
        // correct: points with larger depth get divided more strongly.
        if (w != 0.0){
            out.x /= w;
            out.y /= w;
            out.z /= w;
        }
        return out;
    }

    /**
     * Builds a projection matrix for perspective rendering.
     *
     * A camera does not see the world in an infinite flat plane: objects farther
     * away appear smaller. This matrix simulates the pinhole camera model, which is
     * the basis of perspective projection in real-time 3D graphics.
     */
    public static Mat4x4 makeProjection(double fovDegrees, double aspectRatio, double near, double far){
        Mat4x4 matrix = new Mat4x4();
        // The field of view determines how strongly the scene converges toward the
        // center of the screen. A larger angle means more dramatic perspective.
        double fovRad = 1.0/Math.tan(Math.toRadians(fovDegrees*0.5));
        // The aspect ratio keeps the horizontal and vertical scaling consistent with
        // the display's shape, so a wide screen doesn't distort the image.
        matrix.m[0][0] = fovRad / aspectRatio; 
        matrix.m[1][1] = fovRad;
        // We map the depth range so that objects near the camera are placed near 0
        // and objects farther away are pushed toward 1. This helps the renderer
        // compare depths in a normalized way.
        matrix.m[2][2] = far / (far-near);
        matrix.m[3][2] = (-far * near)/(far-near);
        // The extra homogeneous term allows the matrix to encode the fact that the
        // projected space depends on the camera's depth.
        matrix.m[2][3] = 1.0;
        matrix.m[3][3] = 0.0;

        return matrix;
    }

    // A translation matrix shifts points in 3D space by (x, y, z). This is how we
    // move objects around the scene without changing their shape.
    public static Mat4x4 makeTranslation(double x, double y, double z){
        Mat4x4 matrix = new Mat4x4();
        matrix.m[0][0] = 1.0;
        matrix.m[1][1] = 1.0;
        matrix.m[2][2] = 1.0;
        matrix.m[3][3] = 1.0;
        matrix.m[3][0] = x;
        matrix.m[3][1] = y;
        matrix.m[3][2] = z;
        return matrix;
    }

    // Rotation around the X axis is often called pitch. It tilts the object up or
    // down, similar to looking at the sky or the ground.
    public static Mat4x4 makeRotationX(double angleRad){
        Mat4x4 matrix = new Mat4x4();
        matrix.m[0][0] = 1.0;
        matrix.m[1][1] = Math.cos(angleRad);
        matrix.m[1][2] = Math.sin(angleRad);
        matrix.m[2][1] = -Math.sin(angleRad);
        matrix.m[2][2] = Math.cos(angleRad);
        matrix.m[3][3] = 1.0;
        return matrix;
    }

    // Rotation around the Y axis is often called yaw. This changes the left/right
    // facing of the object or camera, which is why it is central to turning in a 3D
    // scene.
    public static Mat4x4 makeRotationY(double angleRad){
        Mat4x4 matrix = new Mat4x4();
        matrix.m[0][0] = Math.cos(angleRad);
        matrix.m[0][2] = Math.sin(angleRad);
        matrix.m[1][1] = 1.0;
        matrix.m[2][0] = -Math.sin(angleRad);
        matrix.m[2][2] = Math.cos(angleRad);
        matrix.m[3][3] = 1.0;
        return matrix;
    }

    // Rotation around the Z axis is called roll. In this engine it is used less
    // often, but it is still a standard way to twist an object around its forward
    // direction.
    public static Mat4x4 makeRotationZ(double angleRad){
        Mat4x4 matrix = new Mat4x4();
        matrix.m[0][0] = Math.cos(angleRad);
        matrix.m[0][1] = Math.sin(angleRad);
        matrix.m[1][0] = -Math.sin(angleRad);
        matrix.m[1][1] = Math.cos(angleRad);
        matrix.m[2][2] = 1.0;
        matrix.m[3][3] = 1.0;
        return matrix;
    }
}