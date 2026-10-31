package Lucid;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Properties;

public final class SceneIO {
    private static final String FORMAT_VERSION = "1";
    private static final int MAX_OBJECTS = 10000;

    private SceneIO() {}

    public static void save(Scene scene, Path path) throws IOException {
        Properties properties = new Properties();
        properties.setProperty("lucid.scene.version", FORMAT_VERSION);
        List<SceneObject> objects = scene.getObjects();
        properties.setProperty("scene.objectCount", Integer.toString(objects.size()));
        for (int i = 0; i < objects.size(); i++) {
            SceneObject object = objects.get(i);
            String prefix = "object." + i + ".";
            properties.setProperty(prefix + "id", object.id);
            properties.setProperty(prefix + "name", object.name);
            properties.setProperty(prefix + "type", object.type.name());
            properties.setProperty(prefix + "position", vector(object.transform.position, false));
            properties.setProperty(prefix + "rotationDegrees", vector(object.transform.rotation, true));
            properties.setProperty(prefix + "scale", vector(object.transform.scale, false));
            properties.setProperty(prefix + "color", hex(object.color));
            properties.setProperty(prefix + "lightColor", hex(object.lightColor));
            properties.setProperty(prefix + "intensity", Double.toString(object.intensity));
        }
        try (OutputStream output = Files.newOutputStream(path)) {
            properties.store(output, "Lucid scene; rotation values are in degrees");
        }
    }

    public static Scene load(Path path) throws IOException {
        Properties properties = new Properties();
        try (InputStream input = Files.newInputStream(path)) {
            properties.load(input);
        }
        if (!FORMAT_VERSION.equals(properties.getProperty("lucid.scene.version"))) {
            throw new IOException("Unsupported or missing Lucid scene format version.");
        }
        int count = parseInt(required(properties, "scene.objectCount"), "scene.objectCount");
        if (count < 0 || count > MAX_OBJECTS) {
            throw new IOException("Scene object count must be between 0 and " + MAX_OBJECTS + ".");
        }

        Scene scene = new Scene();
        for (int i = 0; i < count; i++) {
            String prefix = "object." + i + ".";
            try {
                SceneObject.Type type = SceneObject.Type.valueOf(required(properties, prefix + "type"));
                Vec3d position = parseVector(required(properties, prefix + "position"), prefix + "position");
                Vec3d rotation = parseVector(required(properties, prefix + "rotationDegrees"),
                        prefix + "rotationDegrees").mul(Math.PI / 180.0);
                Vec3d scale = parseVector(required(properties, prefix + "scale"), prefix + "scale");
                int color = parseColor(required(properties, prefix + "color"), prefix + "color");
                int lightColor = parseColor(required(properties, prefix + "lightColor"),
                        prefix + "lightColor");
                double intensity = parseDouble(required(properties, prefix + "intensity"),
                        prefix + "intensity");
                SceneObject object = new SceneObject(required(properties, prefix + "id"),
                        required(properties, prefix + "name"), type,
                        new Transform(position, rotation, scale), color, lightColor, intensity);
                scene.add(object);
            } catch (IllegalArgumentException ex) {
                throw new IOException("Invalid scene data for object " + i + ": "
                        + ex.getMessage(), ex);
            }
        }
        return scene;
    }

    private static String required(Properties properties, String key) throws IOException {
        String value = properties.getProperty(key);
        if (value == null || value.trim().isEmpty()) {
            throw new IOException("Missing required scene property: " + key);
        }
        return value.trim();
    }

    private static Vec3d parseVector(String value, String key) throws IOException {
        String[] parts = value.split(",", -1);
        if (parts.length != 3) throw new IOException(key + " must contain three comma-separated values.");
        return new Vec3d(parseDouble(parts[0], key), parseDouble(parts[1], key),
                parseDouble(parts[2], key));
    }

    private static String vector(Vec3d value, boolean degrees) {
        double conversion = degrees ? 180.0 / Math.PI : 1.0;
        return Double.toString(value.x * conversion) + ","
                + Double.toString(value.y * conversion) + ","
                + Double.toString(value.z * conversion);
    }

    private static int parseColor(String value, String key) throws IOException {
        String digits = value.startsWith("#") ? value.substring(1) : value;
        if (!digits.matches("[0-9a-fA-F]{6}")) {
            throw new IOException(key + " must be a six-digit RGB hex value.");
        }
        return Integer.parseInt(digits, 16);
    }

    private static String hex(int color) {
        return String.format("#%06X", color & 0xFFFFFF);
    }

    private static int parseInt(String value, String key) throws IOException {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ex) {
            throw new IOException(key + " must be an integer.", ex);
        }
    }

    private static double parseDouble(String value, String key) throws IOException {
        try {
            double result = Double.parseDouble(value.trim());
            if (!Double.isFinite(result)) throw new NumberFormatException("not finite");
            return result;
        } catch (NumberFormatException ex) {
            throw new IOException(key + " must be a finite number.", ex);
        }
    }
}
