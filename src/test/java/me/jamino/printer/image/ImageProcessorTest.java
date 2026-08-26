package me.jamino.printer.image;

import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ImageProcessorTest {
    @Test
    void createsDeterministicSizedColorVariant() throws Exception {
        byte[] source = sourcePng();
        ProcessedImage first = ImageProcessor.createVariant(source, 9, 5, false);
        ProcessedImage second = ImageProcessor.createVariant(source, 9, 5, false);

        assertEquals(9, first.width());
        assertEquals(5, first.height());
        assertEquals(first.contentId(), second.contentId());
        assertEquals(64, first.contentId().length());
        BufferedImage decoded = ImageProcessor.decodeChecked(first.png());
        assertEquals(9, decoded.getWidth());
        assertEquals(5, decoded.getHeight());
    }

    @Test
    void monochromeVariantContainsOnlyBlackAndWhite() throws Exception {
        ProcessedImage variant = ImageProcessor.createVariant(sourcePng(), 12, 8, true);
        BufferedImage decoded = ImageProcessor.decodeChecked(variant.png());
        Set<Integer> colors = new HashSet<>();
        for (int y = 0; y < decoded.getHeight(); y++) {
            for (int x = 0; x < decoded.getWidth(); x++) colors.add(decoded.getRGB(x, y) & 0xFFFFFF);
        }
        assertTrue(colors.stream().allMatch(color -> color == 0x000000 || color == 0xFFFFFF));
    }

    @Test
    void rejectsNonImageBytes() {
        assertThrows(IOException.class, () -> ImageProcessor.decodeChecked(new byte[]{1, 2, 3, 4}));
    }

    private static byte[] sourcePng() throws IOException {
        BufferedImage image = new BufferedImage(4, 3, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                int red = x * 70;
                int green = y * 100;
                int blue = (x + y) * 35;
                image.setRGB(x, y, 0xFF000000 | red << 16 | green << 8 | blue);
            }
        }
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ImageIO.write(image, "png", output);
        return output.toByteArray();
    }
}
