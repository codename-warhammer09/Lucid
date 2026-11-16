package Lucid;

import java.awt.BorderLayout;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileNameExtensionFilter;

/**
 * Routes each application launch to a blank scene, a built-in template, or an
 * existing scene, with a separate action for opening the user guide.
 */
@SuppressWarnings("serial")
public final class StartupWindow extends JFrame {
    private static final Path PROJECT_DIRECTORY = resolveProjectDirectory();
    private static final Path TEMPLATE_DIRECTORY = PROJECT_DIRECTORY.resolve("templates");
    private static final Path SCENE_DIRECTORY = PROJECT_DIRECTORY.resolve("scenes");
    private static final Path DOCUMENTATION_FILE = PROJECT_DIRECTORY.resolve("Documentation.md");

    private final DefaultListModel<Path> catalogModel = new DefaultListModel<>();
    private final JList<Path> catalogList = new JList<>(catalogModel);

    private StartupWindow() {
        super("Lucid — 3D Engine");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(620, 420));
        setSize(720, 480);
        setLocationRelativeTo(null);

        JPanel content = new JPanel(new BorderLayout(12, 16));
        content.setBorder(javax.swing.BorderFactory.createEmptyBorder(24, 28, 20, 28));
        JLabel heading = new JLabel("Welcome to Lucid");
        heading.setFont(heading.getFont().deriveFont(24.0f));
        JPanel welcome = new JPanel(new BorderLayout(0, 6));
        welcome.add(heading, BorderLayout.NORTH);
        welcome.add(new JLabel("<html>v0.6: five ready-to-play templates, surface-snapped "
                + "START markers, a dedicated PLAY window, and configurable editor/PLAY "
                + "backgrounds.</html>"), BorderLayout.CENTER);
        content.add(welcome, BorderLayout.NORTH);

        JPanel choices = new JPanel(new GridLayout(2, 2, 12, 12));
        choices.add(choiceButton("Start from Blank Scene", this::startBlankScene));
        choices.add(choiceButton("Start from Template", this::chooseTemplate));
        choices.add(choiceButton("Load Existing Scene", this::chooseExistingScene));
        choices.add(choiceButton("View Documentation", this::openDocumentation));
        content.add(choices, BorderLayout.CENTER);

        JPanel information = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 3));
        information.add(new JLabel("Developed by Arsal Abbas Mirza  |  "
                + "Copyright © 2026 Arsal Abbas Mirza  |  Version: v0.6"));
        content.add(information, BorderLayout.SOUTH);
        setContentPane(content);

        catalogList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        catalogList.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public java.awt.Component getListCellRendererComponent(JList<?> list,
                    Object value, int index, boolean selected, boolean focus) {
                JLabel label = (JLabel) super.getListCellRendererComponent(
                        list, value, index, selected, focus);
                if (value instanceof Path) {
                    label.setText(((Path) value).getFileName().toString());
                }
                return label;
            }
        });
    }

    public static void launch() {
        SwingUtilities.invokeLater(() -> new StartupWindow().setVisible(true));
    }

    private static Path resolveProjectDirectory() {
        Path classLocation = null;
        try {
            java.security.CodeSource codeSource =
                    StartupWindow.class.getProtectionDomain().getCodeSource();
            if (codeSource != null) {
                classLocation = Paths.get(codeSource.getLocation().toURI())
                        .toAbsolutePath().normalize();
                if (!Files.isDirectory(classLocation)) {
                    classLocation = classLocation.getParent();
                }
            }
        } catch (URISyntaxException | SecurityException ex) {
            // Fall back to the working directory when the runtime hides its code location.
        }

        Path projectDirectory = findProjectDirectory(classLocation);
        if (projectDirectory != null) return projectDirectory;

        Path workingDirectory = Paths.get("").toAbsolutePath().normalize();
        projectDirectory = findProjectDirectory(workingDirectory);
        if (projectDirectory != null) return projectDirectory;
        return classLocation != null ? classLocation : workingDirectory;
    }

    private static Path findProjectDirectory(Path location) {
        for (Path candidate = location; candidate != null; candidate = candidate.getParent()) {
            if (Files.isDirectory(candidate.resolve("templates"))
                    && Files.isRegularFile(candidate.resolve("README.md"))) {
                return candidate;
            }
        }
        return null;
    }

    private JButton choiceButton(String text, Runnable action) {
        JButton button = new JButton(text);
        button.addActionListener(event -> action.run());
        return button;
    }

    private void startBlankScene() {
        openEditor(new Scene(), null, false);
    }

    private void chooseTemplate() {
        chooseCatalogFile(TEMPLATE_DIRECTORY, "Choose a Template", false);
    }

    private void chooseExistingScene() {
        chooseCatalogFile(SCENE_DIRECTORY, "Load Existing Scene", true);
    }

    private void chooseCatalogFile(Path directory, String title, boolean allowBrowse) {
        final List<Path> files;
        try {
            files = listSceneFiles(directory);
        } catch (IOException | SecurityException ex) {
            showError("Could not read " + directory + ": " + ex.getMessage());
            return;
        }

        catalogModel.clear();
        for (Path file : files) catalogModel.addElement(file);
        catalogList.setVisibleRowCount(Math.max(4, Math.min(10, files.size())));
        JScrollPane list = new JScrollPane(catalogList);
        list.setPreferredSize(new Dimension(420, 190));
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.add(new JLabel(files.isEmpty()
                ? "No .lucid files found in " + directory + "."
                : "Choose a .lucid file from " + directory + "."), BorderLayout.NORTH);
        panel.add(list, BorderLayout.CENTER);

        Object[] options = allowBrowse
                ? new Object[] {"Load Scene", "Browse...", "Cancel"}
                : new Object[] {"Load Template", "Cancel"};
        while (true) {
            int choice = JOptionPane.showOptionDialog(this, panel, title,
                    JOptionPane.DEFAULT_OPTION, JOptionPane.PLAIN_MESSAGE,
                    null, options, options[0]);
            if (choice == 0) {
                Path selected = catalogList.getSelectedValue();
                if (selected == null) {
                    showError("Select a .lucid file first.");
                    continue;
                }
                loadScene(selected, !allowBrowse);
                return;
            }
            if (allowBrowse && choice == 1) {
                Path selected = browseForScene();
                if (selected != null) loadScene(selected, false);
                return;
            }
            return;
        }
    }

    private List<Path> listSceneFiles(Path directory) throws IOException {
        if (Files.notExists(directory)) return new ArrayList<>();
        if (!Files.isDirectory(directory)) {
            throw new IOException(directory + " exists but is not a directory.");
        }
        try (Stream<Path> paths = Files.list(directory)) {
            List<Path> files = new ArrayList<>();
            paths.filter(Files::isRegularFile)
                    .filter(this::isLucidFile)
                    .forEach(files::add);
            files.sort(Comparator.comparing(
                    path -> path.getFileName().toString().toLowerCase(Locale.ROOT)));
            return files;
        }
    }

    private boolean isLucidFile(Path path) {
        return path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".lucid");
    }

    private Path browseForScene() {
        JFileChooser chooser = new JFileChooser();
        if (Files.isDirectory(SCENE_DIRECTORY)) {
            chooser.setCurrentDirectory(SCENE_DIRECTORY.toFile());
        }
        chooser.setFileFilter(new FileNameExtensionFilter("Lucid scene (*.lucid)", "lucid"));
        chooser.setAcceptAllFileFilterUsed(false);
        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return null;
        return chooser.getSelectedFile().toPath();
    }

    private void loadScene(Path path, boolean template) {
        try {
            Scene loaded = SceneIO.load(path);
            openEditor(loaded, template ? null : path, template);
        } catch (IOException | IllegalArgumentException ex) {
            showError("Could not load scene: " + ex.getMessage());
        }
    }

    private void openEditor(Scene scene, Path currentFile, boolean templateScene) {
        SceneEditor editor = new SceneEditor(scene, currentFile, templateScene);
        editor.setVisible(true);
        dispose();
    }

    private void openDocumentation() {
        try {
            if (!Files.isRegularFile(DOCUMENTATION_FILE)) {
                throw new IOException("Documentation.md was not found in "
                        + Paths.get("").toAbsolutePath() + ".");
            }
            if (!Desktop.isDesktopSupported()
                    || !Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
                throw new IOException("This system does not support opening files "
                        + "with the default desktop application.");
            }
            Desktop.getDesktop().open(DOCUMENTATION_FILE.toFile());
        } catch (IOException | UnsupportedOperationException | SecurityException ex) {
            showError("Could not open documentation: " + ex.getMessage());
        }
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "Lucid startup error",
                JOptionPane.ERROR_MESSAGE);
    }
}
