// LUCID ENGINE
// A 3D Engine Project
// Developed by: Arsal Abbas Mirza
// The project is intended to grow into a full game engine.
package Lucid;

/**
 * This is the very small entry point for the application. In practice it simply
 * hands control to the editor, which is where the scene is created, edited, and
 * rendered on-screen.
 *
 * The project is intentionally lightweight: the editor builds scenes from
 * transformed objects, renders opaque-mesh light occlusion, and lets the camera
 * collide with objects whose collision property is enabled. The real work is split
 * between the math helpers, scene data model, and rendering pipeline.
 */
public final class Lucid {
    private Lucid() {}

    public static void main(String[] args) {
        SceneEditor.main(args);
    }
}
