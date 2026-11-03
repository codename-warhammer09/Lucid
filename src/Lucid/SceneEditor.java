package Lucid;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.KeyboardFocusManager;
import java.awt.event.ActionEvent;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;
import javax.swing.AbstractAction;
import javax.swing.ButtonGroup;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JSplitPane;
import javax.swing.JToolBar;
import javax.swing.JToggleButton;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.Timer;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.text.JTextComponent;

@SuppressWarnings("serial")
public final class SceneEditor extends JFrame {
    // The editor keeps the scene, camera, and renderer in one place so the UI can
    // update objects, manipulate the camera, and render the current world without
    // complex event plumbing.
    private final Scene scene;
    private final Camera camera = new Camera();
    private final SceneRenderer renderer = new SceneRenderer();
    private final Preferences editorPreferences = Preferences.userNodeForPackage(SceneEditor.class);
    private final JCheckBox clickToCameraCheck = new JCheckBox("Click to Camera", true);
    private final JCheckBoxMenuItem clickToCameraMenuItem =
            new JCheckBoxMenuItem("Click viewport to enter Camera mode", true);
    private final JToggleButton selectModeButton = new JToggleButton("Select");
    private final JToggleButton cameraModeButton = new JToggleButton("Camera");
    private final JRadioButtonMenuItem selectModeMenuItem =
            new JRadioButtonMenuItem("Select mode", true);
    private final JRadioButtonMenuItem cameraModeMenuItem =
            new JRadioButtonMenuItem("Camera mode");
    private final DefaultListModel<SceneObject> objectListModel = new DefaultListModel<>();
    private final JList<SceneObject> objectList = new JList<>(objectListModel);
    private final JLabel status = new JLabel("Ready — add an object to begin.");
    private final PropertiesPanel properties;
    private final ViewportPanel viewport;
    private String selectedId;
    private Path currentFile;
    private boolean refreshingList;
    private RenderSettings renderSettings;

    public SceneEditor() {
        this(new Scene(), null, false);
    }

    public SceneEditor(Scene initialScene, Path initialFile) {
        this(initialScene, initialFile, false);
    }

    public SceneEditor(Scene initialScene, Path initialFile, boolean templateScene) {
        super("Lucid — 3D Engine");
        if (initialScene == null) {
            throw new IllegalArgumentException("An initial scene is required.");
        }
        scene = initialScene;
        currentFile = initialFile;
        if (templateScene) {
            camera.position.x = 0.0;
            camera.position.y = 2.5;
            camera.position.z = -9.0;
            camera.pitch = Math.toRadians(9.0);
        }
        renderSettings = RenderSettings.load(editorPreferences);
        renderer.configure(renderSettings);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(1000, 650));
        setSize(1280, 800);
        setLocationRelativeTo(null);

        properties = new PropertiesPanel(this::setStatus, this::refreshObjectList);
        viewport = new ViewportPanel(scene, camera, renderer, this::selectionChanged,
                this::refreshObjectList, this::inputModeChanged, this::setStatus);
        viewport.setClickToCamera(clickToCameraCheck.isSelected());
        clickToCameraCheck.addActionListener(event ->
                setClickToCamera(clickToCameraCheck.isSelected()));
        clickToCameraMenuItem.addActionListener(event ->
                setClickToCamera(clickToCameraMenuItem.isSelected()));
        selectModeButton.addActionListener(event -> activateInputMode(ViewportPanel.InputMode.SELECT));
        cameraModeButton.addActionListener(event -> activateInputMode(ViewportPanel.InputMode.CAMERA));
        selectModeMenuItem.addActionListener(event -> activateInputMode(ViewportPanel.InputMode.SELECT));
        cameraModeMenuItem.addActionListener(event -> activateInputMode(ViewportPanel.InputMode.CAMERA));
        setJMenuBar(createMenuBar());
        add(createToolbar(), BorderLayout.NORTH);
        add(createWorkspace(), BorderLayout.CENTER);
        add(status, BorderLayout.SOUTH);
        bindEditorKeys();
        refreshObjectList();
        if (scene.getObjects().isEmpty()) {
            setStatus("Ready — add an object to begin.");
        } else if (currentFile == null) {
            setStatus("Loaded template. Save as... to choose a scene file.");
        } else {
            setStatus("Loaded " + currentFile.getFileName() + ".");
        }

        // The editor runs a small animation loop to update the camera and repaint the
        // viewport continuously while the user navigates the scene.
        Timer timer = new Timer(16, event -> {
            viewport.updateCamera();
            viewport.repaint();
        });
        timer.start();
    }

    // The workspace is split into three regions: the list of scene objects, the main
    // viewport, and the property editor. This gives the user a practical scene-
    // editor layout without adding more complexity than necessary.
    private JSplitPane createWorkspace() {
        JPanel scenePanel = new JPanel(new BorderLayout());
        scenePanel.add(new JLabel("Scene objects"), BorderLayout.NORTH);
        objectList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        objectList.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public java.awt.Component getListCellRendererComponent(JList<?> list,
                    Object value, int index, boolean selected, boolean focus) {
                JLabel label = (JLabel) super.getListCellRendererComponent(
                        list, value, index, selected, focus);
                if (value instanceof SceneObject) {
                    SceneObject object = (SceneObject) value;
                    label.setText(object.name + "  [" + object.type.name().replace('_', ' ') + "]");
                }
                return label;
            }
        });
        objectList.addListSelectionListener(event -> {
            if (!refreshingList && !event.getValueIsAdjusting()) {
                SceneObject object = objectList.getSelectedValue();
                selectedId = object == null ? null : object.id;
                viewport.setSelectedId(selectedId);
                properties.showObject(object);
            }
        });
        scenePanel.add(new JScrollPane(objectList), BorderLayout.CENTER);
        scenePanel.setPreferredSize(new Dimension(220, 500));

        JPanel viewportPanel = new JPanel(new BorderLayout());
        viewportPanel.add(viewport, BorderLayout.CENTER);
        JSplitPane rightSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                viewportPanel, properties);
        rightSplit.setResizeWeight(1.0);
        rightSplit.setDividerLocation(820);
        rightSplit.setPreferredSize(new Dimension(1000, 600));

        JSplitPane workspace = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                scenePanel, rightSplit);
        workspace.setResizeWeight(0.0);
        workspace.setDividerLocation(220);
        return workspace;
    }

    private JToolBar createToolbar() {
        JToolBar toolbar = new JToolBar();
        toolbar.setFloatable(false);
        addButton(toolbar, "Cube", SceneObject.Type.CUBE);
        addButton(toolbar, "Wall", SceneObject.Type.WALL);
        addButton(toolbar, "Directional light", SceneObject.Type.DIRECTIONAL_LIGHT);
        addButton(toolbar, "Point light", SceneObject.Type.POINT_LIGHT);
        toolbar.addSeparator();
        ButtonGroup inputModes = new ButtonGroup();
        inputModes.add(selectModeButton);
        inputModes.add(cameraModeButton);
        selectModeButton.setSelected(true);
        toolbar.add(selectModeButton);
        toolbar.add(cameraModeButton);
        toolbar.add(clickToCameraCheck);
        toolbar.addSeparator();
        toolbar.add(button("Delete", this::deleteSelected));
        toolbar.add(button("Properties (P)", this::focusProperties));
        return toolbar;
    }

    private JMenuBar createMenuBar() {
        JMenuBar bar = new JMenuBar();
        JMenu file = new JMenu("File");
        file.add(menuItem("New scene", event -> newScene()));
        file.add(menuItem("Open...", event -> openScene()));
        file.add(menuItem("Save", event -> saveScene()));
        file.add(menuItem("Save as...", event -> saveSceneAs()));
        bar.add(file);

        JMenu create = new JMenu("Create");
        create.add(menuItem("Cube", event -> addObject(SceneObject.Type.CUBE)));
        create.add(menuItem("Thick wall", event -> addObject(SceneObject.Type.WALL)));
        create.add(menuItem("Directional light", event ->
                addObject(SceneObject.Type.DIRECTIONAL_LIGHT)));
        create.add(menuItem("Point light", event -> addObject(SceneObject.Type.POINT_LIGHT)));
        bar.add(create);

        JMenu view = new JMenu("View");
        ButtonGroup inputModes = new ButtonGroup();
        inputModes.add(selectModeMenuItem);
        inputModes.add(cameraModeMenuItem);
        view.add(selectModeMenuItem);
        view.add(cameraModeMenuItem);
        view.addSeparator();
        view.add(clickToCameraMenuItem);
        view.add(menuItem("Render settings...", event -> showRenderSettings()));
        bar.add(view);
        return bar;
    }

    private void addButton(JToolBar toolbar, String title, SceneObject.Type type) {
        toolbar.add(button("Add " + title, () -> addObject(type)));
    }

    private JButton button(String title, Runnable action) {
        JButton button = new JButton(title);
        button.addActionListener(event -> action.run());
        return button;
    }

    private JMenuItem menuItem(String title, java.util.function.Consumer<ActionEvent> action) {
        JMenuItem item = new JMenuItem(title);
        item.addActionListener(event -> action.accept(event));
        return item;
    }

    // Creates a new object in front of the camera, with a position and scale suited
    // to the current view. This makes it easy to add geometry without manually
    // positioning every item from scratch.
    private void addObject(SceneObject.Type type) {
        String baseName;
        switch (type) {
            case CUBE: baseName = "Cube"; break;
            case WALL: baseName = "Wall"; break;
            case DIRECTIONAL_LIGHT: baseName = "Directional Light"; break;
            case POINT_LIGHT: baseName = "Point Light"; break;
            default: throw new IllegalStateException("Unsupported template: " + type);
        }
        int ordinal = 1;
        String objectName = baseName + " " + ordinal;
        while (hasObjectNamed(objectName)) {
            objectName = baseName + " " + ++ordinal;
        }

        Vec3d forward = camera.forward();
        Vec3d right = camera.right();
        int objectCount = scene.getObjects().size();
        int column = objectCount == 0 ? 0 : (objectCount % 2 == 1 ? 1 : -1);
        int row = objectCount / 3;
        Vec3d position = camera.position.add(forward.mul(5.0))
                .add(right.mul(column * 2.5)).add(new Vec3d(0.0, row * 2.0, 0.0));
        Vec3d scale;
        Vec3d rotation = new Vec3d();
        switch (type) {
            case CUBE: scale = new Vec3d(2.0, 2.0, 2.0); break;
            case WALL: scale = new Vec3d(4.0, 2.0, 0.2); break;
            case DIRECTIONAL_LIGHT:
                scale = new Vec3d(0.35, 0.35, 0.35);
                rotation.x = Math.toRadians(45.0);
                break;
            case POINT_LIGHT:
                scale = new Vec3d(0.35, 0.35, 0.35);
                break;
            default: throw new IllegalStateException("Unsupported template: " + type);
        }
        SceneObject object = SceneObject.create(objectName, type,
                new Transform(position, rotation, scale));
        if (type == SceneObject.Type.POINT_LIGHT) object.intensity = 5.0;
        scene.add(object);
        selectedId = object.id;
        refreshObjectList();
        setStatus("Added " + object.name + ".");
        viewport.requestFocusInWindow();
    }

    private void selectionChanged() {
        selectedId = viewport.getSelectedId();
        refreshObjectList();
    }

    private void activateInputMode(ViewportPanel.InputMode mode) {
        viewport.setInputMode(mode);
        viewport.requestFocusInWindow();
    }

    private void inputModeChanged(ViewportPanel.InputMode mode) {
        boolean selecting = mode == ViewportPanel.InputMode.SELECT;
        selectModeButton.setSelected(selecting);
        selectModeMenuItem.setSelected(selecting);
        cameraModeButton.setSelected(!selecting);
        cameraModeMenuItem.setSelected(!selecting);
        setStatus(selecting ? "Select mode: click objects to select and edit transforms."
                : "Camera mode: use W/A/S/D, arrows, E/Q, and Shift to navigate.");
    }

    private void setClickToCamera(boolean enabled) {
        clickToCameraCheck.setSelected(enabled);
        clickToCameraMenuItem.setSelected(enabled);
        viewport.setClickToCamera(enabled);
        setStatus(enabled ? "Viewport clicks enter Camera mode."
                : "Viewport clicks select objects.");
    }

    private void showRenderSettings() {
        JSlider ambient = new JSlider(0, 100,
                (int) Math.round(renderSettings.ambientLight * 100.0));
        ambient.setMajorTickSpacing(25);
        ambient.setPaintTicks(true);
        ambient.setPaintLabels(true);
        JSlider fieldOfView = new JSlider(45, 120,
                (int) Math.round(renderSettings.fieldOfView));
        fieldOfView.setMajorTickSpacing(15);
        fieldOfView.setPaintTicks(true);
        fieldOfView.setPaintLabels(true);
        JSlider falloff = new JSlider(0, 50,
                (int) Math.round(renderSettings.pointLightFalloff * 100.0));
        falloff.setMajorTickSpacing(10);
        falloff.setPaintTicks(true);
        falloff.setPaintLabels(true);
        JCheckBox shadows = new JCheckBox("Enable opaque-mesh shadows",
                renderSettings.shadowsEnabled);
        JComboBox<String> resolution = new JComboBox<>(new String[] {
            "320 x 240 (Fast)",
            "640 x 480",
            "960 x 720 (High detail)"
        });
        resolution.setSelectedIndex(renderSettings.getResolutionIndex());

        JPanel form = new JPanel(new java.awt.GridLayout(0, 2, 8, 8));
        form.add(new JLabel("Ambient light (%)"));
        form.add(ambient);
        form.add(new JLabel("Field of view (degrees)"));
        form.add(fieldOfView);
        form.add(new JLabel("Point-light falloff (0-0.50)"));
        form.add(falloff);
        form.add(new JLabel("Internal render resolution"));
        form.add(resolution);
        form.add(new JLabel("Shadows"));
        form.add(shadows);

        int choice = JOptionPane.showConfirmDialog(this, form, "Render Settings",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (choice != JOptionPane.OK_OPTION) return;

        int resolutionIndex = resolution.getSelectedIndex();
        try {
            RenderSettings updated = new RenderSettings(
                    ambient.getValue() / 100.0, shadows.isSelected(),
                    fieldOfView.getValue(), falloff.getValue() / 100.0,
                    RenderSettings.getResolutionWidth(resolutionIndex),
                    RenderSettings.getResolutionHeight(resolutionIndex));
            updated.save(editorPreferences);
            renderSettings = updated;
            renderer.configure(renderSettings);
            viewport.repaint();
            setStatus("Updated and saved render settings.");
        } catch (IllegalArgumentException ex) {
            showRenderSettingsError("Render settings not applied: " + ex.getMessage());
        } catch (BackingStoreException ex) {
            showRenderSettingsError("Could not save render settings: " + ex.getMessage());
        } catch (SecurityException | IllegalStateException ex) {
            showRenderSettingsError("Could not persist render settings: " + ex.getMessage());
        }
    }

    private void showRenderSettingsError(String message) {
        setStatus(message);
        JOptionPane.showMessageDialog(this, message, "Render settings error",
                JOptionPane.ERROR_MESSAGE);
    }

    // Rebuilds the list model from the current scene contents and synchronizes the
    // property panel with the selected object. This keeps the editor UI and the
    // underlying scene data in a consistent state.
    private void refreshObjectList() {
        int selectedIndex = -1;
        refreshingList = true;
        try {
            objectListModel.clear();
            for (int i = 0; i < scene.getObjects().size(); i++) {
                SceneObject object = scene.getObjects().get(i);
                objectListModel.addElement(object);
                if (object.id.equals(selectedId)) selectedIndex = i;
            }
            if (selectedIndex >= 0) objectList.setSelectedIndex(selectedIndex);
            else objectList.clearSelection();
        } finally {
            refreshingList = false;
        }
        SceneObject object = scene.findById(selectedId);
        properties.showObject(object);
        viewport.setSelectedId(selectedId);
        viewport.repaint();
    }

    private void deleteSelected() {
        if (KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner()
                instanceof JTextComponent) return;
        SceneObject selected = scene.findById(selectedId);
        if (selected == null) return;
        scene.remove(selected.id);
        selectedId = null;
        refreshObjectList();
        setStatus("Deleted " + selected.name + ".");
        viewport.requestFocusInWindow();
    }

    private void focusProperties() {
        if (KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner()
                instanceof JTextComponent) return;
        if (scene.findById(selectedId) == null) {
            setStatus("Select an object before opening its properties.");
            return;
        }
        properties.focusProperties();
    }

    // Keyboard shortcuts are wired through the window's action map. This gives us a
    // simple way to handle Delete, the P key, and Escape without cluttering the
    // rest of the editor logic.
    private void bindEditorKeys() {
        getRootPane().getInputMap(JPanel.WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke("DELETE"), "delete-selected");
        getRootPane().getActionMap().put("delete-selected", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent event) {
                deleteSelected();
            }
        });
        getRootPane().getInputMap(JPanel.WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke('P'), "open-properties");
        getRootPane().getActionMap().put("open-properties", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent event) {
                focusProperties();
            }
        });
        getRootPane().getInputMap(JPanel.WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke("ESCAPE"), "close-editor");
        getRootPane().getActionMap().put("close-editor", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent event) {
                dispatchEvent(new WindowEvent(SceneEditor.this, WindowEvent.WINDOW_CLOSING));
            }
        });
    }

    private void newScene() {
        if (!scene.getObjects().isEmpty()) {
            int choice = JOptionPane.showConfirmDialog(this,
                    "Clear the current scene?", "New scene", JOptionPane.YES_NO_OPTION);
            if (choice != JOptionPane.YES_OPTION) return;
        }
        scene.replaceWith(new java.util.ArrayList<>());
        selectedId = null;
        currentFile = null;
        refreshObjectList();
        setStatus("Created an empty scene.");
    }

    // File operations are kept deliberately simple: a scene is saved as a
    // `.lucid` property file and reopened by reading the same format back in.
    private void openScene() {
        JFileChooser chooser = sceneChooser();
        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;
        Path path = chooser.getSelectedFile().toPath();
        try {
            Scene loaded = SceneIO.load(path);
            scene.replaceWith(loaded.getObjects());
            selectedId = null;
            currentFile = path;
            refreshObjectList();
            setStatus("Loaded " + path.getFileName() + ".");
        } catch (IOException | IllegalArgumentException ex) {
            showError("Could not load scene: " + ex.getMessage());
        }
    }

    private void saveScene() {
        if (currentFile == null) {
            saveSceneAs();
            return;
        }
        saveTo(currentFile);
    }

    private void saveSceneAs() {
        JFileChooser chooser = sceneChooser();
        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;
        File selected = chooser.getSelectedFile();
        if (!selected.getName().toLowerCase(java.util.Locale.ROOT).endsWith(".lucid")) {
            selected = new File(selected.getParentFile(), selected.getName() + ".lucid");
        }
        saveTo(selected.toPath());
    }

    private JFileChooser sceneChooser() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileFilter(new FileNameExtensionFilter("Lucid scene (*.lucid)", "lucid"));
        return chooser;
    }

    private void saveTo(Path path) {
        try {
            SceneIO.save(scene, path);
            currentFile = path;
            setStatus("Saved " + path.getFileName() + ".");
        } catch (IOException | IllegalArgumentException ex) {
            showError("Could not save scene: " + ex.getMessage());
        }
    }

    private void showError(String message) {
        setStatus(message);
        JOptionPane.showMessageDialog(this, message, "Scene error", JOptionPane.ERROR_MESSAGE);
    }

    private boolean hasObjectNamed(String name) {
        for (SceneObject object : scene.getObjects()) {
            if (object.name.equalsIgnoreCase(name)) return true;
        }
        return false;
    }

    private void setStatus(String message) {
        status.setText(message);
    }

    public static void main(String[] args) {
        StartupWindow.launch();
    }
}
