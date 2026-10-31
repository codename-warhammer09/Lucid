package Lucid;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class Scene {
    private final List<SceneObject> objects = new ArrayList<>();

    public List<SceneObject> getObjects() {
        return Collections.unmodifiableList(objects);
    }

    public void add(SceneObject object) {
        if (object == null) {
            throw new IllegalArgumentException("Scene object cannot be null.");
        }
        if (findById(object.id) != null) {
            throw new IllegalArgumentException("Scene already contains object ID " + object.id + ".");
        }
        objects.add(object);
    }

    public boolean remove(String id) {
        return objects.removeIf(object -> object.id.equals(id));
    }

    public SceneObject findById(String id) {
        if (id == null) return null;
        for (SceneObject object : objects) {
            if (object.id.equals(id)) return object;
        }
        return null;
    }

    public void replaceWith(List<SceneObject> replacement) {
        Scene validated = new Scene();
        for (SceneObject object : replacement) validated.add(object);
        objects.clear();
        objects.addAll(validated.objects);
    }
}
