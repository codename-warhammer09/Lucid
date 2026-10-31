package Lucid;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class SceneRenderer {
    public static final int WIDTH = 320;
    public static final int HEIGHT = 240;
    private static final double NEAR_PLANE = 0.1;
    private static final double FAR_PLANE = 100.0;
    private static final double AMBIENT_LIGHT = 0.22;

    private final BufferedImage frameBuffer =
            new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
    private final int[] pixels = ((DataBufferInt) frameBuffer.getRaster()
            .getDataBuffer()).getData();
    private final double[] zBuffer = new double[WIDTH * HEIGHT];
    private final int[] objectBuffer = new int[WIDTH * HEIGHT];
    private final Mat4x4 projection = Mat4x4.makeProjection(
            90.0, (double) WIDTH / HEIGHT, NEAR_PLANE, FAR_PLANE);

    public BufferedImage render(Scene scene, Camera camera, String selectedId) {
        List<SceneObject> objects = scene.getObjects();
        Arrays.fill(pixels, 0x111522);
        Arrays.fill(zBuffer, Double.MAX_VALUE);
        Arrays.fill(objectBuffer, -1);

        Mat4x4 yaw = Mat4x4.makeRotationY(camera.yaw);
        Mat4x4 pitch = Mat4x4.makeRotationX(-camera.pitch);
        for (int i = 0; i < objects.size(); i++) {
            SceneObject object = objects.get(i);
            if (!object.isLight()) {
                renderObject(object, i, objects, camera, yaw, pitch,
                        selectedId != null && selectedId.equals(object.id));
            }
        }
        for (int i = 0; i < objects.size(); i++) {
            SceneObject object = objects.get(i);
            if (object.isLight()) {
                renderObject(object, i, objects, camera, yaw, pitch,
                        selectedId != null && selectedId.equals(object.id));
            }
        }
        return frameBuffer;
    }

    public String pick(Scene scene, int x, int y) {
        if (x < 0 || x >= WIDTH || y < 0 || y >= HEIGHT) return null;
        int objectIndex = objectBuffer[x + y * WIDTH];
        List<SceneObject> objects = scene.getObjects();
        return objectIndex >= 0 && objectIndex < objects.size()
                ? objects.get(objectIndex).id : null;
    }

    private void renderObject(SceneObject object, int objectIndex, List<SceneObject> objects,
            Camera camera, Mat4x4 yaw, Mat4x4 pitch, boolean selected) {
        Vec3d[] worldVertices = MeshFactory.getBoxVertices();
        Vec3d[] viewVertices = new Vec3d[worldVertices.length];
        for (int i = 0; i < worldVertices.length; i++) {
            Vec3d world = camera.transformLocalPoint(object, worldVertices[i]);
            Vec3d relative = world.sub(camera.position);
            viewVertices[i] = pitch.transformVec3d(yaw.transformVec3d(relative));
        }

        int[][] edges = MeshFactory.getBoxEdges();
        for (Triangle triangle : MeshFactory.createMesh(object)) {
            Vec3d world0 = camera.transformLocalPoint(object, triangle.p[0]);
            Vec3d world1 = camera.transformLocalPoint(object, triangle.p[1]);
            Vec3d world2 = camera.transformLocalPoint(object, triangle.p[2]);
            Vec3d normal = world1.sub(world0).cross(world2.sub(world0)).normalize();
            if (!object.isLight() && normal.dot(camera.position.sub(world0)) <= 0.0) continue;

            List<Vec3d> clipped = new ArrayList<>(3);
            for (Vec3d vertex : triangle.p) {
                Vec3d world = camera.transformLocalPoint(object, vertex);
                clipped.add(pitch.transformVec3d(yaw.transformVec3d(world.sub(camera.position))));
            }
            clipped = clipPolygon(clipped, NEAR_PLANE, true);
            clipped = clipPolygon(clipped, FAR_PLANE, false);
            int color = object.isLight()
                    ? object.color
                    : applyLighting(object.color, normal, world0, objects, camera);
            for (int i = 1; i < clipped.size() - 1; i++) {
                fillTriangle(projectToScreen(clipped.get(0)),
                        projectToScreen(clipped.get(i)),
                        projectToScreen(clipped.get(i + 1)), color, objectIndex);
            }
        }

        if (selected) {
            for (int[] edge : edges) {
                Vec3d[] line = clipLineToDepth(viewVertices[edge[0]], viewVertices[edge[1]]);
                if (line != null) {
                    drawDepthLine(projectToScreen(line[0]), projectToScreen(line[1]),
                            0xFFFF30, objectIndex);
                }
            }
        }
    }

    private int applyLighting(int baseColor, Vec3d normal, Vec3d surface,
            List<SceneObject> objects, Camera camera) {
        double red = AMBIENT_LIGHT;
        double green = AMBIENT_LIGHT;
        double blue = AMBIENT_LIGHT;
        for (SceneObject light : objects) {
            if (!light.isLight()) continue;
            double diffuse;
            if (light.type == SceneObject.Type.DIRECTIONAL_LIGHT) {
                Vec3d towardLight = camera.transformLocalDirection(
                        new Vec3d(0.0, 0.0, -1.0), light.transform.rotation).normalize();
                diffuse = Math.max(0.0, normal.dot(towardLight)) * light.intensity;
            } else {
                Vec3d toLight = light.transform.position.sub(surface);
                double distanceSquared = toLight.dot(toLight);
                diffuse = distanceSquared == 0.0 ? 0.0
                        : Math.max(0.0, normal.dot(toLight.normalize())) * light.intensity
                                / (1.0 + 0.12 * distanceSquared);
            }
            red += diffuse * ((light.lightColor >> 16) & 0xFF) / 255.0;
            green += diffuse * ((light.lightColor >> 8) & 0xFF) / 255.0;
            blue += diffuse * (light.lightColor & 0xFF) / 255.0;
        }
        int r = shadeChannel((baseColor >> 16) & 0xFF, red);
        int g = shadeChannel((baseColor >> 8) & 0xFF, green);
        int b = shadeChannel(baseColor & 0xFF, blue);
        return (r << 16) | (g << 8) | b;
    }

    private int shadeChannel(int channel, double intensity) {
        return (int) Math.max(0, Math.min(255, Math.round(channel * intensity)));
    }

    private List<Vec3d> clipPolygon(List<Vec3d> polygon, double plane, boolean keepGreater) {
        List<Vec3d> clipped = new ArrayList<>();
        if (polygon.isEmpty()) return clipped;

        Vec3d previous = polygon.get(polygon.size() - 1);
        boolean previousInside = keepGreater ? previous.z >= plane : previous.z <= plane;
        for (Vec3d current : polygon) {
            boolean currentInside = keepGreater ? current.z >= plane : current.z <= plane;
            if (currentInside != previousInside) {
                double t = (plane - previous.z) / (current.z - previous.z);
                Vec3d intersection = previous.add(current.sub(previous).mul(t));
                intersection.z = plane;
                clipped.add(intersection);
            }
            if (currentInside) clipped.add(current);
            previous = current;
            previousInside = currentInside;
        }
        return clipped;
    }

    private Vec3d projectToScreen(Vec3d point) {
        Vec3d projected = projection.transformVec3d(point);
        projected.x = (projected.x + 1.0) * 0.5 * WIDTH;
        projected.y = (1.0 - (projected.y + 1.0) * 0.5) * HEIGHT;
        return projected;
    }

    private Vec3d[] clipLineToDepth(Vec3d start, Vec3d end) {
        Vec3d[] clipped = clipLineAgainstPlane(start, end, NEAR_PLANE, true);
        return clipped == null ? null : clipLineAgainstPlane(clipped[0], clipped[1], FAR_PLANE, false);
    }

    private Vec3d[] clipLineAgainstPlane(Vec3d start, Vec3d end, double plane,
            boolean keepGreater) {
        boolean startInside = keepGreater ? start.z >= plane : start.z <= plane;
        boolean endInside = keepGreater ? end.z >= plane : end.z <= plane;
        if (!startInside && !endInside) return null;
        if (startInside && endInside) return new Vec3d[] {start, end};

        double t = (plane - start.z) / (end.z - start.z);
        Vec3d intersection = start.add(end.sub(start).mul(t));
        intersection.z = plane;
        if (startInside) {
            return new Vec3d[] {start, intersection};
        }
        return new Vec3d[] {intersection, end};
    }

    private Vec3d[] clipLineToViewport(Vec3d start, Vec3d end) {
        double dx = end.x - start.x;
        double dy = end.y - start.y;
        double[] p = {-dx, dx, -dy, dy};
        double[] q = {start.x, WIDTH - 1.0 - start.x, start.y, HEIGHT - 1.0 - start.y};
        double tStart = 0.0;
        double tEnd = 1.0;
        for (int i = 0; i < p.length; i++) {
            if (p[i] == 0.0) {
                if (q[i] < 0.0) return null;
                continue;
            }
            double t = q[i] / p[i];
            if (p[i] < 0.0) tStart = Math.max(tStart, t);
            else tEnd = Math.min(tEnd, t);
            if (tStart > tEnd) return null;
        }
        Vec3d delta = end.sub(start);
        return new Vec3d[] {start.add(delta.mul(tStart)), start.add(delta.mul(tEnd))};
    }

    private void drawDepthLine(Vec3d start, Vec3d end, int color, int objectIndex) {
        Vec3d[] viewportLine = clipLineToViewport(start, end);
        if (viewportLine == null) return;
        start = viewportLine[0];
        end = viewportLine[1];
        double dx = end.x - start.x;
        double dy = end.y - start.y;
        int steps = (int) Math.ceil(Math.max(Math.abs(dx), Math.abs(dy)));
        for (int i = 0; i <= steps; i++) {
            double t = steps == 0 ? 0.0 : (double) i / steps;
            int x = (int) Math.round(start.x + dx * t);
            int y = (int) Math.round(start.y + dy * t);
            drawDepthPixel(x, y, start.z + (end.z - start.z) * t, color, objectIndex);
        }
    }

    private void fillTriangle(Vec3d p0, Vec3d p1, Vec3d p2, int color, int objectIndex) {
        int minX = (int) Math.max(0, Math.min(p0.x, Math.min(p1.x, p2.x)));
        int maxX = (int) Math.min(WIDTH - 1, Math.max(p0.x, Math.max(p1.x, p2.x)));
        int minY = (int) Math.max(0, Math.min(p0.y, Math.min(p1.y, p2.y)));
        int maxY = (int) Math.min(HEIGHT - 1, Math.max(p0.y, Math.max(p1.y, p2.y)));
        double area = edgeFunction(p0, p1, p2.x, p2.y);
        if (area == 0.0) return;

        for (int y = minY; y <= maxY; y++) {
            for (int x = minX; x <= maxX; x++) {
                double px = x + 0.5;
                double py = y + 0.5;
                double w0 = edgeFunction(p1, p2, px, py);
                double w1 = edgeFunction(p2, p0, px, py);
                double w2 = edgeFunction(p0, p1, px, py);
                if ((w0 >= 0 && w1 >= 0 && w2 >= 0)
                        || (w0 <= 0 && w1 <= 0 && w2 <= 0)) {
                    w0 /= area;
                    w1 /= area;
                    w2 /= area;
                    double z = w0 * p0.z + w1 * p1.z + w2 * p2.z;
                    int index = x + y * WIDTH;
                    if (z < zBuffer[index]) {
                        zBuffer[index] = z;
                        pixels[index] = color;
                        objectBuffer[index] = objectIndex;
                    }
                }
            }
        }
    }

    private double edgeFunction(Vec3d a, Vec3d b, double px, double py) {
        return (px - a.x) * (b.y - a.y) - (py - a.y) * (b.x - a.x);
    }

    private void drawDepthPixel(int x, int y, double z, int color, int objectIndex) {
        if (x < 0 || x >= WIDTH || y < 0 || y >= HEIGHT) return;
        int index = x + y * WIDTH;
        if (z <= zBuffer[index] + 0.001) {
            pixels[index] = color;
            objectBuffer[index] = objectIndex;
        }
    }

    public void drawScaled(Graphics2D graphics, int x, int y, int width, int height) {
        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        graphics.drawImage(frameBuffer, x, y, width, height, null);
    }
}
