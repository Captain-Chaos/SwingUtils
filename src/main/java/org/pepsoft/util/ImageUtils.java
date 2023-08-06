package org.pepsoft.util;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.RenderedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static java.awt.image.BufferedImage.TYPE_INT_ARGB;

public class ImageUtils {
    /**
     * Serialise a {@link RenderedImage} (such as a {@link BufferedImage}) to a byte array.
     */
    public static byte[] toBytes(RenderedImage image) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, "PNG", baos);
        return baos.toByteArray();
    }

    /**
     * Deserialise an image previously serialised using {@link #toBytes(RenderedImage)} to a {@link BufferedImage}.
     */
    public static BufferedImage fromBytes(byte[] bytes) throws IOException {
        ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
        return ImageIO.read(bais);
    }

    /**
     * Create a square image of a specific size with a specific solid colour.
     */
    public static BufferedImage createColourSquare(int size, int colour) {
        final BufferedImage image = new BufferedImage(size, size, TYPE_INT_ARGB);
        for (int x = 0; x < size; x++) {
            for (int y = 0; y < size; y++) {
                image.setRGB(x, y, 0xff000000 | colour);
            }
        }
        return image;
    }

    /**
     * Create a deep copy of a {@link BufferedImage}.
     */
    public static BufferedImage clone(BufferedImage image) {
        final ColorModel cm = image.getColorModel();
        return new BufferedImage(cm, image.copyData(image.getRaster().createCompatibleWritableRaster()), cm.isAlphaPremultiplied(), null);
    }
}