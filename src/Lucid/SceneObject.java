package Lucid;

import java.util.UUID;

/**
 * A single item in the scene.
 *
 * The engine's world is built from distinct objects, each with a name, a type,
 * a transform, and sometimes a light profile. This class acts as the basic record
 * for every actor in the scene, from cubes and walls to point lights and sunlight.
 */
public final class SceneObject {
    public enum Type {
        CUBE,
        WALL,
        DIRECTIONAL_LIGHT,
        POINT_LIGHT
    }

    // Objects are identified by a unique string so they can be selected,
    // manipulated, and serialized reliably.
    public final String id;
    public String name;
    public final Type type;
    public Transform transform;
    public int color;
    public int lightColor;
    public double intensity;

    public SceneObject(String id, String name, Type type, Transform transform,
            int color, int lightColor, double intensity) {
        if (id == null || id.trim().isEmpty()) {
            throw new IllegalArgumentException("Object ID cannot be empty.");
        }
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Object name cannot be empty.");
        }
        if (type == null || transform == null) {
            throw new IllegalArgumentException("Object type and transform are required.");
        }
        if ((color & 0xFF000000) != 0 || (lightColor & 0xFF000000) != 0) {
            throw new IllegalArgumentException("Colors must be 24-bit RGB values.");
        }
        if (!Double.isFinite(intensity) || intensity < 0.0 || intensity > 8.0) {
            throw new IllegalArgumentException("Light intensity must be between 0 and 8.");
        }
        transform.validate();
        this.id = id;
        this.name = name.trim();
        this.type = type;
        this.transform = transform;
        this.color = color;
        this.lightColor = lightColor;
        this.intensity = intensity;
    }

    // Factory method for creating an object with a generated ID and sensible default
    // visual values. Light objects get a warm yellow/orange color, while regular
    // scene objects default to a green tone.
    public static SceneObject create(String name, Type type, Transform transform) {
        int color = type == Type.DIRECTIONAL_LIGHT || type == Type.POINT_LIGHT
                ? 0xFFD060 : 0x00FF88;
        return new SceneObject(UUID.randomUUID().toString(), name, type, transform,
                color, 0xFFFFFF, 1.0);
    }

    // Some objects contribute light; others are just geometry. This helper lets the
    // renderer quickly separate those concerns without inspecting every object in
    // the scene manually.
    public boolean isLight() {
        return type == Type.DIRECTIONAL_LIGHT || type == Type.POINT_LIGHT;
    }
}
