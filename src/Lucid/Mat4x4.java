package Lucid;

public class Mat4x4 {
    // Now to represent a 4x4 Matrix, we do so using a 2D array of double with 4 rows and 4 columns.
    // This double array is essentially our core instance var.
    public double[][] m = new double[4][4];

// Multiplies a 3D Vector by the 4x4 matrix object and applies Perspective Division
public Vec3d transformVec3d(Vec3d input){
    Vec3d out = new Vec3d(); // The output, transformed 3D vector we will return at the end.
    // First, we implement Matrix-Vector Multiplication
    out.x = input.x * m[0][0] + input.y*m[1][0] + input.z*m[2][0] + m[3][0];
    out.y = input.x * m[0][1] + input.y*m[1][1] + input.z*m[2][1] + m[3][1];
    out.z = input.x * m[0][2] + input.y*m[1][2] + input.z*m[2][2] + m[3][2];
    // Now for W component which collects Z depth for perspective division (which shrinks object
    // based on its distance from the camera)
    double w = input.x*m[0][3] + input.y*m[1][3] + input.z*m[2][3] + m[3][3];
    // Perspective Divide
    if (w != 0.0){
        out.x /= w;
        out.y /= w;
        out.z /= w;
    }
    return out;
}
    // 3D Projection Matrix Construction
    public static Mat4x4 makeProjection(double fovDegrees, double aspectRatio, double near, double far){
        Mat4x4 matrix = new Mat4x4();
        // Perspective scaling from the vertical field of view.
        double fovRad = 1.0/Math.tan(Math.toRadians(fovDegrees*0.5));
        // Horizontal and Vertical Projection Scales
        matrix.m[0][0] = fovRad / aspectRatio; 
        matrix.m[1][1] = fovRad;
        // Depth Mapping, such that near -> 0 and far -> 1
        matrix.m[2][2] = far / (far-near);
        matrix.m[3][2] = (-far * near)/(far-near);
        // Homogenous perspective division, such that w_clip = z_view
        matrix.m[2][3] = 1.0;
        matrix.m[3][3] = 0.0;

        return matrix;
    }
    // A Translation Matrix to shift points in 3D space by (x,y,z)
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
    // Matrix for X-Axis Rotation (also called Pitch tilt.)
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
    // Matrix for Y-Axis Rotation (also called Yaw tilt.)
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