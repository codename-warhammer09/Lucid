package Lucid;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.function.Consumer;
import javax.swing.JPanel;

@SuppressWarnings("serial")
public final class ViewportPanel extends JPanel {
    public enum InputMode {
        SELECT,
        CAMERA
    }

    // Keep the camera slightly outside collider surfaces rather than letting its
    // viewpoint reach the box boundary.
    private static final double CAMERA_RADIUS = 0.2;
    // Prevent floating-point contact from being treated as an overlap.
    private static final double COLLISION_EPSILON = 1e-6;

    // The viewport is the user-facing window into the world. It stores a reference
    // to the scene and camera so frames can be refreshed and interaction can update
    // either without extra plumbing.
    private final Scene scene;
    private final Camera camera;
    private final SceneRenderer renderer;
    private final Runnable selectionChanged;
    private final Runnable objectChanged;
    private final Consumer<InputMode> inputModeChanged;
    private final Consumer<String> showStatus;
    private String selectedId;
    private InputMode inputMode = InputMode.SELECT;
    private boolean clickToCamera = true;
    private boolean keyForward;
    private boolean keyBackward;
    private boolean keyLeft;
    private boolean keyRight;
    private boolean turnLeft;
    private boolean turnRight;
    private boolean lookUp;
    private boolean lookDown;
    private boolean rise;
    private boolean fall;
    private boolean sprint;
    private long lastUpdate = System.nanoTime();

    public ViewportPanel(Scene scene, Camera camera, SceneRenderer renderer,
            Runnable selectionChanged, Runnable objectChanged,
            Consumer<InputMode> inputModeChanged, Consumer<String> showStatus) {
        this.scene = scene;
        this.camera = camera;
        this.renderer = renderer;
        this.selectionChanged = selectionChanged;
        this.objectChanged = objectChanged;
        this.inputModeChanged = inputModeChanged;
        this.showStatus = showStatus;
        setBackground(new Color(0x111522));
        setFocusable(true);
        setPreferredSize(new Dimension(640, 480));
        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent event) {
                requestFocusInWindow();
                if (inputMode == InputMode.SELECT) {
                    if (clickToCamera) setInputMode(InputMode.CAMERA);
                    else selectAt(event.getX(), event.getY());
                }
            }
        });
        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent event) {
                if (inputMode == InputMode.CAMERA && event.getKeyCode() == KeyEvent.VK_R) {
                    camera.reset();
                    repaint();
                } else if (inputMode == InputMode.CAMERA) {
                    setKey(event.getKeyCode(), true);
                } else {
                    transformSelected(event);
                }
            }

            @Override
            public void keyReleased(KeyEvent event) {
                if (inputMode == InputMode.CAMERA) setKey(event.getKeyCode(), false);
            }
        });
        addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent event) {
                clearKeys();
            }
        });
    }

    public void setSelectedId(String id) {
        selectedId = id;
        repaint();
    }

    public String getSelectedId() {
        return selectedId;
    }

    public InputMode getInputMode() {
        return inputMode;
    }

    public void setInputMode(InputMode mode) {
        if (mode == null) throw new IllegalArgumentException("Input mode is required.");
        if (inputMode == mode) return;
        inputMode = mode;
        clearKeys();
        inputModeChanged.accept(mode);
        repaint();
    }

    public void setClickToCamera(boolean enabled) {
        clickToCamera = enabled;
    }

    // Updates the camera based on held keys. Translation is checked against enabled
    // geometry colliders; turning and looking remain unrestricted.
    public void updateCamera() {
        long now = System.nanoTime();
        if (inputMode != InputMode.CAMERA) {
            lastUpdate = now;
            return;
        }
        double delta = Math.min(0.05, (now - lastUpdate) / 1_000_000_000.0);
        lastUpdate = now;
        double speed = (sprint ? 6.0 : 3.0) * delta;
        double turn = 1.8 * delta;
        if (turnLeft) camera.yaw -= turn;
        if (turnRight) camera.yaw += turn;
        camera.yaw = Math.IEEEremainder(camera.yaw, Math.PI * 2.0);
        Vec3d forward = camera.forward();
        Vec3d rightVector = camera.right();
        double moveX = (keyForward ? forward.x : 0.0) - (keyBackward ? forward.x : 0.0)
                - (keyLeft ? rightVector.x : 0.0) + (keyRight ? rightVector.x : 0.0);
        double moveZ = (keyForward ? forward.z : 0.0) - (keyBackward ? forward.z : 0.0)
                - (keyLeft ? rightVector.z : 0.0) + (keyRight ? rightVector.z : 0.0);
        double length = Math.sqrt(moveX * moveX + moveZ * moveZ);
        if (length > 0.0) {
            moveCamera(new Vec3d(moveX / length * speed, 0.0, moveZ / length * speed));
        }
        if (rise) moveCamera(new Vec3d(0.0, speed, 0.0));
        if (fall) moveCamera(new Vec3d(0.0, -speed, 0.0));
        if (lookUp) camera.pitch = Math.max(-Math.PI / 2.0 + 0.01, camera.pitch - turn);
        if (lookDown) camera.pitch = Math.min(Math.PI / 2.0 - 0.01, camera.pitch + turn);
    }

    private void moveCamera(Vec3d movement) {
        // Resolve each world axis independently so blocked motion on one axis does
        // not prevent the camera from sliding along a collider on another.
        moveAlongAxis(0, movement.x);
        moveAlongAxis(1, movement.y);
        moveAlongAxis(2, movement.z);
    }

    private void moveAlongAxis(int axis, double distance) {
        if (distance == 0.0) return;
        Vec3d movement = axis == 0 ? new Vec3d(distance, 0.0, 0.0)
                : axis == 1 ? new Vec3d(0.0, distance, 0.0)
                : new Vec3d(0.0, 0.0, distance);
        double allowedFraction = 1.0;
        for (SceneObject object : scene.getObjects()) {
            if (object.isLight() || !object.collisionEnabled) continue;

            // Expanding the local box by the camera radius is a swept sphere
            // approximation that also accounts for each object's world scale.
            Vec3d start = camera.transformWorldPointToLocal(object, camera.position);
            Vec3d localMovement = camera.transformWorldDirectionToLocal(object, movement);
            Vec3d scale = object.transform.scale;
            double radiusX = CAMERA_RADIUS / scale.x;
            double radiusY = CAMERA_RADIUS / scale.y;
            double radiusZ = CAMERA_RADIUS / scale.z;
            Double collision = segmentBoxEntry(start, localMovement,
                    -0.5 - radiusX, 0.5 + radiusX,
                    -0.5 - radiusY, 0.5 + radiusY,
                    -0.5 - radiusZ, 0.5 + radiusZ);
            if (collision != null) {
                allowedFraction = Math.min(allowedFraction,
                        Math.max(0.0, collision - COLLISION_EPSILON / Math.abs(distance)));
            }
        }
        if (axis == 0) camera.position.x += distance * allowedFraction;
        else if (axis == 1) camera.position.y += distance * allowedFraction;
        else camera.position.z += distance * allowedFraction;
    }

    private Double segmentBoxEntry(Vec3d start, Vec3d movement,
            double minX, double maxX, double minY, double maxY, double minZ, double maxZ) {
        // Slab intersection returns the first movement fraction entering the box,
        // or null when the movement segment does not cross it.
        double[] starts = {start.x, start.y, start.z};
        double[] deltas = {movement.x, movement.y, movement.z};
        double[] mins = {minX, minY, minZ};
        double[] maxs = {maxX, maxY, maxZ};
        boolean startsInside = true;
        boolean endsInside = true;
        for (int axis = 0; axis < 3; axis++) {
            startsInside &= starts[axis] >= mins[axis] && starts[axis] <= maxs[axis];
            double end = starts[axis] + deltas[axis];
            endsInside &= end >= mins[axis] && end <= maxs[axis];
        }
        if (startsInside) return endsInside ? 0.0 : null;

        double entry = 0.0;
        double exit = 1.0;
        for (int axis = 0; axis < 3; axis++) {
            if (deltas[axis] == 0.0) {
                if (starts[axis] < mins[axis] || starts[axis] > maxs[axis]) return null;
                continue;
            }
            double first = (mins[axis] - starts[axis]) / deltas[axis];
            double second = (maxs[axis] - starts[axis]) / deltas[axis];
            entry = Math.max(entry, Math.min(first, second));
            exit = Math.min(exit, Math.max(first, second));
            if (entry > exit) return null;
        }
        return entry <= 1.0 ? entry : null;
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        renderer.render(scene, camera, selectedId);
        int renderWidth = renderer.getWidth();
        int renderHeight = renderer.getHeight();
        double scale = Math.min((double) getWidth() / renderWidth,
                (double) getHeight() / renderHeight);
        int width = (int) (renderWidth * scale);
        int height = (int) (renderHeight * scale);
        int x = (getWidth() - width) / 2;
        int y = (getHeight() - height) / 2;
        renderer.drawScaled((Graphics2D) graphics, x, y, width, height);
    }

    // Converts a mouse click into an object selection by reusing the renderer's
    // objectBuffer. The click coordinates are first mapped from the scaled panel
    // space back to the internal 320x240 render buffer.
    private void selectAt(int x, int y) {
        int renderWidth = renderer.getWidth();
        int renderHeight = renderer.getHeight();
        double scale = Math.min((double) getWidth() / renderWidth,
                (double) getHeight() / renderHeight);
        int width = (int) (renderWidth * scale);
        int height = (int) (renderHeight * scale);
        int left = (getWidth() - width) / 2;
        int top = (getHeight() - height) / 2;
        if (scale <= 0.0 || x < left || x >= left + width || y < top || y >= top + height) {
            return;
        }
        renderer.render(scene, camera, selectedId);
        int imageX = (int) ((x - left) / scale);
        int imageY = (int) ((y - top) / scale);
        String picked = renderer.pick(scene, imageX, imageY);
        if (picked != null || selectedId != null) {
            selectedId = picked;
            selectionChanged.run();
            repaint();
        }
    }

    private void transformSelected(KeyEvent event) {
        SceneObject selected = scene.findById(selectedId);
        if (selected == null) return;

        int axis;
        double direction;
        switch (event.getKeyCode()) {
            case KeyEvent.VK_LEFT: axis = 0; direction = -1.0; break;
            case KeyEvent.VK_RIGHT: axis = 0; direction = 1.0; break;
            case KeyEvent.VK_UP: axis = 1; direction = 1.0; break;
            case KeyEvent.VK_DOWN: axis = 1; direction = -1.0; break;
            case KeyEvent.VK_PAGE_UP: axis = 2; direction = 1.0; break;
            case KeyEvent.VK_PAGE_DOWN: axis = 2; direction = -1.0; break;
            default: return;
        }

        boolean rotate = event.isControlDown();
        boolean scale = event.isAltDown();
        if (rotate && scale) return;
        double step = event.isShiftDown() ? 0.01 : 0.1;
        Transform transform = selected.transform;
        Vec3d target = scale ? transform.scale
                : rotate ? transform.rotation : transform.position;
        double amount = direction * (rotate ? step * 50.0 * Math.PI / 180.0 : step);
        double value = component(target, axis) + amount;
        if (scale) value = Math.max(0.01, value);
        setComponent(target, axis, value);
        transform.validate();
        objectChanged.run();
        showStatus.accept(rotate ? "Rotated " + selected.name + "."
                : scale ? "Scaled " + selected.name + "."
                : "Moved " + selected.name + ".");
    }

    private static double component(Vec3d vector, int axis) {
        return axis == 0 ? vector.x : axis == 1 ? vector.y : vector.z;
    }

    private static void setComponent(Vec3d vector, int axis, double value) {
        if (axis == 0) vector.x = value;
        else if (axis == 1) vector.y = value;
        else vector.z = value;
    }

    // Keyboard state uses a set of booleans (forward, backward, turn left, etc.).
    // These are updated as keys are pressed and released so motion feels smooth and
    // continuous while the camera is controlled in real time.
    private void setKey(int keyCode, boolean pressed) {
        switch (keyCode) {
            case KeyEvent.VK_W: keyForward = pressed; break;
            case KeyEvent.VK_S: keyBackward = pressed; break;
            case KeyEvent.VK_A: keyLeft = pressed; break;
            case KeyEvent.VK_D: keyRight = pressed; break;
            case KeyEvent.VK_LEFT: turnLeft = pressed; break;
            case KeyEvent.VK_RIGHT: turnRight = pressed; break;
            case KeyEvent.VK_UP: lookUp = pressed; break;
            case KeyEvent.VK_DOWN: lookDown = pressed; break;
            case KeyEvent.VK_E: rise = pressed; break;
            case KeyEvent.VK_Q: fall = pressed; break;
            case KeyEvent.VK_SHIFT: sprint = pressed; break;
            default: break;
        }
    }

    private void clearKeys() {
        keyForward = keyBackward = keyLeft = keyRight = false;
        turnLeft = turnRight = lookUp = lookDown = false;
        rise = fall = sprint = false;
    }
}
