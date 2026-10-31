# Lucid — 3D Engine Project

Lucid is an evolving 3D engine project written in Java. Its current
implementation is an early, runnable foundation: a software-rendered cube in a
Swing window, with a movable first-person camera.

The features and controls described here document the engine's present state,
not a limit on its intended scope or a claim that the engine is complete. As
development continues, this README can be updated to reflect what has been
implemented.

## Current development state

The engine currently:

- Transforms and projects a triangle mesh in software.
- Clips geometry to near and far depth planes, shades faces with directional
  lighting, and uses a depth buffer to resolve visibility.
- Renders a sample cube at 320 x 240 and scales the image 3x for display in a
  960 x 720 Swing window.
- Provides a keyboard-controlled camera for walking, strafing, looking around,
  vertical movement, sprinting, and resetting.

This is the project's current working snapshot, not a feature-complete engine.

## Requirements

- A Java Development Kit (JDK) with `java` and `javac` available on your
  `PATH`.
- A desktop environment that can display Swing windows.

Lucid uses only Java's standard library. No third-party dependencies, build
tools, or editor extensions are required.

## Build and run

Open a terminal in the project directory (the directory containing this
README), then compile and launch:

```powershell
New-Item -ItemType Directory -Force bin | Out-Null
javac -d bin src\Lucid\Lucid.java src\Lucid\Mat4x4.java src\Lucid\Triangle.java src\Lucid\Vec3d.java
java -cp bin Lucid.Lucid
```

The commands work in Windows PowerShell. In VS Code, you can also open this
folder as a Java project and run `Lucid.Lucid` from the editor.

## Current controls

These controls describe the current camera implementation. Click the game
window if it does not have keyboard focus.

| Key | Action |
| --- | --- |
| `W` / `S` | Walk forward / backward, relative to the camera's heading |
| `A` / `D` | Strafe left / right |
| `Left` / `Right` | Turn left / right |
| `Up` / `Down` | Look up / down |
| `E` / `Q` | Move up / down |
| `Shift` | Move faster while held |
| `R` | Reset the camera to its starting position and orientation |
| `Esc` | Close the application |

Horizontal movement follows the camera heading. Moving diagonally does not
increase the walking speed. Camera movement keys are cleared when the window
loses focus.

## Project layout

```text
src/
  Lucid/
    Lucid.java     Swing window, input, camera, render loop, and rasterizer
    Mat4x4.java    4 x 4 transforms, rotations, and perspective projection
    Triangle.java  Triangle mesh data
    Vec3d.java     3D vector operations
lib/               Optional location for external JARs; none are required
bin/               Compiled class output
.vscode/            VS Code Java project settings
```

The entry point is `Lucid.Lucid`. The sample cube mesh, camera defaults,
projection range, and render resolution are defined in `src/Lucid/Lucid.java`.
