package me.jamino.printer.image;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ImageProcessorTest {
    @ParameterizedTest
    @ValueSource(strings = {"lossless.webp", "lossy.webp", "alpha.webp", "animated.webp", "sample.ico", "sample.tga"})
    void decodesExtendedFormatsAndCanonicalizesToPng(String fixture) throws Exception {
        byte[] bytes;
        try (var stream = getClass().getResourceAsStream("/images/" + fixture)) {
            assertNotNull(stream);
            bytes = stream.readAllBytes();
        }
        BufferedImage image = ImageProcessor.decodeChecked(bytes);
        assertEquals(8, image.getWidth());
        assertEquals(6, image.getHeight());
        int left = image.getRGB(2, 2);
        int right = image.getRGB(6, 2);
        assertTrue(((left >> 16) & 255) > 150, "Left side must remain red (first animation frame)");
        assertTrue((right & 255) > 150, "Right side must remain blue, not mirrored");
        ProcessedImage canonical = ImageProcessor.createVariant(bytes, 8, 6, false);
        assertEquals(0x89, canonical.png()[0] & 255);
        BufferedImage png = ImageProcessor.decodeChecked(canonical.png());
        assertEquals(255, png.getRGB(0, 0) >>> 24);
        if (fixture.equals("alpha.webp")) assertEquals(0xFFFFFFFF, png.getRGB(0, 0));
    }

    @ParameterizedTest
    @ValueSource(strings = {"png", "jpeg", "gif", "bmp", "tiff"})
    void decodesStandardRasterFormats(String format) throws Exception {
        BufferedImage image = new BufferedImage(7, 5, BufferedImage.TYPE_INT_RGB);
        image.setRGB(2, 2, 0xFFCC4422);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        assertTrue(ImageIO.write(image, format, output));
        BufferedImage decoded = ImageProcessor.decodeChecked(output.toByteArray());
        assertEquals(7, decoded.getWidth());
        assertEquals(5, decoded.getHeight());
    }

    @Test
    void acceptsCdnTypesButRejectsDocumentsAndUnsupportedCodecs() {
        for (String type : new String[]{"image/webp", "IMAGE/TIFF; charset=binary", "image/x-icon",
                "image/x-tga", "application/octet-stream", ""}) {
            assertTrue(ImageProcessor.acceptsContentType(type), type);
        }
        for (String type : new String[]{"text/html", "image/svg+xml", "image/avif", "application/pdf"}) {
            assertFalse(ImageProcessor.acceptsContentType(type), type);
        }
    }

    @Test
    void rejectsOversizedDimensionsBeforeReadingPixels() throws Exception {
        BufferedImage image = new BufferedImage(4097, 1, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ImageIO.write(image, "png", output);
        assertThrows(IOException.class, () -> ImageProcessor.decodeChecked(output.toByteArray()));
    }

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
