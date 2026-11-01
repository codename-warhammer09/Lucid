package Lucid;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The scene is the world container for all objects that should be rendered.
 *
 * In a simple engine like this, a scene is just a list of objects plus a few
 * convenient lookup methods. The data structure is intentionally small, which
 * makes it easy to add objects, remove them, search by ID, and replace the whole
 * scene when loading a file.
 */
public final class Scene {
    private final List<SceneObject> objects = new ArrayList<>();

    // We expose a read-only view so callers cannot accidentally mutate the scene
    // without going through the proper add/remove methods.
    public List<SceneObject> getObjects() {
        return Collections.unmodifiableList(objects);
    }

    // Adds a new object only if it is not null and not already present.
    public void add(SceneObject object) {
        if (object == null) {
            throw new IllegalArgumentException("Scene object cannot be null.");
        }
        if (findById(object.id) != null) {
            throw new IllegalArgumentException("Scene already contains object ID " + object.id + ".");
        }
        objects.add(object);
    }

    // Removes an object by its unique identifier.
    public boolean remove(String id) {
        return objects.removeIf(object -> object.id.equals(id));
    }

    // Finds an object by ID with a simple linear scan. The scene is small enough
    // that this is straightforward and avoids unnecessary complexity.
    public SceneObject findById(String id) {
        if (id == null) return null;
        for (SceneObject object : objects) {
            if (object.id.equals(id)) return object;
        }
        return null;
    }

    // Replaces the scene contents with a new set of objects while preserving the
    // validity checks of the scene itself.
    public void replaceWith(List<SceneObject> replacement) {
        Scene validated = new Scene();
        for (SceneObject object : replacement) validated.add(object);
        objects.clear();
        objects.addAll(validated.objects);
    }
}
