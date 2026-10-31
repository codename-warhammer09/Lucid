// The Lucid Engine, 
// A simple 3D engine written in Java, using only the standard library.
// Developer: Arsal Abbas Mirza

package Lucid;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
// import java.awt.event.KeyListener; Not used currently.
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


public class Lucid extends JPanel implements Runnable {
    // Low Res Internal Render for Retro Feel
    public static final int WIDTH = 320;
    public static final int HEIGHT = 240;
    
    // For Scaling it by 3x so it doesn't appear tiny
    
    public static final int SCALE = 3; 
    private static final double NEAR_PLANE = 0.1;
    private static final double FAR_PLANE = 100.0;
    private static final double MAX_CAMERA_PITCH = Math.PI / 2.0 - 0.01;
    
    // Memory Hook to Screen Pixels 
    
    private final BufferedImage frameBuffer = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
    private final int[] pixels = ((DataBufferInt) frameBuffer.getRaster().getDataBuffer()).getData(); 
    
    // ^ Gets the raster data  for the current image to be displayed.
    private final double[] zBuffer = new double[WIDTH * HEIGHT]; // Z-Buffer Array.
    private boolean running = true; // Our program running flag.
    
    // 3D Projection Matrix and Rotation Angle State
    private Mat4x4 projMatrix;
    // private double rotationAngle = 0.0; // Rotation Angle in Rads. Deprecated 
    
    private static final double AMBIENT_LIGHT = 0.25;
    private static final double KEY_LIGHT_STRENGTH = 0.55;
    private static final double FILL_LIGHT_STRENGTH = 0.30;
    private final Vec3d keyLightDirection = new Vec3d(0.0, 1.0, -1.0).normalize();
    private final Vec3d fillLightDirection = new Vec3d(1.0, 0.0, -1.0).normalize();
    
    // Camera and Input State Variables
    private Vec3d cameraPos = new Vec3d(0.0, 0.0, -5.0); // Positioned 5 units back from Origin
    private double cameraYaw = 0.0; // Yaw (Horizontal Looking Angle) in radians
    private double cameraPitch = 0.0; // Pitch (Vertical tilt) rotation in radians

    // Movement Input Flags
    private volatile boolean keyW = false;
    private volatile boolean keyA = false;
    private volatile boolean keyS = false;
    private volatile boolean keyD = false;
    private volatile boolean keyLeft = false;
    private volatile boolean keyRight = false;
    private volatile boolean keyUp = false;
    private volatile boolean keyDown = false;
    private volatile boolean keyRise = false;
    private volatile boolean keyFall = false;
    private volatile boolean keySprint = false;
    private volatile boolean resetCameraRequested = false;

    private final Vec3d[] boundaryVertices = {
        new Vec3d(-1, -1, -1), new Vec3d(-1, 1, -1),
        new Vec3d(1, -1, -1), new Vec3d(1, 1, -1),
        new Vec3d(-1, -1, 1), new Vec3d(-1, 1, 1),
        new Vec3d(1, -1, 1), new Vec3d(1, 1, 1)
    };
    private final int[][] boundaryEdges = {
        {0, 1}, {0, 2}, {1, 3}, {2, 3},
        {4, 5}, {4, 6}, {5, 7}, {6, 7},
        {0, 4}, {1, 5}, {2, 6}, {3, 7}
    };
    private final Triangle[] worldMesh = new Triangle[] {
        // SOUTH FACE (Z = -1)
        new Triangle(new Vec3d(-1, -1, -1), new Vec3d(-1,  1, -1), new Vec3d( 1,  1, -1), 0x00FF88),
        new Triangle(new Vec3d(-1, -1, -1), new Vec3d( 1,  1, -1), new Vec3d( 1, -1, -1), 0x00FF88),

        // EAST FACE (X = 1)
        new Triangle(new Vec3d( 1, -1, -1), new Vec3d( 1,  1, -1), new Vec3d( 1,  1,  1), 0x00FF88),
        new Triangle(new Vec3d( 1, -1, -1), new Vec3d( 1,  1,  1), new Vec3d( 1, -1,  1), 0x00FF88),

        // NORTH FACE (Z = 1)
        new Triangle(new Vec3d( 1, -1,  1), new Vec3d( 1,  1,  1), new Vec3d(-1,  1,  1), 0x00FF88),
        new Triangle(new Vec3d( 1, -1,  1), new Vec3d(-1,  1,  1), new Vec3d(-1, -1,  1), 0x00FF88),

        // WEST FACE (X = -1)
        new Triangle(new Vec3d(-1, -1,  1), new Vec3d(-1,  1,  1), new Vec3d(-1,  1, -1), 0x00FF88),
        new Triangle(new Vec3d(-1, -1,  1), new Vec3d(-1,  1, -1), new Vec3d(-1, -1, -1), 0x00FF88),

        // TOP FACE (Y = 1)
        new Triangle(new Vec3d(-1,  1, -1), new Vec3d(-1,  1,  1), new Vec3d( 1,  1,  1), 0x00FF88),
        new Triangle(new Vec3d(-1,  1, -1), new Vec3d( 1,  1,  1), new Vec3d( 1,  1, -1), 0x00FF88),

        // BOTTOM FACE (Y = -1)
        new Triangle(new Vec3d( 1, -1,  1), new Vec3d(-1, -1,  1), new Vec3d(-1, -1, -1), 0x00FF88),
        new Triangle(new Vec3d( 1, -1,  1), new Vec3d(-1, -1, -1), new Vec3d( 1, -1, -1), 0x00FF88)
    };

    public Lucid(){
        // The Program's constructor.
        setPreferredSize(new Dimension(WIDTH*SCALE, HEIGHT*SCALE));
        setFocusable(true);
        // Initialize the projection matrix with a 90-degree vertical FOV.
        double aspectRatio = (double) WIDTH / (double) HEIGHT; // Width divided by Height
        projMatrix = Mat4x4.makeProjection(90.0, aspectRatio, NEAR_PLANE, FAR_PLANE);

        // Escape Key Listener that Exits when Esc is pressed
        addKeyListener(new KeyAdapter() {
        @Override 
        public void keyPressed(KeyEvent e){
                setKeyState(e.getKeyCode(), true);
            }
        @Override 
        public void keyReleased(KeyEvent e){
                setKeyState(e.getKeyCode(), false);
            }
        });
        addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                clearMovementKeys();
            }
        });

    }
    // Now a Helper Method to Help Map Codes to our Boolean Flags
    private void setKeyState(int keyCode, boolean isPressed){
        switch (keyCode) {
            case KeyEvent.VK_W: keyW = isPressed; break;
            case KeyEvent.VK_S: keyS = isPressed; break;
            case KeyEvent.VK_A: keyA = isPressed; break;
            case KeyEvent.VK_D: keyD = isPressed; break;
            case KeyEvent.VK_LEFT: keyLeft = isPressed; break;
            case KeyEvent.VK_RIGHT: keyRight = isPressed; break;
            case KeyEvent.VK_UP: keyUp = isPressed; break;
            case KeyEvent.VK_DOWN: keyDown = isPressed; break;
            case KeyEvent.VK_E: keyRise = isPressed; break;
            case KeyEvent.VK_Q: keyFall = isPressed; break;
            case KeyEvent.VK_SHIFT: keySprint = isPressed; break;
            case KeyEvent.VK_R:
                if (isPressed) resetCameraRequested = true;
                break;
            case KeyEvent.VK_ESCAPE: if (isPressed) System.exit(0); break; // Exit on Escape Press.
        
        }
    }
    private void clearMovementKeys() {
        keyW = keyA = keyS = keyD = false;
        keyLeft = keyRight = keyUp = keyDown = false;
        keyRise = keyFall = keySprint = false;
    }
    @Override
    public void run(){
        
        long lastTime = System.nanoTime();
        double nsPerTick = 1000000000.0 / 60.0; // 60 ticks per second
        double delta = 0;
        while (running){
            long now = System.nanoTime();
            delta += (now - lastTime) / nsPerTick;
            lastTime = now;
            while (delta >= 1){
                updateLogic(1.0 / 60.0);
                delta--;
            }
            render();
            repaint();
            try {
                Thread.sleep(2); // Sleep for 2 milliseconds to reduce CPU usage
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
    private void updateLogic(double deltaSeconds){
        if (resetCameraRequested) {
            cameraPos = new Vec3d(0.0, 0.0, -5.0);
            cameraYaw = 0.0;
            cameraPitch = 0.0;
            resetCameraRequested = false;
        }
        double moveSpeed = (keySprint ? 6.0 : 3.0) * deltaSeconds;
        double turnSpeed = 1.8 * deltaSeconds;

        if (keyLeft) cameraYaw -= turnSpeed;
        if (keyRight) cameraYaw += turnSpeed;
        cameraYaw = Math.IEEEremainder(cameraYaw, Math.PI * 2.0);
        Vec3d forward = new Vec3d(Math.sin(cameraYaw), 0, Math.cos(cameraYaw));
        Vec3d right = new Vec3d(Math.cos(cameraYaw), 0, -Math.sin(cameraYaw));

        double moveX = (keyW ? forward.x : 0.0) - (keyS ? forward.x : 0.0)
                - (keyA ? right.x : 0.0) + (keyD ? right.x : 0.0);
        double moveZ = (keyW ? forward.z : 0.0) - (keyS ? forward.z : 0.0)
                - (keyA ? right.z : 0.0) + (keyD ? right.z : 0.0);
        double moveLength = Math.sqrt(moveX * moveX + moveZ * moveZ);
        if (moveLength > 0.0) {
            cameraPos.x += moveX / moveLength * moveSpeed;
            cameraPos.z += moveZ / moveLength * moveSpeed;
        }
        if (keyRise) cameraPos.y += moveSpeed;
        if (keyFall) cameraPos.y -= moveSpeed;

        if (keyUp) cameraPitch = Math.max(-MAX_CAMERA_PITCH, cameraPitch - turnSpeed);
        if (keyDown) cameraPitch = Math.min(MAX_CAMERA_PITCH, cameraPitch + turnSpeed);

        // The game's state and camera updates 
        // Incrementing the Rotation Angle over Time
        // rotationAngle += 0.02; // Incrementing the rotation angle for continuous rotation.
        // Deprecated.
    }
    private synchronized void render(){
   // Clear screen pixels and reset Z-Buffer depth storage to infinity
        Arrays.fill(pixels, 0x111522);
        Arrays.fill(zBuffer, Double.MAX_VALUE);

        // Build inverse camera rotation matrices for transforming world vertices into camera space
        Mat4x4 yawMatrix = Mat4x4.makeRotationY(cameraYaw);
        Mat4x4 pitchMatrix = Mat4x4.makeRotationX(-cameraPitch);

        // Iterate through every triangle in our world mesh
        for (Triangle tri : worldMesh) {
            // Now we Transform World Vertices into Camera Space
            // Transform World Vertices into Camera Space:
            // 1. tri.p[0].sub(cameraPos) -> Translate vertex relative to camera position
            // 2. yawMatrix -> Apply horizontal left/right turn
            // 3. pitchMatrix -> Apply vertical up/down tilt
            Vec3d v0 = pitchMatrix.transformVec3d(yawMatrix.transformVec3d(tri.p[0].sub(cameraPos)));
            Vec3d v1 = pitchMatrix.transformVec3d(yawMatrix.transformVec3d(tri.p[1].sub(cameraPos)));
            Vec3d v2 = pitchMatrix.transformVec3d(yawMatrix.transformVec3d(tri.p[2].sub(cameraPos)));
            // Keep lighting fixed in world space as the camera moves.
            Vec3d line1 = tri.p[1].sub(tri.p[0]);
            Vec3d line2 = tri.p[2].sub(tri.p[0]);
            Vec3d normal = line1.cross(line2).normalize();
            Vec3d cameraRay = tri.p[0].sub(cameraPos);
            
            // If dot product is negative, triangle faces the camera
            if (normal.dot(cameraRay) < 0.0) {

                double lightIntensity = calculateLightIntensity(normal);
                int shadedColor = applyShading(tri.color, lightIntensity);

                List<Vec3d> clipped = new ArrayList<>();
                clipped.add(v0);
                clipped.add(v1);
                clipped.add(v2);
                clipped = clipPolygon(clipped, NEAR_PLANE, true);
                clipped = clipPolygon(clipped, FAR_PLANE, false);
                for (int i = 1; i < clipped.size() - 1; i++) {
                    fillTriangle(projectToScreen(clipped.get(0)),
                            projectToScreen(clipped.get(i)),
                            projectToScreen(clipped.get(i + 1)), shadedColor);
                }
            }
        }
        drawSceneBoundary(yawMatrix, pitchMatrix);
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
        Vec3d projected = projMatrix.transformVec3d(point);
        projected.x = (projected.x + 1.0) * 0.5 * WIDTH;
        projected.y = (1.0 - (projected.y + 1.0) * 0.5) * HEIGHT;
        return projected;
    }
    private void drawSceneBoundary(Mat4x4 yawMatrix, Mat4x4 pitchMatrix) {
        Vec3d[] cameraVertices = new Vec3d[boundaryVertices.length];
        for (int i = 0; i < boundaryVertices.length; i++) {
            Vec3d relative = boundaryVertices[i].sub(cameraPos);
            cameraVertices[i] = pitchMatrix.transformVec3d(yawMatrix.transformVec3d(relative));
        }
        for (int[] edge : boundaryEdges) {
            Vec3d start = cameraVertices[edge[0]];
            Vec3d end = cameraVertices[edge[1]];
            Vec3d[] clipped = clipLineToDepth(start, end);
            if (clipped == null) continue;
            drawDepthLine(projectToScreen(clipped[0]), projectToScreen(clipped[1]), 0xC8FFE8);
        }
    }
    private Vec3d[] clipLineToDepth(Vec3d start, Vec3d end) {
        Vec3d[] clipped = clipLineAgainstPlane(start, end, NEAR_PLANE, true);
        if (clipped == null) return null;
        return clipLineAgainstPlane(clipped[0], clipped[1], FAR_PLANE, false);
    }
    private Vec3d[] clipLineAgainstPlane(Vec3d start, Vec3d end, double plane, boolean keepGreater) {
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
    private void drawDepthLine(Vec3d start, Vec3d end, int color) {
        Vec3d[] viewportLine = clipLineToViewport(start, end);
        if (viewportLine == null) return;
        start = viewportLine[0];
        end = viewportLine[1];
        double dx = end.x - start.x;
        double dy = end.y - start.y;
        int steps = (int) Math.ceil(Math.max(Math.abs(dx), Math.abs(dy)));
        if (steps == 0) {
            drawDepthPixel((int) Math.round(start.x), (int) Math.round(start.y), start.z, color);
            return;
        }
        for (int i = 0; i <= steps; i++) {
            double t = (double) i / steps;
            int x = (int) Math.round(start.x + dx * t);
            int y = (int) Math.round(start.y + dy * t);
            double z = start.z + (end.z - start.z) * t;
            drawDepthPixel(x, y, z, color);
        }
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
            if (p[i] < 0.0) {
                tStart = Math.max(tStart, t);
            } else {
                tEnd = Math.min(tEnd, t);
            }
            if (tStart > tEnd) return null;
        }
        Vec3d delta = end.sub(start);
        return new Vec3d[] {
            start.add(delta.mul(tStart)),
            start.add(delta.mul(tEnd))
        };
    }
    private void drawDepthPixel(int x, int y, double z, int color) {
        if (x < 0 || x >= WIDTH || y < 0 || y >= HEIGHT) return;
        int index = x + y * WIDTH;
        if (z <= zBuffer[index] + 0.001) {
            pixels[index] = color;
            zBuffer[index] = Math.min(zBuffer[index], z);
        }
    }
    public void drawPixel(int x, int y, int colorHex){
        // This draws a colored Pixel, duh; Conditions applied to prevent Crashes.
        if(x >= 0 && x < WIDTH && y >= 0 && y < HEIGHT){
            pixels[x+y*WIDTH] = colorHex;
        }
    }
    // 2D Edge Function for Half-Space Rasterization
    private double edgeFunction(Vec3d a, Vec3d b, double px, double py) {
        return (px - a.x) * (b.y - a.y) - (py - a.y) * (b.x - a.x);
    }
    // An Implementation of Bresenham's Line Algorithm to draw lines between two points.
    // That uses only integer math! It is a classic algorithm from 1965.
// public void drawLine(int x0, int y0, int x1, int y1, int colorHex){
   //     int dx = Math.abs(x1 - x0);
    //   int dy = Math.abs(y1 - y0);
    //  int sx = x0 < x1 ? 1 : -1;
    //  int sy = y0 < y1 ? 1 : -1;
    //  int err = dx - dy;
    //  while (true) {
    //      drawPixel(x0, y0, colorHex);
    //      if (x0 == x1 && y0 == y1) break;
    //      int err2 = err * 2;
    //      if (err2 > -dy) {
    //          err -= dy;
    //          x0 += sx;
    //      }
    //      if (err2 < dx) {
    //          err += dx;
    //          y0 += sy;
    //      }
    //  }
    // }
    // Barycentric Triangle Rasterizer with Z-Buffering
    public void fillTriangle(Vec3d p0, Vec3d p1, Vec3d p2, int colorHex) {
        // Compute Bounding Box of Triangle on screen
        int minX = (int) Math.max(0, Math.min(p0.x, Math.min(p1.x, p2.x)));
        int maxX = (int) Math.min(WIDTH - 1, Math.max(p0.x, Math.max(p1.x, p2.x)));
        int minY = (int) Math.max(0, Math.min(p0.y, Math.min(p1.y, p2.y)));
        int maxY = (int) Math.min(HEIGHT - 1, Math.max(p0.y, Math.max(p1.y, p2.y)));

        // Total signed area factor of triangle
        double area = edgeFunction(p0, p1, p2.x, p2.y);
        if (area == 0) return; // Degenerate triangle

        // Loop over Bounding Box pixels
        for (int y = minY; y <= maxY; y++) {
            for (int x = minX; x <= maxX; x++) {
                double pixelX = x + 0.5;
                double pixelY = y + 0.5;

                // Evaluate Edge Functions
                double w0 = edgeFunction(p1, p2, pixelX, pixelY);
                double w1 = edgeFunction(p2, p0, pixelX, pixelY);
                double w2 = edgeFunction(p0, p1, pixelX, pixelY);

                // Check if pixel lies inside triangle boundaries
                if ((w0 >= 0 && w1 >= 0 && w2 >= 0) || (w0 <= 0 && w1 <= 0 && w2 <= 0)) {
                    // Normalize barycentric weights
                    w0 /= area;
                    w1 /= area;
                    w2 /= area;

                    // Interpolate depth Z at pixel location
                    double z = w0 * p0.z + w1 * p1.z + w2 * p2.z;

                    int index = x + y * WIDTH;

                    // Z-Buffer Depth Test
                    if (z < zBuffer[index]) {
                        zBuffer[index] = z; // Update depth buffer
                        pixels[index] = colorHex; // Write pixel color
                    }
                }
            }
        }
    }

    // Utility: Multiplies RGB color components by lighting intensity
    private double calculateLightIntensity(Vec3d normal) {
        double keyLight = Math.max(0.0, normal.dot(keyLightDirection));
        double fillLight = Math.max(0.0, normal.dot(fillLightDirection));
        return Math.min(1.0, AMBIENT_LIGHT
                + KEY_LIGHT_STRENGTH * keyLight
                + FILL_LIGHT_STRENGTH * fillLight);
    }

    private int applyShading(int colorHex, double intensity) {
        int r = (int) (((colorHex >> 16) & 0xFF) * intensity);
        int g = (int) (((colorHex >> 8) & 0xFF) * intensity);
        int b = (int) ((colorHex & 0xFF) * intensity);
        return (r << 16) | (g << 8) | b;
    }
    @Override 
    protected synchronized void paintComponent(Graphics g){
    super.paintComponent(g); // To constantly clear the screen.
    g.drawImage(frameBuffer, 0, 0, WIDTH*SCALE, HEIGHT*SCALE, null); // Draws the scaled render to the screen.
    }
    public static void main(String[] args) throws Exception {
            SwingUtilities.invokeLater(() -> {
                // Creating the Basic Frame
                JFrame frame = new JFrame("Lucid Engine");
                // Calling the constructor for the class itself that sets all the necessary stuff into place.
                Lucid lucidGame = new Lucid();
                // Now some JFrame settings below:
                frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                frame.setResizable(false);
                frame.add(lucidGame); // Passing our Class Object to it.
                frame.pack(); // Gotta pack em' frames.
                frame.setLocationRelativeTo(null); // Centered 
                frame.setVisible(true); // We gotta see it, duh!
                lucidGame.requestFocusInWindow();

                // Now to actually start this baby up.
                new Thread(lucidGame).start();
            });
    }
}