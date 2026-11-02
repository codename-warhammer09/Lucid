package Lucid;

import java.util.prefs.Preferences;

/**
 * Editor-wide renderer options. These settings are kept separate from scene data.
 */
public final class RenderSettings {
    private static final int[][] RESOLUTIONS = {
        {320, 240},
        {640, 480},
        {960, 720}
    };

    public final double ambientLight;
    public final boolean shadowsEnabled;
    public final double fieldOfView;
    public final double pointLightFalloff;
    public final int width;
    public final int height;

    public RenderSettings(double ambientLight, boolean shadowsEnabled, double fieldOfView,
            double pointLightFalloff, int width, int height) {
        if (!Double.isFinite(ambientLight) || ambientLight < 0.0 || ambientLight > 1.0) {
            throw new IllegalArgumentException("Ambient light must be between 0 and 1.");
        }
        if (!Double.isFinite(fieldOfView) || fieldOfView < 45.0 || fieldOfView > 120.0) {
            throw new IllegalArgumentException("Field of view must be between 45 and 120 degrees.");
        }
        if (!Double.isFinite(pointLightFalloff)
                || pointLightFalloff < 0.0 || pointLightFalloff > 0.5) {
            throw new IllegalArgumentException("Point-light falloff must be between 0 and 0.5.");
        }
        if (!isSupportedResolution(width, height)) {
            throw new IllegalArgumentException("Unsupported internal render resolution.");
        }
        this.ambientLight = ambientLight;
        this.shadowsEnabled = shadowsEnabled;
        this.fieldOfView = fieldOfView;
        this.pointLightFalloff = pointLightFalloff;
        this.width = width;
        this.height = height;
    }

    public static RenderSettings defaults() {
        return new RenderSettings(0.22, true, 90.0, 0.12, 320, 240);
    }

    public static RenderSettings load(Preferences preferences) {
        RenderSettings defaults = defaults();
        return new RenderSettings(
                preferences.getDouble("ambientLight", defaults.ambientLight),
                preferences.getBoolean("shadowsEnabled", defaults.shadowsEnabled),
                preferences.getDouble("fieldOfView", defaults.fieldOfView),
                preferences.getDouble("pointLightFalloff", defaults.pointLightFalloff),
                preferences.getInt("renderWidth", defaults.width),
                preferences.getInt("renderHeight", defaults.height));
    }

    public void save(Preferences preferences) throws java.util.prefs.BackingStoreException {
        preferences.putDouble("ambientLight", ambientLight);
        preferences.putBoolean("shadowsEnabled", shadowsEnabled);
        preferences.putDouble("fieldOfView", fieldOfView);
        preferences.putDouble("pointLightFalloff", pointLightFalloff);
        preferences.putInt("renderWidth", width);
        preferences.putInt("renderHeight", height);
        preferences.flush();
    }

    public int getResolutionIndex() {
        for (int i = 0; i < RESOLUTIONS.length; i++) {
            if (RESOLUTIONS[i][0] == width && RESOLUTIONS[i][1] == height) return i;
        }
        throw new IllegalStateException("Render resolution is not supported.");
    }

    public static int getResolutionWidth(int index) {
        return RESOLUTIONS[index][0];
    }

    public static int getResolutionHeight(int index) {
        return RESOLUTIONS[index][1];
    }

    private static boolean isSupportedResolution(int width, int height) {
        for (int[] resolution : RESOLUTIONS) {
            if (resolution[0] == width && resolution[1] == height) return true;
        }
        return false;
    }
}
