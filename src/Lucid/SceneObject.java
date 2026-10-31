package Lucid;

import java.util.UUID;

public final class SceneObject {
    public enum Type {
        CUBE,
        WALL,
        DIRECTIONAL_LIGHT,
        POINT_LIGHT
    }

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

    public static SceneObject create(String name, Type type, Transform transform) {
        int color = type == Type.DIRECTIONAL_LIGHT || type == Type.POINT_LIGHT
                ? 0xFFD060 : 0x00FF88;
        return new SceneObject(UUID.randomUUID().toString(), name, type, transform,
                color, 0xFFFFFF, 1.0);
    }

    public boolean isLight() {
        return type == Type.DIRECTIONAL_LIGHT || type == Type.POINT_LIGHT;
    }
}
