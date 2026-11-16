package Lucid;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Converts the scene into a 2D image by repeatedly transforming geometry and
 * drawing it into a frame buffer.
 *
 * This is the heart of the engine. It takes 3D mesh data, positions the camera,
 * applies transforms, checks which triangles are visible, shades them based on
 * lights, and writes the final pixel colors into a buffered image. Opaque meshes
 * also occlude point- and directional-light contributions before surfaces are
 * shaded.
 */
public final class SceneRenderer {
    public static final int WIDTH = 320;
    public static final int HEIGHT = 240;
    private static final double NEAR_PLANE = 0.1;
    private static final double FAR_PLANE = 100.0;
    private static final double SHADOW_EPSILON = 0.001;
    private static final double GOLDEN_ANGLE = Math.PI * (3.0 - Math.sqrt(5.0));

    private static final class LightData {
        private final SceneObject object;
        private final Vec3d direction;

        private LightData(SceneObject object, Vec3d direction) {
            this.object = object;
            this.direction = direction;
        }
    }

    private static final class OccluderData {
        private final SceneObject object;
        private final Vec3d position;
        private final double xx;
        private final double xy;
        private final double xz;
        private final double yx;
        private final double yy;
        private final double yz;
        private final double zx;
        private final double zy;
        private final double zz;
        private final double minX;
        private final double minY;
        private final double minZ;
        private final double maxX;
        private final double maxY;
        private final double maxZ;

        private OccluderData(SceneObject object, Camera camera) {
            this.object = object;
            position = object.transform.position;
            Vec3d[] vertices = MeshFactory.getBoxVertices();
            double boundsMinX = Double.POSITIVE_INFINITY;
            double boundsMinY = Double.POSITIVE_INFINITY;
            double boundsMinZ = Double.POSITIVE_INFINITY;
            double boundsMaxX = Double.NEGATIVE_INFINITY;
            double boundsMaxY = Double.NEGATIVE_INFINITY;
            double boundsMaxZ = Double.NEGATIVE_INFINITY;
            for (Vec3d vertex : vertices) {
                Vec3d world = camera.transformLocalPoint(object, vertex);
                boundsMinX = Math.min(boundsMinX, world.x);
                boundsMinY = Math.min(boundsMinY, world.y);
                boundsMinZ = Math.min(boundsMinZ, world.z);
                boundsMaxX = Math.max(boundsMaxX, world.x);
                boundsMaxY = Math.max(boundsMaxY, world.y);
                boundsMaxZ = Math.max(boundsMaxZ, world.z);
            }
            minX = boundsMinX;
            minY = boundsMinY;
            minZ = boundsMinZ;
            maxX = boundsMaxX;
            maxY = boundsMaxY;
            maxZ = boundsMaxZ;
            Vec3d localX = camera.transformWorldDirectionToLocal(object,
                    new Vec3d(1.0, 0.0, 0.0));
            Vec3d localY = camera.transformWorldDirectionToLocal(object,
                    new Vec3d(0.0, 1.0, 0.0));
            Vec3d localZ = camera.transformWorldDirectionToLocal(object,
                    new Vec3d(0.0, 0.0, 1.0));
            xx = localX.x;
            xy = localY.x;
            xz = localZ.x;
            yx = localX.y;
            yy = localY.y;
            yz = localZ.y;
            zx = localX.z;
            zy = localY.z;
            zz = localZ.z;
        }
    }

    // The frame buffer is the image that will ultimately be displayed in the UI.
    private BufferedImage frameBuffer;
    private int[] pixels;
    // The z-buffer tracks the closest visible depth at each pixel so far. This is
    // how the renderer decides which triangle wins when surfaces overlap.
    private double[] zBuffer;
    // The object buffer stores which object owns a given pixel, which is what makes
    // clicking on the viewport possible.
    private int[] objectBuffer;
    private Mat4x4 projection;
    private RenderSettings settings = RenderSettings.defaults();
    private boolean wireframeOverlayEnabled;
    private int backgroundColor = settings.editorBackgroundColor;

    public SceneRenderer() {
        configure(settings);
    }

    public void configure(RenderSettings renderSettings) {
        if (renderSettings == null) {
            throw new IllegalArgumentException("Render settings are required.");
        }
        settings = renderSettings;
        frameBuffer = new BufferedImage(settings.width, settings.height,
                BufferedImage.TYPE_INT_RGB);
        pixels = ((DataBufferInt) frameBuffer.getRaster().getDataBuffer()).getData();
        zBuffer = new double[settings.width * settings.height];
        objectBuffer = new int[settings.width * settings.height];
        projection = Mat4x4.makeProjection(settings.fieldOfView,
                (double) settings.width / settings.height, NEAR_PLANE, FAR_PLANE);
        backgroundColor = settings.editorBackgroundColor;
    }

    public int getWidth() {
        return settings.width;
    }

    public int getHeight() {
        return settings.height;
    }

    public void setBackgroundColor(int color) {
        if ((color & 0xFF000000) != 0) {
            throw new IllegalArgumentException("Background color must be a 24-bit RGB value.");
        }
        backgroundColor = color;
    }

    public void setWireframeOverlayEnabled(boolean enabled) {
        wireframeOverlayEnabled = enabled;
    }

    // This renders a whole frame from the given scene and camera.
    public BufferedImage render(Scene scene, Camera camera, String selectedId) {
        return render(scene, camera, selectedId, true);
    }

    public BufferedImage render(Scene scene, Camera camera, String selectedId,
            boolean showStartMarker) {
        List<SceneObject> objects = scene.getObjects();
        Arrays.fill(pixels, backgroundColor);
        Arrays.fill(zBuffer, Double.MAX_VALUE);
        Arrays.fill(objectBuffer, -1);

        // Camera rotation is split into yaw and pitch so movement controls can be
        // modeled naturally: turn left/right and look up/down.
        Mat4x4 yaw = Mat4x4.makeRotationY(camera.yaw);
        Mat4x4 pitch = Mat4x4.makeRotationX(-camera.pitch);
        Mat4x4 inverseYaw = Mat4x4.makeRotationY(-camera.yaw);
        Mat4x4 inversePitch = Mat4x4.makeRotationX(camera.pitch);
        List<LightData> lights = new ArrayList<>();
        List<OccluderData> occluders = new ArrayList<>();
        for (SceneObject object : objects) {
            if (object.type == SceneObject.Type.START_MARKER) continue;
            if (object.isLight()) {
                Vec3d direction = object.type == SceneObject.Type.DIRECTIONAL_LIGHT
                        ? camera.transformLocalDirection(
                                new Vec3d(0.0, 0.0, -1.0), object.transform.rotation).normalize()
                        : null;
                lights.add(new LightData(object, direction));
            } else if (object.isGeometry()) {
                occluders.add(new OccluderData(object, camera));
            }
        }
        for (int i = 0; i < objects.size(); i++) {
            SceneObject object = objects.get(i);
            if (object.isGeometry() || (showStartMarker
                    && object.type == SceneObject.Type.START_MARKER)) {
                renderObject(object, i, lights, occluders, camera, yaw, pitch,
                        inverseYaw, inversePitch,
                        selectedId != null && selectedId.equals(object.id));
            }
        }
        for (int i = 0; i < objects.size(); i++) {
            SceneObject object = objects.get(i);
            if (object.isLight()) {
                renderObject(object, i, lights, occluders, camera, yaw, pitch,
                        inverseYaw, inversePitch,
                        selectedId != null && selectedId.equals(object.id));
            }
        }
        return frameBuffer;
    }

    // Reads the object id under a given pixel in the render buffer. This is used by
    // the viewport when the user clicks to select an object.
    public String pick(Scene scene, int x, int y) {
        if (x < 0 || x >= settings.width || y < 0 || y >= settings.height) return null;
        int objectIndex = objectBuffer[x + y * settings.width];
        List<SceneObject> objects = scene.getObjects();
        return objectIndex >= 0 && objectIndex < objects.size()
                ? objects.get(objectIndex).id : null;
    }

    // Draws one object's mesh.
    private void renderObject(SceneObject object, int objectIndex, List<LightData> lights,
            List<OccluderData> occluders,
            Camera camera, Mat4x4 yaw, Mat4x4 pitch,
            Mat4x4 inverseYaw, Mat4x4 inversePitch, boolean selected) {
        if (object.type == SceneObject.Type.START_MARKER) {
            Vec3d markerPosition = object.transform.position;
            object = new SceneObject(object.id, object.name, object.type,
                    new Transform(new Vec3d(markerPosition.x,
                            markerPosition.y + object.transform.scale.y * 0.5 + 0.005,
                            markerPosition.z), object.transform.rotation, object.transform.scale),
                    object.color, object.lightColor, object.intensity);
        }
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
            int faceColor = object.isLight() || object.type == SceneObject.Type.START_MARKER
                    ? object.color
                    : settings.perPixelLighting ? object.color
                            : applyLighting(object.color, normal, world0, object,
                                    lights, occluders);
            for (int i = 1; i < clipped.size() - 1; i++) {
                Vec3d view0 = clipped.get(0);
                Vec3d view1 = clipped.get(i);
                Vec3d view2 = clipped.get(i + 1);
                fillTriangle(projectToScreen(view0), projectToScreen(view1),
                        projectToScreen(view2), view0, view1, view2,
                        viewToWorld(view0, camera, inverseYaw, inversePitch),
                        viewToWorld(view1, camera, inverseYaw, inversePitch),
                        viewToWorld(view2, camera, inverseYaw, inversePitch),
                        object.color, faceColor, normal,
                        object.isLight() || object.type == SceneObject.Type.START_MARKER
                                ? null : object,
                        objectIndex, lights, occluders);
            }
        }

        if (selected || wireframeOverlayEnabled) {
            int edgeColor = selected ? 0xFFFF30 : 0x8292AA;
            for (int[] edge : edges) {
                Vec3d[] line = clipLineToDepth(viewVertices[edge[0]], viewVertices[edge[1]]);
                if (line != null) {
                    drawDepthLine(projectToScreen(line[0]), projectToScreen(line[1]),
                            edgeColor, objectIndex);
                }
            }
        }
    }

    // Applies ambient and diffuse lighting at a surface sample.
    private int applyLighting(int baseColor, Vec3d normal, Vec3d surface,
            SceneObject receiver, List<LightData> lights,
            List<OccluderData> occluders) {
        return applyLighting(baseColor, normal.x, normal.y, normal.z,
                surface.x, surface.y, surface.z, receiver, lights, occluders);
    }

    private int applyLighting(int baseColor, double normalX, double normalY, double normalZ,
            double surfaceX, double surfaceY, double surfaceZ, SceneObject receiver,
            List<LightData> lights, List<OccluderData> occluders) {
        double red = settings.ambientLight;
        double green = settings.ambientLight;
        double blue = settings.ambientLight;
        for (LightData lightData : lights) {
            SceneObject light = lightData.object;
            double diffuse;
            double towardX;
            double towardY;
            double towardZ;
            double lightDistance;
            if (light.type == SceneObject.Type.DIRECTIONAL_LIGHT) {
                // Sunlight is modeled as a single direction, not a point in space.
                towardX = lightData.direction.x;
                towardY = lightData.direction.y;
                towardZ = lightData.direction.z;
                diffuse = Math.max(0.0, normalX * towardX + normalY * towardY
                        + normalZ * towardZ) * light.intensity;
                lightDistance = Double.POSITIVE_INFINITY;
            } else {
                // A point light spreads in all directions, so its brightness drops as
                // the surface grows farther away from the light source.
                double toLightX = light.transform.position.x - surfaceX;
                double toLightY = light.transform.position.y - surfaceY;
                double toLightZ = light.transform.position.z - surfaceZ;
                double distanceSquared = toLightX * toLightX + toLightY * toLightY
                        + toLightZ * toLightZ;
                lightDistance = Math.sqrt(distanceSquared);
                towardX = distanceSquared == 0.0 ? 0.0 : toLightX / lightDistance;
                towardY = distanceSquared == 0.0 ? 0.0 : toLightY / lightDistance;
                towardZ = distanceSquared == 0.0 ? 0.0 : toLightZ / lightDistance;
                diffuse = distanceSquared == 0.0 ? 0.0
                        : Math.max(0.0, normalX * towardX + normalY * towardY
                                + normalZ * towardZ) * light.intensity
                                        / (1.0 + settings.pointLightFalloff * distanceSquared);
            }
            double visibility = settings.shadowsEnabled && diffuse > 0.0
                    ? shadowVisibility(surfaceX, surfaceY, surfaceZ,
                            towardX, towardY, towardZ, lightDistance,
                            receiver, occluders) : 1.0;
            red += diffuse * visibility * ((light.lightColor >> 16) & 0xFF) / 255.0;
            green += diffuse * visibility * ((light.lightColor >> 8) & 0xFF) / 255.0;
            blue += diffuse * visibility * (light.lightColor & 0xFF) / 255.0;
        }
        int r = shadeChannel((baseColor >> 16) & 0xFF, red);
        int g = shadeChannel((baseColor >> 8) & 0xFF, green);
        int b = shadeChannel(baseColor & 0xFF, blue);
        return (r << 16) | (g << 8) | b;
    }

    private double shadowVisibility(double surfaceX, double surfaceY, double surfaceZ,
            double towardX, double towardY, double towardZ, double lightDistance,
            SceneObject receiver, List<OccluderData> occluders) {
        int samples = settings.shadowSamples;
        if (samples == 1 || settings.shadowSoftness == 0.0) {
            return isOccluded(surfaceX, surfaceY, surfaceZ, towardX, towardY, towardZ,
                    lightDistance, receiver, occluders) ? 0.0 : 1.0;
        }

        double referenceX = Math.abs(towardY) < 0.9 ? 0.0 : 1.0;
        double referenceY = Math.abs(towardY) < 0.9 ? 1.0 : 0.0;
        double referenceZ = 0.0;
        double tangentX = towardY * referenceZ - towardZ * referenceY;
        double tangentY = towardZ * referenceX - towardX * referenceZ;
        double tangentZ = towardX * referenceY - towardY * referenceX;
        double tangentLength = Math.sqrt(tangentX * tangentX
                + tangentY * tangentY + tangentZ * tangentZ);
        tangentX /= tangentLength;
        tangentY /= tangentLength;
        tangentZ /= tangentLength;
        double bitangentX = towardY * tangentZ - towardZ * tangentY;
        double bitangentY = towardZ * tangentX - towardX * tangentZ;
        double bitangentZ = towardX * tangentY - towardY * tangentX;
        double angularRadius = Math.tan(Math.toRadians(settings.shadowSoftness));
        int visibleSamples = 0;
        for (int i = 0; i < samples; i++) {
            double radius = Math.sqrt((i + 0.5) / samples) * angularRadius;
            double angle = i * GOLDEN_ANGLE;
            double tangentOffset = radius * Math.cos(angle);
            double bitangentOffset = radius * Math.sin(angle);
            double sampleX = towardX + tangentX * tangentOffset + bitangentX * bitangentOffset;
            double sampleY = towardY + tangentY * tangentOffset + bitangentY * bitangentOffset;
            double sampleZ = towardZ + tangentZ * tangentOffset + bitangentZ * bitangentOffset;
            double sampleLength = Math.sqrt(sampleX * sampleX + sampleY * sampleY + sampleZ * sampleZ);
            sampleX /= sampleLength;
            sampleY /= sampleLength;
            sampleZ /= sampleLength;
            if (!isOccluded(surfaceX, surfaceY, surfaceZ, sampleX, sampleY, sampleZ,
                    lightDistance, receiver, occluders)) {
                visibleSamples++;
            }
        }
        return (double) visibleSamples / samples;
    }

    private boolean isOccluded(double surfaceX, double surfaceY, double surfaceZ,
            double directionX, double directionY, double directionZ, double lightDistance,
            SceneObject receiver, List<OccluderData> occluders) {
        // Offset the ray from its surface to avoid self-intersection. Point-light
        // rays stop at the light; directional-light rays extend to infinity.
        double originX = surfaceX + directionX * SHADOW_EPSILON;
        double originY = surfaceY + directionY * SHADOW_EPSILON;
        double originZ = surfaceZ + directionZ * SHADOW_EPSILON;
        double maxDistance = Double.isInfinite(lightDistance)
                ? lightDistance : lightDistance - 2.0 * SHADOW_EPSILON;
        if (maxDistance <= 0.0) return false;

        for (OccluderData occluder : occluders) {
            if (occluder.object != receiver
                    && rayIntersectsBox(originX, originY, originZ,
                            directionX, directionY, directionZ, occluder, maxDistance)) {
                return true;
            }
        }
        return false;
    }

    private boolean rayIntersectsBox(double originX, double originY, double originZ,
            double directionX, double directionY, double directionZ,
            OccluderData box, double maxDistance) {
        if (!rayIntersectsBounds(originX, originY, originZ,
                directionX, directionY, directionZ, box, maxDistance)) {
            return false;
        }
        double offsetX = originX - box.position.x;
        double offsetY = originY - box.position.y;
        double offsetZ = originZ - box.position.z;
        double startX = offsetX * box.xx + offsetY * box.xy + offsetZ * box.xz;
        double startY = offsetX * box.yx + offsetY * box.yy + offsetZ * box.yz;
        double startZ = offsetX * box.zx + offsetY * box.zy + offsetZ * box.zz;
        double deltaX = directionX * box.xx + directionY * box.xy + directionZ * box.xz;
        double deltaY = directionX * box.yx + directionY * box.yy + directionZ * box.yz;
        double deltaZ = directionX * box.zx + directionY * box.zy + directionZ * box.zz;
        double entry = 0.0;
        double exit = maxDistance;

        if (deltaX == 0.0) {
            if (startX < -0.5 || startX > 0.5) return false;
        } else {
            double first = (-0.5 - startX) / deltaX;
            double second = (0.5 - startX) / deltaX;
            entry = Math.max(entry, Math.min(first, second));
            exit = Math.min(exit, Math.max(first, second));
            if (entry > exit) return false;
        }
        if (deltaY == 0.0) {
            if (startY < -0.5 || startY > 0.5) return false;
        } else {
            double first = (-0.5 - startY) / deltaY;
            double second = (0.5 - startY) / deltaY;
            entry = Math.max(entry, Math.min(first, second));
            exit = Math.min(exit, Math.max(first, second));
            if (entry > exit) return false;
        }
        if (deltaZ == 0.0) {
            if (startZ < -0.5 || startZ > 0.5) return false;
        } else {
            double first = (-0.5 - startZ) / deltaZ;
            double second = (0.5 - startZ) / deltaZ;
            entry = Math.max(entry, Math.min(first, second));
            exit = Math.min(exit, Math.max(first, second));
            if (entry > exit) return false;
        }
        return exit > SHADOW_EPSILON && entry < maxDistance;
    }

    private boolean rayIntersectsBounds(double originX, double originY, double originZ,
            double directionX, double directionY, double directionZ,
            OccluderData bounds, double maxDistance) {
        double entry = 0.0;
        double exit = maxDistance;
        if (directionX == 0.0) {
            if (originX < bounds.minX || originX > bounds.maxX) return false;
        } else {
            double first = (bounds.minX - originX) / directionX;
            double second = (bounds.maxX - originX) / directionX;
            entry = Math.max(entry, Math.min(first, second));
            exit = Math.min(exit, Math.max(first, second));
            if (entry > exit) return false;
        }
        if (directionY == 0.0) {
            if (originY < bounds.minY || originY > bounds.maxY) return false;
        } else {
            double first = (bounds.minY - originY) / directionY;
            double second = (bounds.maxY - originY) / directionY;
            entry = Math.max(entry, Math.min(first, second));
            exit = Math.min(exit, Math.max(first, second));
            if (entry > exit) return false;
        }
        if (directionZ == 0.0) {
            if (originZ < bounds.minZ || originZ > bounds.maxZ) return false;
        } else {
            double first = (bounds.minZ - originZ) / directionZ;
            double second = (bounds.maxZ - originZ) / directionZ;
            entry = Math.max(entry, Math.min(first, second));
            exit = Math.min(exit, Math.max(first, second));
            if (entry > exit) return false;
        }
        return exit > SHADOW_EPSILON && entry < maxDistance;
    }

    private int shadeChannel(int channel, double intensity) {
        return (int) Math.max(0, Math.min(255, Math.round(channel * intensity)));
    }

    private Vec3d viewToWorld(Vec3d viewPoint, Camera camera,
            Mat4x4 inverseYaw, Mat4x4 inversePitch) {
        return inverseYaw.transformVec3d(inversePitch.transformVec3d(viewPoint))
                .add(camera.position);
    }

    // Clips a polygon against a depth plane.
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

    // Converts a 3D point into screen coordinates. This is where the camera's view
    // volume is mapped onto the visible 2D viewport.
    private Vec3d projectToScreen(Vec3d point) {
        Vec3d projected = projection.transformVec3d(point);
        projected.x = (projected.x + 1.0) * 0.5 * settings.width;
        projected.y = (1.0 - (projected.y + 1.0) * 0.5) * settings.height;
        return projected;
    }

    // For selected objects, we draw their edges after clipping them to the visible
    // depth range so the outline does not render behind the camera or beyond the
    // far plane.
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

    // Lines are clipped against the viewport bounds before drawing so that no edge
    // is painted outside the visible area.
    private Vec3d[] clipLineToViewport(Vec3d start, Vec3d end) {
        double dx = end.x - start.x;
        double dy = end.y - start.y;
        double[] p = {-dx, dx, -dy, dy};
        double[] q = {start.x, settings.width - 1.0 - start.x,
                start.y, settings.height - 1.0 - start.y};
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

    // Fills a triangle pixel by pixel using barycentric weights. This technique lets
    // us know whether a point lies inside the triangle and what its interpolated z
    // depth should be.
    private void fillTriangle(Vec3d p0, Vec3d p1, Vec3d p2,
            Vec3d view0, Vec3d view1, Vec3d view2,
            Vec3d world0, Vec3d world1, Vec3d world2,
            int baseColor, int flatColor, Vec3d normal, SceneObject receiver,
            int objectIndex, List<LightData> lights, List<OccluderData> occluders) {
        int minX = (int) Math.max(0, Math.min(p0.x, Math.min(p1.x, p2.x)));
        int maxX = (int) Math.min(settings.width - 1,
                Math.max(p0.x, Math.max(p1.x, p2.x)));
        int minY = (int) Math.max(0, Math.min(p0.y, Math.min(p1.y, p2.y)));
        int maxY = (int) Math.min(settings.height - 1,
                Math.max(p0.y, Math.max(p1.y, p2.y)));
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
                    int index = x + y * settings.width;
                    if (z < zBuffer[index]) {
                        zBuffer[index] = z;
                        if (settings.perPixelLighting && receiver != null) {
                            // Perspective-correct interpolation keeps shadow rays on the face.
                            double q0 = w0 / view0.z;
                            double q1 = w1 / view1.z;
                            double q2 = w2 / view2.z;
                            double reciprocalDepth = q0 + q1 + q2;
                            double surfaceX = (q0 * world0.x + q1 * world1.x + q2 * world2.x)
                                    / reciprocalDepth;
                            double surfaceY = (q0 * world0.y + q1 * world1.y + q2 * world2.y)
                                    / reciprocalDepth;
                            double surfaceZ = (q0 * world0.z + q1 * world1.z + q2 * world2.z)
                                    / reciprocalDepth;
                            pixels[index] = applyLighting(baseColor,
                                    normal.x, normal.y, normal.z,
                                    surfaceX, surfaceY, surfaceZ,
                                    receiver, lights, occluders);
                        } else {
                            pixels[index] = flatColor;
                        }
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
        if (x < 0 || x >= settings.width || y < 0 || y >= settings.height) return;
        int index = x + y * settings.width;
        if (z <= zBuffer[index] + 0.001) {
            pixels[index] = color;
            objectBuffer[index] = objectIndex;
        }
    }

    // Scales the internal render target to the panel size while leaving the image
    // crisp and using pixel-nearest sampling.
    public void drawScaled(Graphics2D graphics, int x, int y, int width, int height) {
        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        graphics.drawImage(frameBuffer, x, y, width, height, null);
    }
}
