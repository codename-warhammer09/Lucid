package Lucid;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.KeyboardFocusManager;
import java.awt.event.ActionEvent;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import javax.swing.AbstractAction;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JToolBar;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.text.JTextComponent;

@SuppressWarnings("serial")
public final class SceneEditor extends JFrame {
    private final Scene scene = new Scene();
    private final Camera camera = new Camera();
    private final SceneRenderer renderer = new SceneRenderer();
    private final DefaultListModel<SceneObject> objectListModel = new DefaultListModel<>();
    private final JList<SceneObject> objectList = new JList<>(objectListModel);
    private final JLabel status = new JLabel("Ready — add an object to begin.");
    private final PropertiesPanel properties;
    private final ViewportPanel viewport;
    private String selectedId;
    private Path currentFile;
    private boolean refreshingList;

    public SceneEditor() {
        super("Lucid — 3D Engine");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(1000, 650));
        setSize(1280, 800);
        setLocationRelativeTo(null);

        properties = new PropertiesPanel(this::setStatus, this::refreshObjectList);
        viewport = new ViewportPanel(scene, camera, renderer, this::selectionChanged);
        setJMenuBar(createMenuBar());
        add(createToolbar(), BorderLayout.NORTH);
        add(createWorkspace(), BorderLayout.CENTER);
        add(status, BorderLayout.SOUTH);
        bindEditorKeys();

        Timer timer = new Timer(16, event -> {
            viewport.updateCamera();
            viewport.repaint();
        });
        timer.start();
    }

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
        SwingUtilities.invokeLater(() -> new SceneEditor().setVisible(true));
    }
}
