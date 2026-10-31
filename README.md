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
  depth buffering, ambient and directional/point lighting.
- A scene object list, viewport selection, and a visible outline for the
  selected object.
- An inspector for object name, position, rotation, scale, and color. Light
  objects also expose light color and intensity.
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
2. Select an object in the scene list or click it in the viewport. The
   selection is outlined and its values appear in the Properties panel.
3. Edit the values and choose **Apply**. Positions and scales use world units;
   rotations are entered in degrees. Object colors accept six hexadecimal RGB
   digits, with or without a leading `#`.
4. Choose **File → Save**, **Save as...**, or **Open...** to persist and restore
   scenes. Opening a scene validates the file before replacing the current
   scene. Invalid files are reported without discarding the current scene.

The wall template is a box with a rectangular footprint and a small but
non-zero thickness; its dimensions can be adjusted with the scale fields.
Directional lights use their rotation to aim the light. Point lights use
their position, color, and intensity to illuminate nearby faces.

## Current controls

Click the viewport to give it keyboard focus. Camera movement is disabled
while editing a text field.

| Key | Action |
| --- | --- |
| `W` / `S` | Move forward / backward |
| `A` / `D` | Strafe left / right |
| `Left` / `Right` | Turn left / right |
| `Up` / `Down` | Look up / down |
| `E` / `Q` | Move up / down |
| `Shift` | Move faster while held |
| `R` | Reset the camera |
| `P` | Focus the selected object's Properties |
| `Delete` | Delete the selected object |
| `Esc` | Close the editor |

## Scene file format

`.lucid` files are versioned Java Properties text files. Object entries store
their ID, name, template type, transform, raster color, and light settings.
Rotation values in the file are in degrees. The format is designed to be
readable and does not require external serialization libraries.

## Project layout

```text
src/
  Lucid/
    Lucid.java          Application entry point
    SceneEditor.java    Editor window, scene list, templates, and file actions
    ViewportPanel.java  Viewport input, camera movement, and display
    PropertiesPanel.java Object inspector and validation
    SceneRenderer.java  Software rendering, lighting, highlighting, and picking
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
