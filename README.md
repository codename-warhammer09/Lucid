# Lucid — 3D Engine Project

Lucid is an evolving, dependency-free 3D engine project written in Java. Its
current development state is a small scene editor backed by a software
renderer. It provides a starting point for building scenes from reusable
templates; it is not intended to represent the finished scope of the engine.

## Current development state

The editor currently includes:

- A startup window with options to start blank, load a built-in template, load
  an existing scene, or open the project documentation.
- An initially empty scene with cube, thick-wall, directional-light, and
  point-light templates.
- Two point-lit `.lucid` examples in `templates/`: a wide flat plain and a
  roofed room/maze interior. User scenes placed in `scenes/` are listed at
  startup.
- A software-rendered viewport with perspective projection, near/far clipping,
  depth buffering, ambient and directional/point lighting with per-pixel
  diffuse shading and multi-sample opaque-mesh shadows.
- A scene object list, viewport selection, and a visible outline for the
  selected object, with explicit Select and Camera input modes.
- An inspector for object name, position, rotation, scale, color, and optional
  camera collision. Light objects also expose light color and intensity.
- Editor-wide render settings for per-face/per-pixel lighting, shadow softness
  and quality, ambient lighting, point-light falloff, field of view, and internal
  render resolution. These settings are stored as user preferences, separately
  from scene files.
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

The `Lucid.Lucid` entry point opens the startup window. Choose **Start from
Blank Scene**, **Start from Template**, or **Load Existing Scene** to open the
editor. Template and scene options list `.lucid` files from their project
folders; existing scenes can also be browsed from elsewhere. **View
Documentation** opens [Documentation.md](Documentation.md) in the system's
default application and leaves the startup window open.

The startup window displays developer credit, copyright, and version
information: Arsal Abbas Mirza, Copyright © 2026 Arsal Abbas Mirza, Version v0.5.

## Editor workflow

1. Choose **Start from Blank Scene** to begin empty, **Start from Template** to
   use a demonstration in `templates/`, or **Load Existing Scene** to restore a
   `.lucid` file. Save user scenes in `scenes/` to list them at startup; use
   **Browse...** to open a file stored elsewhere.
2. Use **Add Cube**, **Add Wall**, **Add Directional Light**, or **Add Point
   Light** to add an object to an empty scene.
3. Use **Select** mode and turn off **Click to Camera** to select objects by
   clicking them in the viewport; objects can also be selected from the scene
   list. The selection is outlined and its values appear in the Properties panel.
   With **Click to Camera** enabled (the default), a viewport click enters Camera
   mode; use **Select** in the toolbar to return.
4. Edit the values and choose **Apply**. Positions and scales use world units;
   rotations are entered in degrees. Object colors accept six hexadecimal RGB
   digits, with or without a leading `#`. Enable **Camera collision enabled**
   on a cube or wall to stop the camera at its surface; it is off by default.
5. Choose **File → Save**, **Save as...**, or **Open...** to persist and restore
   scenes. Opening a scene validates the file before replacing the current
   scene. Invalid files are reported without discarding the current scene.
6. Open **View → Render settings...** to adjust the viewport renderer. Settings
   apply immediately when accepted and persist across editor launches; they are
   not written into `.lucid` scene files.

The built-in [wide plain](templates/wide-plain.lucid) uses rotated wall objects
as floor panels and point lights to demonstrate a broad open space. The
[room/maze](templates/room-maze.lucid) example includes a roof, interior
partitions, an entrance, camera-colliding walls, and point lights. Templates
load as unsaved scenes so saving cannot overwrite the shipped examples. The
wall template used by the editor is a box with a rectangular footprint and a
small but non-zero thickness; its dimensions can be adjusted with the scale
fields.
Directional lights use their rotation to aim the light. Point lights use
their position, color, and intensity to illuminate nearby faces.
The Render Settings panel controls global ambient light, point-light falloff,
whether opaque geometry casts shadows, shadow softness and sample quality,
lighting mode, field of view, and internal resolution. These settings persist as
editor preferences and do not modify scene files. Per-pixel lighting evaluates
diffuse contribution and shadow visibility at each visible surface pixel, so a
shadow edge can cross a face instead of darkening its entire triangle. The
**Fast (per-face)** mode is available for lower CPU use. Softness controls the
angular spread of the sampled light direction; low (one-sample) shadows remain
hard, while medium/high sample presets estimate a softer penumbra. Per-pixel
lighting with low (hard-shadow) quality is the default; more samples and higher
internal resolutions cost more CPU. **View → Wireframe overlay**
toggles depth-tested edge outlines for visible scene objects; the selected object
keeps its yellow highlight.

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
| `Esc` with an object selected | Deselect it |
| `Esc` with no object selected | Close the editor |

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

Potential rendering improvements include a world grid and axis guides for spatial
orientation, followed by optional edge antialiasing or supersampling, which would
need to balance smoother edges against the software renderer's per-pixel cost.
Per-light source sizes, gamma-aware color handling, and ambient-occlusion-style
contact shading are further candidates beyond the current global soft-shadow
approximation. Render settings could later expose per-light source sizes and
lighting diagnostics while retaining straightforward fast-quality presets.

## Scene file format

`.lucid` files are versioned Java Properties text files. Object entries store
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
    StartupWindow.java  Startup choices, scene/template discovery, and docs
    SceneEditor.java    Editor window, scene list, and file actions
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
templates/              Built-in .lucid demonstration scenes
scenes/                 User scenes listed at startup
Documentation.md        User guide opened from the startup window
lib/                    Optional location for external JARs; none are required
bin/                    Compiled class output
.vscode/                 VS Code Java project settings
```
