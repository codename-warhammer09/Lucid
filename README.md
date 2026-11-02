# Lucid — 3D Engine Project

Lucid is an evolving, dependency-free 3D engine project written in Java. Its
current development state is a small scene editor backed by a software
renderer. It provides a starting point for building scenes from reusable
templates; it is not intended to represent the finished scope of the engine.

## Current development state

The editor currently includes:

- An initially empty scene with cube, thick-wall, directional-light, and
  point-light templates.
- A software-rendered viewport with perspective projection, near/far clipping,
  depth buffering, ambient and directional/point lighting with opaque-mesh
  shadow occlusion.
- A scene object list, viewport selection, and a visible outline for the
  selected object, with explicit Select and Camera input modes.
- An inspector for object name, position, rotation, scale, color, and optional
  camera collision. Light objects also expose light color and intensity.
- Editor-wide render settings for ambient lighting, shadows, point-light falloff,
  field of view, and internal render resolution. These settings are stored as
  user preferences, separately from scene files.
- Scene creation, opening, and saving in a versioned `.lucid` text format.

The implementation and controls below describe what exists now and will evolve
as engine development continues.

## Requirements

- A Java Development Kit (JDK) with `java` and `javac` on your `PATH`.
- A desktop environment that can display Swing windows.

Lucid uses only Java's standard library. No third-party dependencies, build
tools, or editor extensions are required.

## Build and run

Open PowerShell in the project directory (the directory containing this
README), then compile and launch:

```powershell
New-Item -ItemType Directory -Force bin | Out-Null
javac -d bin src\Lucid\*.java
java -cp bin Lucid.Lucid
```

The `Lucid.Lucid` entry point opens the integrated scene editor.

## Editor workflow

1. Use **Add Cube**, **Add Wall**, **Add Directional Light**, or **Add Point
   Light** to add an object to the initially empty scene.
2. Use **Select** mode and turn off **Click to Camera** to select objects by
   clicking them in the viewport; objects can also be selected from the scene
   list. The selection is outlined and its values appear in the Properties panel.
   With **Click to Camera** enabled (the default), a viewport click enters Camera
   mode; use **Select** in the toolbar to return.
3. Edit the values and choose **Apply**. Positions and scales use world units;
   rotations are entered in degrees. Object colors accept six hexadecimal RGB
   digits, with or without a leading `#`. Enable **Camera collision enabled**
   on a cube or wall to stop the camera at its surface; it is off by default.
4. Choose **File → Save**, **Save as...**, or **Open...** to persist and restore
   scenes. Opening a scene validates the file before replacing the current
   scene. Invalid files are reported without discarding the current scene.
5. Open **View → Render settings...** to adjust the viewport renderer. Settings
   apply immediately when accepted and persist across editor launches; they are
   not written into `.lucid` scene files.

The wall template is a box with a rectangular footprint and a small but
non-zero thickness; its dimensions can be adjusted with the scale fields.
Directional lights use their rotation to aim the light. Point lights use
their position, color, and intensity to illuminate nearby faces.
The Render Settings panel controls global ambient light, point-light falloff,
and whether opaque geometry casts shadows. Field of view and internal resolution
are also adjustable; higher resolutions improve detail at a higher software
rendering cost.

## Current controls

Use **Select** and **Camera** in the toolbar to choose the viewport input mode.
Clicking the viewport enters Camera mode when **Click to Camera** is enabled;
when disabled, clicking selects an object. In Select mode, click the Select
toolbar button to focus the viewport for keyboard transforms. Keyboard shortcuts
do not act while editing a text field.

| Key | Action |
| --- | --- |
| Camera mode: `W` / `S` | Move forward / backward |
| Camera mode: `A` / `D` | Strafe left / right |
| Camera mode: `Left` / `Right` | Turn left / right |
| Camera mode: `Up` / `Down` | Look up / down |
| Camera mode: `E` / `Q` | Move up / down |
| Camera mode: `Shift` | Move faster while held |
| Camera mode: `R` | Reset the camera |
| Select mode: `Left` / `Right` | Move selected object along X |
| Select mode: `Up` / `Down` | Move selected object along Y |
| Select mode: `Page Up` / `Page Down` | Move selected object along Z |
| Select mode: `Ctrl` + transform key | Rotate around the corresponding world axis |
| Select mode: `Alt` + transform key | Scale along the corresponding world axis |
| Select mode: `Shift` + transform key | Use a finer adjustment step |
| `P` | Focus the selected object's Properties |
| `Delete` | Delete the selected object |
| `Esc` | Close the editor |

Transform keyboard steps are 0.1 world units for movement and scale and 5
degrees for rotation; hold Shift for 0.01 units or 0.5 degrees. The inspector
remains available for precise transform values.

## Suggested QoL improvements

Potential follow-ups, in priority order:

1. Undo/redo for object creation, deletion, and edits.
2. Duplicate the selected object and frame/focus the viewport on it.
3. Optional position/rotation/scale snapping and configurable transform steps.
4. Unsaved-scene indicators and prompts before discarding changes.
5. Inspector reset/revert actions and an in-editor shortcut reference.

## Scene file format

``.lucid` files are versioned Java Properties text files. Object entries store
their ID, name, template type, transform, raster color, light settings, and
camera-collision setting. Rotation values in the file are in degrees. Files
without the collision property remain valid and default to collision off. The
format is designed to be readable and does not require external serialization
libraries.

## Project layout

```text
src/
  Lucid/
    Lucid.java          Application entry point
    SceneEditor.java    Editor window, scene list, templates, and file actions
    ViewportPanel.java  Viewport input, camera movement, and display
    PropertiesPanel.java Object inspector and validation
    SceneRenderer.java  Software rendering, lighting, highlighting, and picking
    RenderSettings.java Editor-wide render settings and preference persistence
    Scene.java          Scene object collection
    SceneObject.java    Object type, identity, material, and light properties
    Transform.java      Position, rotation, and scale
    MeshFactory.java    Reusable cube and wall geometry
    SceneIO.java        Versioned scene persistence
    Camera.java         Camera state and object transforms
    Mat4x4.java         Matrix and projection operations
    Triangle.java       Triangle data
    Vec3d.java          3D vector operations
lib/                    Optional location for external JARs; none are required
bin/                    Compiled class output
.vscode/                 VS Code Java project settings
```
