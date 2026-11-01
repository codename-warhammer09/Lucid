package Lucid;

/**
 * A single triangle used as the basic building block for a mesh.
 *
 * Any 3D model can be broken down into tiny triangles. They are easy to work with
 * because a triangle is always planar, and the renderer can calculate its surface
 * normal, shading, and screen coverage reliably.
 */
public class Triangle {
    // A triangle is defined by three vertices in 3D space. The order matters for
    // the surface normal, which in turn affects how lights are applied.
    public Vec3d[] p = new Vec3d[3];
    public int color; // The color of the triangle

    // Default triangle: a white triangle at the origin.
    public Triangle(){
            p[0] = new Vec3d();
            p[1] = new Vec3d();
            p[2] = new Vec3d();
            this.color = 0xFFFFFF; // White default colour.
    }

    // A triangle can be created from any three corner points and a color.
    public Triangle(Vec3d p0, Vec3d p1, Vec3d p2, int color){
        this.p[0] = p0;
        this.p[1] = p1;
        this.p[2] = p2;
        this.color = color;
    }

}
