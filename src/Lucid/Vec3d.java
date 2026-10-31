package Lucid; 

public class Vec3d {
    public double x,y,z; // The 3 coordinates
    // Constructors below
    public Vec3d(){
        this(0,0,0); // Default.
    }
    public Vec3d(double x, double y, double z){
        this.x = x;
        this.y = y;
        this.z = z;
    }
    public Vec3d add(Vec3d v){
        return new Vec3d(this.x + v.x, this.y + v.y, this.z + v.z);
    }
    // Vector Subtraction
    public Vec3d sub(Vec3d v){
        return new Vec3d(this.x - v.x, this.y - v.y, this.z - v.z);
    }
    // Vector Scaling by a Scalar
    public Vec3d mul(double scalar){
        return new Vec3d((this.x)*scalar, (this.y)*scalar, (this.z)*scalar);
    }
    // Dot Product
    public double dot(Vec3d v){
        return (this.x*v.x) + (this.y*v.y) + (this.z*v.z);
    }
    // Cross Product
    public Vec3d cross(Vec3d v){
        return new Vec3d(
            (this.y*v.z) - (this.z*v.y),
            (this.z*v.x) - (this.x*v.z),
            (this.x*v.y) - (this.y*v.x)
        );
    }
    // Length (Magnitude) of Vector, via sq root of dot product of itself.
    public double length(){
        return Math.sqrt(dot(this));
    }
    // Normalization - it converts the vector to 1 length, keeping direction
    public Vec3d normalize(){
        double len = length();
        if (len == 0) return new Vec3d(0,0,0);
        return new Vec3d(x/len, y/len, z/len);
    }
}