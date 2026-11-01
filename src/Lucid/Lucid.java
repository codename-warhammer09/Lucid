// LUCID ENGINE
// A 3D Engine Project
// Developed by: Arsal Abbas Mirza
package Lucid;

/**
 * This is the very small entry point for the application. In practice it simply
 * hands control to the editor, which is where the scene is created, edited, and
 * rendered on-screen.
 *
 * The project is intentionally lightweight: the real work is split between the
 * math helpers, the scene data model, and the rendering pipeline.
 */
public final class Lucid {
    private Lucid() {}

    public static void main(String[] args) {
        SceneEditor.main(args);
    }
}
