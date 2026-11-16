package Lucid;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import javax.swing.AbstractAction;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

@SuppressWarnings("serial")
public final class PlayWindow extends javax.swing.JFrame {
    private final ViewportPanel viewport;
    private final Runnable onClose;
    private final Timer timer;
    private boolean closing;

    public PlayWindow(Scene scene, Camera camera, RenderSettings settings,
            Runnable onClose) {
        super("Lucid — PLAY");
        if (scene == null || camera == null || settings == null || onClose == null) {
            throw new IllegalArgumentException("Scene, camera, settings, and close action are required.");
        }
        this.onClose = onClose;
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        setMinimumSize(new Dimension(640, 480));
        setSize(960, 720);

        SceneRenderer renderer = new SceneRenderer();
        renderer.configure(settings);
        renderer.setBackgroundColor(settings.playBackgroundColor);
        viewport = new ViewportPanel(scene, camera, renderer, () -> {}, () -> {},
                mode -> {}, message -> {}, false);
        viewport.setClickToCamera(false);
        viewport.setInputMode(ViewportPanel.InputMode.CAMERA);

        JPanel content = new JPanel(new BorderLayout());
        content.add(new JLabel("PLAY — WASD to move, arrows to look, Shift to move faster; Escape to return"),
                BorderLayout.NORTH);
        content.add(viewport, BorderLayout.CENTER);
        setContentPane(content);

        getRootPane().getInputMap(JPanel.WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke("ESCAPE"), "exit-play");
        getRootPane().getActionMap().put("exit-play", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent event) {
                closePlay();
            }
        });
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent event) {
                closePlay();
            }
        });

        timer = new Timer(16, event -> {
            viewport.updateCamera();
            viewport.repaint();
        });
        timer.start();
    }

    public void start() {
        setLocationRelativeTo(null);
        setVisible(true);
        SwingUtilities.invokeLater(viewport::requestFocusInWindow);
    }

    private void closePlay() {
        if (closing) return;
        closing = true;
        timer.stop();
        dispose();
        onClose.run();
    }
}
