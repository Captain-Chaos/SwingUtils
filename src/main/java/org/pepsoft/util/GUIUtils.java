package org.pepsoft.util;

import org.pepsoft.util.swing.ManagesScale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.plaf.FontUIResource;
import javax.swing.plaf.InsetsUIResource;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.awt.image.BufferedImageOp;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.prefs.Preferences;

import static java.awt.Image.SCALE_SMOOTH;
import static java.awt.RenderingHints.*;
import static java.awt.image.AffineTransformOp.TYPE_BICUBIC;
import static java.awt.image.AffineTransformOp.TYPE_NEAREST_NEIGHBOR;
import static java.awt.image.BufferedImage.TYPE_INT_ARGB;
import static java.lang.Math.round;

/**
 * System properties you can set before this class is initialised:
 *
 * <p>{@code org.pepsoft.util.GUIUtils.disableScaling} - set to {@code true} to force the scale to 100%<br>
 * {@code org.pepsoft.snapshotVersion} - set to {@code true} to cause the UI scale preference to be loaded from a
 * separate, snapshot setting
 *
 * <p>Created by pepijn on 13-Jan-17.
 */
public class GUIUtils {
    /**
     * Scale an image according to {@link #UI_SCALE}. Nearest neighbour scaling
     * is used, in other words no smoothing or interpolation is applied.
     *
     * @param image The image to scale.
     * @return The original image if {@code UI_SCALE} is 1, or an
     * appropriately scaled copy otherwise.
     */
    public static BufferedImage scaleToUI(Image image) {
        return scaleToUI(image, false);
    }

    /**
     * Scale an image according to {@link #UI_SCALE_FLOAT} or {@link #UI_SCALE}.
     *
     * @param image The image to scale.
     * @param smooth Whether to do smooth scaling. When this is {@code true},
     *               the image is scaled to the floating point UI scale using
     *               bicubic scaling. When it is {@code false}, it is scaled to
     *               the UI scale rounded to the nearest integer, using nearest
     *               neighbour scaling.
     * @return The original image if {@code UI_SCALE} is 1, or an
     * appropriately scaled copy otherwise.
     */
    public static BufferedImage scaleToUI(Image image, boolean smooth) {
        if (((UI_SCALE_FLOAT == 1.0f) || ((! smooth) && (UI_SCALE == 1))) && (image instanceof BufferedImage)) {
            return (BufferedImage) image;
        } else if (image instanceof BufferedImage) {
            BufferedImageOp op;
            if (smooth) {
                op = new AffineTransformOp(AffineTransform.getScaleInstance(getUIScale(), getUIScale()), TYPE_BICUBIC);
            } else {
                op = new AffineTransformOp(AffineTransform.getScaleInstance(getUIScaleInt(), getUIScaleInt()), TYPE_NEAREST_NEIGHBOR);
            }
            return op.filter((BufferedImage) image, null);
        } else {
            BufferedImage scaledImage;
            if (smooth) {
                scaledImage = new BufferedImage(round(image.getWidth(null) * getUIScale()), round(image.getHeight(null) * getUIScale()), TYPE_INT_ARGB);
                Graphics2D g2 = scaledImage.createGraphics();
                try {
                    g2.setRenderingHint(KEY_INTERPOLATION, VALUE_INTERPOLATION_BICUBIC);
                    g2.drawImage(image, AffineTransform.getScaleInstance(getUIScale(), getUIScale()), null);
                } finally {
                    g2.dispose();
                }
            } else {
                scaledImage = new BufferedImage(image.getWidth(null) * getUIScaleInt(), image.getHeight(null) * getUIScaleInt(), TYPE_INT_ARGB);
                Graphics2D g2 = scaledImage.createGraphics();
                try {
                    g2.setRenderingHint(KEY_INTERPOLATION, VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
                    g2.drawImage(image, AffineTransform.getScaleInstance(getUIScaleInt(), getUIScaleInt()), null);
                } finally {
                    g2.dispose();
                }
            }
            return scaledImage;
        }
    }

    public static void scaleToUI(Container container) {
        if (UI_SCALE_FLOAT == 1.0f) {
            return;
        }
        for (Component component: container.getComponents()) {
            if (component instanceof ManagesScale) {
                continue;
            }
            if (component instanceof JTable) {
                JTable table = (JTable) component;
                table.setRowHeight(Math.round(table.getRowHeight() * UI_SCALE_FLOAT));
                table.setRowMargin(Math.round(table.getRowMargin() * UI_SCALE_FLOAT));
                table.revalidate();
            } else if (component instanceof JTextArea) {
                component.setFont(UIManager.getFont("TextField.font"));
                component.revalidate();
            } else if (component instanceof AbstractButton) {
                final AbstractButton button = (AbstractButton) component;
                if (button.getIcon() instanceof ImageIcon) {
                    final ImageIcon icon = (ImageIcon) button.getIcon();
                    if (icon.getIconWidth() == icon.getIconHeight()) {
                        button.setIcon(IconUtils.scaleIcon(icon, icon.getIconWidth()));
                        button.revalidate();
                    }
                }
            }
            if (component instanceof Container) {
                scaleToUI((Container) component);
            }
        }
    }

    public static void scaleWindow(Window window) {
        final GraphicsConfiguration gc = window.getGraphicsConfiguration();
        final Rectangle screenBounds = gc.getBounds();
        final Insets insets = Toolkit.getDefaultToolkit().getScreenInsets(gc);
        window.setSize(Math.min(Math.round(window.getWidth() * UI_SCALE_FLOAT), screenBounds.width - insets.left - insets.right),
                Math.min(Math.round(window.getHeight() * UI_SCALE_FLOAT), screenBounds.height - insets.top - insets.bottom));
    }

    /**
     * Adjusts the {@link UIManager} defaults to show the Java 2D UI at the
     * specified scale.
     */
    public static void scaleLookAndFeel(float scale) {
        if (scale == 1.0f) {
            return;
        }
        Set<String> unknownValueTypesEncountered = new HashSet<>();
        for (Map.Entry<Object, Object> entry: UIManager.getDefaults().entrySet()) {
            final Object key = entry.getKey();
            final Object value = UIManager.get(key);
            try {
                if (value instanceof FontUIResource) {
                    final FontUIResource previousResource = (FontUIResource) value;
                    UIManager.put(key, new FontUIResource(previousResource.getFamily(), previousResource.getStyle(), Math.round(previousResource.getSize() * scale)));
                    logger.trace("Scaled FontUIResource {}", key);
                } else if (value instanceof InsetsUIResource) {
                    final InsetsUIResource oldResource = (InsetsUIResource) value;
                    UIManager.put(key, new InsetsUIResource(Math.round(oldResource.top * scale),
                            Math.round(oldResource.left * scale),
                            Math.round(oldResource.bottom * scale),
                            Math.round(oldResource.right * scale)));
                    logger.trace("Scaled InsetsUIResource {}", key);
                } else if (value instanceof Insets) {
                    final Insets oldResource = (Insets) value;
                    UIManager.put(key, new Insets(Math.round(oldResource.top * scale),
                            Math.round(oldResource.left * scale),
                            Math.round(oldResource.bottom * scale),
                            Math.round(oldResource.right * scale)));
                    logger.trace("Scaled Insets {}", key);
                } else if ((value instanceof Integer) && (key instanceof String)) {
                    final String lowerCaseKey = ((String) key).toLowerCase();
                    if (lowerCaseKey.contains("margin")
                            || lowerCaseKey.contains("thickness")
                            || lowerCaseKey.contains("gap")
                            || lowerCaseKey.contains("width")
                            || lowerCaseKey.contains("height")
                            || lowerCaseKey.contains("spacing")
                            || lowerCaseKey.contains("size")
                            || lowerCaseKey.contains("length")
                            || lowerCaseKey.contains("offset")
                            || lowerCaseKey.contains("shift")
                            || lowerCaseKey.contains("indent")
                            || lowerCaseKey.contains("padding")) {
                        UIManager.put(key, Math.round((Integer) value * scale));
                        logger.trace("Scaled integer {}", key);
                    } else {
                        unknownValueTypesEncountered.add("Integer for key " + key);
                        if (logger.isTraceEnabled()) {
                            logger.trace("Did NOT scale {}: {}} ({}})", key, value, value.getClass().getSimpleName());
                        }
                    }
                } else if (value instanceof ImageIcon) {
                    final ImageIcon icon = (ImageIcon) value;
                    UIManager.put(key, new ImageIcon(icon.getImage().getScaledInstance(Math.round(icon.getIconWidth() * scale), -1, SCALE_SMOOTH)));
                    logger.trace("Scaled ImageIcon {}", key);
                } else if (value instanceof Icon) {
                    final Icon icon = (Icon) value;
                    final BufferedImage image = new BufferedImage(icon.getIconWidth(), icon.getIconHeight(), TYPE_INT_ARGB);
                    final Graphics2D g2 = image.createGraphics();
                    try {
                        try {
                            icon.paintIcon(null, g2, 0, 0);
                        } finally {
                            g2.dispose();
                        }
                        UIManager.put(key, new ImageIcon(image.getScaledInstance(Math.round(icon.getIconWidth() * scale), -1, SCALE_SMOOTH)));
                        logger.trace("Scaled Icon {}", key);
                    } catch (NullPointerException | IllegalArgumentException e) {
                        logger.debug("Did NOT scale Icon {} due to {}", key, e.getClass().getSimpleName());
                    }
                } else if ((value instanceof Color) || (value instanceof Boolean)) {
                    // Ignore silently
                } else if (value != null) {
                    unknownValueTypesEncountered.add(value.getClass().getSimpleName());
                    if (logger.isTraceEnabled()) {
                        logger.trace("Did NOT scale {}: {}} ({}})", key, value, value.getClass().getSimpleName());
                    }
                }
            } catch (Throwable t) {
                logger.error("Did NOT scale {}: {}} ({}}) due to {} (message: \"{}\")", key, value, value.getClass().getSimpleName(), t.getClass().getSimpleName(), t.getMessage(), t);
            }
        }
        if (logger.isDebugEnabled()) {
            logger.debug("Did NOT scale value types: {}", unknownValueTypesEncountered);
        }
    }

    /**
     * Override the detected system default UI scale.
     *
     * @param uiScale The UI scale to use instead of the detected system
     *                default.
     */
    public static void setUIScale(float uiScale) {
        UI_SCALE_FLOAT = uiScale;
        UI_SCALE = round(uiScale);
    }

    /**
     * How many times to scale pixel sizes to display at approximately the
     * originally intended size for assets which were designed for 96 dpi
     * screens.
     *
     * <p><strong>Note:</strong> for now UI scaling is only activated on <!-- TODO -->
     * Windows, until the current support on Mac and Linux can be investigated. <!-- TODO -->
     */
    public static float getUIScale() {
        return UI_SCALE_FLOAT;
    }

    /**
     * How many times to scale pixel sizes to display at approximately the
     * originally intended size for assets which were designed for 92-96 dpi
     * screens.
     *
     * <p>This is {@link #getUIScale()} rounded to the nearest integer and is
     * intended for small images that don't scale well to non integer factors
     * such as icons.
     *
     * <p><strong>Note:</strong> for now UI scaling is only activated on <!-- TODO -->
     * Windows, until the current support on Mac and Linux can be investigated. <!-- TODO -->
     */
    public static int getUIScaleInt() {
        return UI_SCALE;
    }

    /**
     * The detected system default UI scale.
     *
     * <p><strong>Note:</strong> for now UI scaling is only activated on <!-- TODO -->
     * Windows, until the current support on Mac and Linux can be investigated. <!-- TODO -->
     */
    public static final float SYSTEM_UI_SCALE_FLOAT = GraphicsEnvironment.isHeadless()
            ? 1.0f
            : SystemUtils.isWindows()
                ? MathUtils.clamp(1.0f, (float) Toolkit.getDefaultToolkit().getScreenResolution() / 96, 2.0f)
                : 1.0f;

    /**
     * How many times to scale pixel sizes to display at approximately the
     * originally intended size for assets which were designed for 92-96 dpi
     * screens.
     *
     * <p>This is {@link #UI_SCALE_FLOAT} rounded to the nearest integer and is
     * intended for small images that don't scale well to non integer factors
     * such as icons.
     *
     * <p><strong>Note:</strong> for now UI scaling is only activated on <!-- TODO -->
     * Windows, until the current support on Mac and Linux can be investigated. <!-- TODO -->
     */
    public static final int SYSTEM_UI_SCALE = round(SYSTEM_UI_SCALE_FLOAT);

    private static final Logger logger = LoggerFactory.getLogger(GUIUtils.class);

    private static float UI_SCALE_FLOAT;
    private static int UI_SCALE;

    static {
        if ("true".equalsIgnoreCase(System.getProperty("org.pepsoft.util.GUIUtils.disableScaling"))) {
            logger.info("Not scaling GUI");
            UI_SCALE_FLOAT = 1.0f;
        } else {
            float manualUIScale = Preferences.userNodeForPackage(GUIUtils.class).getFloat(("true".equalsIgnoreCase(System.getProperty("org.pepsoft.snapshotVersion")) ? "snapshot." : "") + "manualUIScale", -1.0f);
            if (manualUIScale > 0.0f) {
                UI_SCALE_FLOAT = manualUIScale;
            } else {
                UI_SCALE_FLOAT = SYSTEM_UI_SCALE_FLOAT;
            }
        }
        UI_SCALE = Math.max(round(UI_SCALE_FLOAT), 1);
    }
}