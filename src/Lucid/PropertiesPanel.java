package Lucid;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.util.function.Consumer;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;

@SuppressWarnings("serial")
public final class PropertiesPanel extends JPanel {
    // Each field represents one component of a vector: X, Y, and Z. This panel is
    // the editor surface for placing, rotating, scaling, and coloring an object.
    private final JTextField nameField = new JTextField();
    private final JTextField[] positionFields = fields();
    private final JTextField[] rotationFields = fields();
    private final JTextField[] scaleFields = fields();
    private final JTextField colorField = new JTextField();
    private final JTextField lightColorField = new JTextField();
    private final JTextField intensityField = new JTextField();
    private final JLabel selectionLabel = new JLabel("Select an object to edit its properties.");
    private final JLabel colorLabel = new JLabel("Color (RGB hex)");
    private final Consumer<String> showStatus;
    private final Runnable afterApply;
    private SceneObject selected;

    public PropertiesPanel(Consumer<String> showStatus, Runnable afterApply) {
        super(new BorderLayout(8, 8));
        this.showStatus = showStatus;
        this.afterApply = afterApply;
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        add(new JLabel("Properties"), BorderLayout.NORTH);

        JPanel form = new JPanel(new GridLayout(0, 1, 4, 4));
        form.add(selectionLabel);
        form.add(new JLabel("Name"));
        form.add(nameField);
        addVector(form, "Position", positionFields);
        addVector(form, "Rotation (degrees)", rotationFields);
        addVector(form, "Scale", scaleFields);
        form.add(colorLabel);
        form.add(colorField);
        form.add(new JLabel("Light color (RGB hex)"));
        form.add(lightColorField);
        form.add(new JLabel("Light intensity (0–8)"));
        form.add(intensityField);
        JButton apply = new JButton("Apply");
        apply.addActionListener(event -> applyChanges());
        form.add(apply);
        JScrollPane scroll = new JScrollPane(form);
        scroll.setBorder(null);
        add(scroll, BorderLayout.CENTER);
    }

    // Loads the chosen object into the form so the user can edit it without losing
    // the existing transform or light values.
    public void showObject(SceneObject object) {
        selected = object;
        boolean hasSelection = object != null;
        selectionLabel.setText(hasSelection
                ? object.type.name().replace('_', ' ') : "Select an object to edit its properties.");
        nameField.setEnabled(hasSelection);
        setFieldsEnabled(positionFields, hasSelection);
        setFieldsEnabled(rotationFields, hasSelection);
        setFieldsEnabled(scaleFields, hasSelection);
        colorField.setEnabled(hasSelection);
        lightColorField.setEnabled(hasSelection && object.isLight());
        intensityField.setEnabled(hasSelection && object.isLight());
        if (!hasSelection) {
            nameField.setText("");
            colorField.setText("");
            lightColorField.setText("");
            intensityField.setText("");
            clearFields(positionFields);
            clearFields(rotationFields);
            clearFields(scaleFields);
            return;
        }

        nameField.setText(object.name);
        setVector(positionFields, object.transform.position, false);
        setVector(rotationFields, object.transform.rotation, true);
        setVector(scaleFields, object.transform.scale, false);
        colorLabel.setText(object.isLight() ? "Marker color (RGB hex)" : "Raster color (RGB hex)");
        colorField.setText(String.format("#%06X", object.color));
        lightColorField.setText(String.format("#%06X", object.lightColor));
        intensityField.setText(Double.toString(object.intensity));
    }

    public void focusProperties() {
        if (selected != null) nameField.requestFocusInWindow();
    }

    // Validates the edited values and writes them back to the selected object.
    private void applyChanges() {
        if (selected == null) return;
        try {
            String name = nameField.getText().trim();
            Vec3d position = parseVector(positionFields, "Position");
            Vec3d rotation = parseVector(rotationFields, "Rotation");
            rotation = rotation.mul(Math.PI / 180.0);
            Vec3d scale = parseVector(scaleFields, "Scale");
            int color = parseColor(colorField.getText(), "Color");
            int lightColor = selected.isLight()
                    ? parseColor(lightColorField.getText(), "Light color") : selected.lightColor;
            double intensity = selected.isLight()
                    ? Double.parseDouble(intensityField.getText().trim()) : selected.intensity;
            SceneObject validated = new SceneObject(selected.id, name, selected.type,
                    new Transform(position, rotation, scale), color, lightColor, intensity);
            selected.name = validated.name;
            selected.transform = validated.transform;
            selected.color = validated.color;
            selected.lightColor = validated.lightColor;
            selected.intensity = validated.intensity;
            showStatus.accept("Updated " + selected.name + ".");
            afterApply.run();
        } catch (IllegalArgumentException ex) {
            showStatus.accept("Properties not applied: " + ex.getMessage());
        }
    }

    private static Vec3d parseVector(JTextField[] fields, String label) {
        return new Vec3d(parseFinite(fields[0], label + " X"),
                parseFinite(fields[1], label + " Y"),
                parseFinite(fields[2], label + " Z"));
    }

    private static double parseFinite(JTextField field, String label) {
        double value = Double.parseDouble(field.getText().trim());
        if (!Double.isFinite(value)) throw new IllegalArgumentException(label + " must be finite.");
        return value;
    }

    private static int parseColor(String text, String label) {
        String digits = text.trim();
        if (digits.startsWith("#")) digits = digits.substring(1);
        if (!digits.matches("[0-9a-fA-F]{6}")) {
            throw new IllegalArgumentException(label + " must be six hexadecimal digits.");
        }
        return Integer.parseInt(digits, 16);
    }

    private static JTextField[] fields() {
        return new JTextField[] {new JTextField(), new JTextField(), new JTextField()};
    }

    private static void addVector(JPanel panel, String label, JTextField[] fields) {
        panel.add(new JLabel(label + " (X, Y, Z)"));
        JPanel row = new JPanel(new GridLayout(1, 3, 4, 0));
        for (JTextField field : fields) row.add(field);
        panel.add(row);
    }

    private static void setVector(JTextField[] fields, Vec3d value, boolean degrees) {
        double factor = degrees ? 180.0 / Math.PI : 1.0;
        fields[0].setText(Double.toString(value.x * factor));
        fields[1].setText(Double.toString(value.y * factor));
        fields[2].setText(Double.toString(value.z * factor));
    }

    private static void setFieldsEnabled(JTextField[] fields, boolean enabled) {
        for (JTextField field : fields) field.setEnabled(enabled);
    }

    private static void clearFields(JTextField[] fields) {
        for (JTextField field : fields) field.setText("");
    }
}
