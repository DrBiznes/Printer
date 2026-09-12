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
        ProcessedImage canonical = ImageProcessor.createVariant(bytes, 8, 6, false, 0xFFFFFF);
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
    void canonicalizationDeduplicatesUploadsAndPreservesOriginalDimensions() throws Exception {
        BufferedImage input = new BufferedImage(1200, 600, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        ImageIO.write(input, "png", bytes);
        ProcessedImage first = ImageProcessor.canonicalize(bytes.toByteArray(), 300, 300);
        ProcessedImage duplicate = ImageProcessor.canonicalize(bytes.toByteArray(), 300, 300);
        assertEquals(first.contentId(), duplicate.contentId());
        assertEquals(300, first.width());
        assertEquals(150, first.height());
        assertEquals(1200, first.originalWidth());
        assertEquals(600, first.originalHeight());
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
        ProcessedImage first = ImageProcessor.createVariant(source, 9, 5, false, 0xFFFFFF);
        ProcessedImage second = ImageProcessor.createVariant(source, 9, 5, false, 0xFFFFFF);

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
        ProcessedImage variant = ImageProcessor.createVariant(sourcePng(), 12, 8, true, 0xFFFFFF);
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

    @Test
    void canonicalSourcePreservesAlphaAndVariantsApplySelectedBackgroundDeterministically() throws Exception {
        BufferedImage input = new BufferedImage(4, 2, BufferedImage.TYPE_INT_ARGB);
        input.setRGB(1, 0, 0x80FF0000);
        input.setRGB(3, 1, 0xFF00FF00);
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        ImageIO.write(input, "png", bytes);
        var canonical = ImageProcessor.canonicalize(bytes.toByteArray(), 4, 2);
        var source = ImageProcessor.decodeChecked(canonical.png());
        assertEquals(0, source.getRGB(0, 0) >>> 24);
        assertEquals(128, source.getRGB(1, 0) >>> 24);
        var white = ImageProcessor.createVariant(canonical.png(), 4, 2, false, 0xFFFFFF);
        var blue = ImageProcessor.createVariant(canonical.png(), 4, 2, false, 0x0000FF);
        var duplicate = ImageProcessor.createVariant(canonical.png(), 4, 2, false, 0x0000FF);
        assertFalse(white.contentId().equals(blue.contentId()));
        assertEquals(blue.contentId(), duplicate.contentId());
        var output = ImageProcessor.decodeChecked(blue.png());
        assertEquals(0xFF0000FF, output.getRGB(0, 0));
        assertEquals(0xFF80007F, output.getRGB(1, 0));
        assertEquals(0xFF00FF00, output.getRGB(3, 1));
        assertEquals(0xFFFFFFFF, ImageProcessor.decodeChecked(white.png()).getRGB(0, 0));
    }

    @Test
    void redBackgroundReplacesTransparentWhiteButNotOpaqueWhite() throws Exception {
        BufferedImage input = new BufferedImage(225, 225, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < input.getHeight(); y++) {
            for (int x = 0; x < input.getWidth(); x++) input.setRGB(x, y, 0x00FFFFFF);
        }
        input.setRGB(112, 112, 0xFFFFFFFF);
        input.setRGB(113, 112, 0xFF000000);
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        ImageIO.write(input, "png", bytes);
        var canonical = ImageProcessor.canonicalize(bytes.toByteArray(), 225, 225);
        var print = ImageProcessor.createVariant(canonical.png(), 225, 225, false, 0xB02E26);
        var output = ImageProcessor.decodeChecked(print.png());
        assertEquals(0xFFB02E26, output.getRGB(0, 0));
        assertEquals(0xFFB02E26, output.getRGB(224, 224));
        assertEquals(0xFFFFFFFF, output.getRGB(112, 112));
        assertEquals(0xFF000000, output.getRGB(113, 112));
    }

    @Test
    void monochromeKeepsBackgroundFlatAndDownscaledSourcesRetainAlpha() throws Exception {
        BufferedImage input = new BufferedImage(8, 8, BufferedImage.TYPE_INT_ARGB);
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        ImageIO.write(input, "png", bytes);
        var canonical = ImageProcessor.canonicalize(bytes.toByteArray(), 2, 2);
        assertEquals(0, ImageProcessor.decodeChecked(canonical.png()).getRGB(0, 0) >>> 24);
        var black = ImageProcessor.createVariant(canonical.png(), 2, 2, true, 0x000000);
        var white = ImageProcessor.createVariant(canonical.png(), 2, 2, true, 0xFFFFFF);
        assertEquals(0xFF000000, ImageProcessor.decodeChecked(black.png()).getRGB(0, 0));
        assertEquals(0xFFFFFFFF, ImageProcessor.decodeChecked(white.png()).getRGB(0, 0));
        var red = ImageProcessor.createVariant(canonical.png(), 2, 2, true, 0xB02E26);
        assertEquals(0xFFB02E26, ImageProcessor.decodeChecked(red.png()).getRGB(0, 0));
    }

    @Test
    void monochromeDithersOnlyArtworkAndBackgroundCannotChangeOpaqueInk() throws Exception {
        BufferedImage input = new BufferedImage(32, 24, BufferedImage.TYPE_INT_ARGB);
        for (int y = 4; y < 20; y++) {
            for (int x = 4; x < 28; x++) input.setRGB(x, y, 0xFF808080);
        }
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        ImageIO.write(input, "png", bytes);
        var white = ImageProcessor.createVariant(bytes.toByteArray(), 32, 24, true, 0xFFFFFF);
        var red = ImageProcessor.createVariant(bytes.toByteArray(), 32, 24, true, 0xB02E26);
        var duplicate = ImageProcessor.createVariant(bytes.toByteArray(), 32, 24, true, 0xB02E26);
        assertEquals(red.contentId(), duplicate.contentId());
        assertFalse(white.contentId().equals(red.contentId()));
        var whitePixels = ImageProcessor.decodeChecked(white.png());
        var redPixels = ImageProcessor.decodeChecked(red.png());
        Set<Integer> artworkColors = new HashSet<>();
        for (int y = 0; y < 24; y++) {
            for (int x = 0; x < 32; x++) {
                if ((input.getRGB(x, y) >>> 24) == 0) {
                    assertEquals(0xFFB02E26, redPixels.getRGB(x, y), "Paper must have no dither speckles");
                } else {
                    assertEquals(whitePixels.getRGB(x, y), redPixels.getRGB(x, y), "Paper cannot change ink");
                    artworkColors.add(redPixels.getRGB(x, y));
                }
            }
        }
        assertEquals(Set.of(0xFF000000, 0xFFFFFFFF), artworkColors);
    }

    @Test
    void monochromeRetainsSoftAlphaEdgesWithoutConvertingPaperToInk() throws Exception {
        BufferedImage input = new BufferedImage(3, 1, BufferedImage.TYPE_INT_ARGB);
        input.setRGB(1, 0, 0x80FF0000);
        input.setRGB(2, 0, 0x80FFFFFF);
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        ImageIO.write(input, "png", bytes);
        var print = ImageProcessor.createVariant(bytes.toByteArray(), 3, 1, true, 0x0000FF);
        var output = ImageProcessor.decodeChecked(print.png());
        assertEquals(0xFF0000FF, output.getRGB(0, 0));
        assertEquals(0xFF00007F, output.getRGB(1, 0));
        assertEquals(0xFF8080FF, output.getRGB(2, 0));
    }

    @Test
    void opaqueMonochromeArtworkDeduplicatesAcrossBackgroundSelections() throws Exception {
        var white = ImageProcessor.createVariant(sourcePng(), 12, 8, true, 0xFFFFFF);
        var red = ImageProcessor.createVariant(sourcePng(), 12, 8, true, 0xB02E26);
        assertEquals(white.contentId(), red.contentId());
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
