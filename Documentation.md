# Lucid Engine Documentation

Lucid is a lightweight Java scene editor and software-rendered 3D engine. This
v0.5 guide combines the practical setup, editor controls, scene workflow, and
`.lucid` format reference in one place.

## Starting Lucid

Build and run from the project directory with a Java Development Kit installed:

```powershell
New-Item -ItemType Directory -Force bin | Out-Null
javac -d bin src\Lucid\*.java
java -cp bin Lucid.Lucid
```

The startup window offers:

- **Start from Blank Scene** to open the existing empty-scene workflow.
- **Start from Template** to load a demonstration scene from `templates/`.
- **Load Existing Scene** to select a project scene in `scenes/` or browse for
  any `.lucid` file.
- **View Documentation** to open this guide using the system's default
  application. The startup window remains available.

Templates are loaded as new scenes and are not associated with their built-in
file, so saving cannot overwrite a shipped example. Existing scenes retain
their selected file for the normal **File → Save** workflow.

## Built-in templates and saved scenes

`templates/wide-plain.lucid` demonstrates a broad, flat floor assembled from
wall objects rotated onto their sides, with point lights over the space.
`templates/room-maze.lucid` demonstrates a roofed multi-room interior, an open
entry, partitions, camera-colliding walls, and point lights in several rooms.
These are quickstarts, not a template-authoring feature.

Save user scenes in `scenes/` to have them listed on the startup window. A scene
can also be opened from elsewhere with **Browse...**. Both templates and scenes
use the same `.lucid` format.

## Editor workflow

The editor starts with an empty scene when **Start from Blank Scene** is chosen.
Use **Add Cube**, **Add Wall**, **Add Directional Light**, or **Add Point Light**
to place objects. A wall is a rectangular box with a small but non-zero
thickness; its scale and rotation can be edited in Properties.

Use the scene list or Select mode to select an object. The Properties panel
edits its name, transform, color, light settings, and optional camera collision.
Positions and scales use world units; rotations in the editor are in degrees.
Object colors accept six hexadecimal RGB digits, with or without a leading `#`.

Choose **File → New scene**, **Open...**, **Save**, or **Save as...** to manage
scenes. Opening a scene validates the file before replacing the current scene;
invalid files are reported without discarding the current scene. Render options
are available under **View → Render settings...** and persist as editor
preferences rather than scene data.

## Controls

Keyboard shortcuts do not act while editing a text field.

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
degrees for rotation. Hold Shift for 0.01 units or 0.5 degrees.

## Rendering and lighting

The viewport uses a software renderer with perspective projection, near/far
clipping, depth buffering, ambient lighting, and directional/point lighting.
In per-pixel mode, diffuse lighting and shadow visibility are evaluated at each
visible surface pixel. This lets a shadow boundary cross a face instead of
applying one triangle-wide light result. Surfaces use flat face normals; the
renderer is not a physically based material shader.

Opaque geometry can cast shadows. **View → Render settings...** controls
per-face (fast) or per-pixel lighting, shadows, global shadow softness and sample
quality, ambient light, point-light falloff, field of view, and internal render
resolution. Soft shadows estimate the visible fraction of multiple rays around
each light direction. Low quality uses one hard-shadow sample; medium and high
use four and eight samples. A softness of zero also produces hard shadows.
Per-pixel shading, more shadow samples, and higher render resolutions increase
software-rendering cost. These options persist as editor preferences and do not
change `.lucid` scene files. The default is per-pixel lighting with one hard
shadow sample; select medium or high samples to enable soft penumbrae.

**View → Wireframe overlay** shows depth-tested box edges for visible scene
objects; the selected object's edges remain yellow.

Future rendering improvements could include world-grid and axis guides,
antialiasing, per-light source sizes, gamma-aware color handling, and
ambient-occlusion-style contact shading. Per-light source sizes would allow more
physical control of soft shadows than the current global angular approximation.

Directional lights use their rotation to aim the light. Point lights use
position, color, and intensity to illuminate nearby faces. Geometry can opt into
camera collision; it is off by default for newly created objects.

## `.lucid` scene format

`.lucid` files are versioned Java Properties text files. Version 1 stores the
scene format version and an object count, followed by each object's ID, name,
type, position, rotation in degrees, scale, raster color, light color, light
intensity, and camera-collision setting. Colors are six-digit RGB hex values.
Files without the optional `collisionEnabled` property remain valid and default
to collision off.

Scenes are validated before they replace the current editor contents. The
format is intended to be readable and editable without external serialization
libraries.

## Project layout

```text
src\Lucid\       Java application, editor, scene model, and renderer
templates\       Built-in demonstration scenes in .lucid format
scenes\          User scenes listed by the startup window
Documentation.md This guide
README.md        Project overview and quick setup
bin\             Compiled class output
```
