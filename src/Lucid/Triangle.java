package Lucid;
// This class implements a Triangle via our 3D Vectors Class.
public class Triangle {
    public Vec3d[] p = new Vec3d[3]; // The 3 vertices of the triangle
    public int color; // The color of the triangle

    public Triangle(){
            p[0] = new Vec3d();
            p[1] = new Vec3d();
            p[2] = new Vec3d();
            this.color = 0xFFFFFF; // White default colour.
    }
    public Triangle(Vec3d p0, Vec3d p1, Vec3d p2, int color){
        this.p[0] = p0;
        this.p[1] = p1;
        this.p[2] = p2;
        this.color = color;
    }

}
