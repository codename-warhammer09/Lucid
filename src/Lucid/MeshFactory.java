package Lucid;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class MeshFactory {
    private static final Vec3d[] BOX_VERTICES = {
        new Vec3d(-0.5, -0.5, -0.5), new Vec3d(-0.5, 0.5, -0.5),
        new Vec3d(0.5, -0.5, -0.5), new Vec3d(0.5, 0.5, -0.5),
        new Vec3d(-0.5, -0.5, 0.5), new Vec3d(-0.5, 0.5, 0.5),
        new Vec3d(0.5, -0.5, 0.5), new Vec3d(0.5, 0.5, 0.5)
    };
    private static final int[][] BOX_FACES = {
        {0, 1, 3}, {0, 3, 2}, {6, 7, 5}, {6, 5, 4},
        {2, 3, 7}, {2, 7, 6}, {4, 5, 1}, {4, 1, 0},
        {1, 5, 7}, {1, 7, 3}, {4, 0, 2}, {4, 2, 6}
    };
    private static final int[][] BOX_EDGES = {
        {0, 1}, {0, 2}, {1, 3}, {2, 3}, {4, 5}, {4, 6},
        {5, 7}, {6, 7}, {0, 4}, {1, 5}, {2, 6}, {3, 7}
    };

    private MeshFactory() {}

    public static List<Triangle> createMesh(SceneObject object) {
        List<Triangle> triangles = new ArrayList<>(BOX_FACES.length);
        for (int[] face : BOX_FACES) {
            triangles.add(new Triangle(copy(BOX_VERTICES[face[0]]),
                    copy(BOX_VERTICES[face[1]]), copy(BOX_VERTICES[face[2]]), object.color));
        }
        return triangles;
    }

    public static Vec3d[] getBoxVertices() {
        Vec3d[] vertices = new Vec3d[BOX_VERTICES.length];
        for (int i = 0; i < vertices.length; i++) vertices[i] = copy(BOX_VERTICES[i]);
        return vertices;
    }

    public static int[][] getBoxEdges() {
        int[][] edges = new int[BOX_EDGES.length][];
        for (int i = 0; i < edges.length; i++) edges[i] = Arrays.copyOf(BOX_EDGES[i], 2);
        return edges;
    }

    private static Vec3d copy(Vec3d vector) {
        return new Vec3d(vector.x, vector.y, vector.z);
    }
}
