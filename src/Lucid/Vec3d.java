package Lucid; 

/**
 * A simple 3D vector with x, y, and z components.
 *
 * In 3D graphics, vectors are used for almost everything: positions of points,
 * directions a camera faces, normals on surfaces, and the differences between two
 * points in space. This class uses the familiar component-wise math of linear
 * algebra, but keeps the implementation intentionally small and readable so the
 * surrounding engine code can stay straightforward.
 */
public class Vec3d {
    // Each vector stores three numbers: one for left/right, one for up/down,
    // and one for forward/backward. These values describe where a point sits in
    // 3D space or how a direction points in that space.
    public double x,y,z;

    // A default vector is just the origin: (0, 0, 0).
    public Vec3d(){
        this(0,0,0); // Default.
    }

    // A vector can be created from explicit coordinates when we need a point or
    // direction that is not the origin.
    public Vec3d(double x, double y, double z){
        this.x = x;
        this.y = y;
        this.z = z;
    }

    // Addition is used when combining positions or moving one point by another
    // vector. Think of it as taking one arrow and placing it at the end of the
    // other arrow.
    public Vec3d add(Vec3d v){
        return new Vec3d(this.x + v.x, this.y + v.y, this.z + v.z);
    }

    // Subtraction is the opposite of addition. It tells us the difference between
    // two points, which is extremely important for directions like "from camera to
    // surface" or "from one vertex to another".
    public Vec3d sub(Vec3d v){
        return new Vec3d(this.x - v.x, this.y - v.y, this.z - v.z);
    }

    // Multiplying by a scalar scales the vector length without changing its
    // direction. For example, a direction vector can be made twice as long.
    public Vec3d mul(double scalar){
        return new Vec3d((this.x)*scalar, (this.y)*scalar, (this.z)*scalar);
    }

    // The dot product measures how aligned two vectors are. It is often used to
    // determine how much a surface is facing the light or camera, because a larger
    // positive dot product means the vectors point in similar directions.
    public double dot(Vec3d v){
        return (this.x*v.x) + (this.y*v.y) + (this.z*v.z);
    }

    // The cross product produces a vector perpendicular to both inputs. This is
    // how surfaces get their normals: the normal tells us which way a triangle is
    // facing, which is essential for shading and hidden-surface culling.
    public Vec3d cross(Vec3d v){
        return new Vec3d(
            (this.y*v.z) - (this.z*v.y),
            (this.z*v.x) - (this.x*v.z),
            (this.x*v.y) - (this.y*v.x)
        );
    }

    // The length is the magnitude of the vector, i.e. how long the arrow is.
    // We compute it from the dot product against itself, which is a neat math
    // identity: |v|^2 = v · v.
    public double length(){
        return Math.sqrt(dot(this));
    }

    // Normalization changes a vector so it keeps the same direction but becomes a
    // unit-length vector of length 1. This makes comparisons and lighting math much
    // easier because we can reason about direction without caring about magnitude.
    public Vec3d normalize(){
        double len = length();
        if (len == 0) return new Vec3d(0,0,0);
        return new Vec3d(x/len, y/len, z/len);
    }
}