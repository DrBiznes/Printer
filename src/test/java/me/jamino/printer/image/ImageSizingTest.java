package me.jamino.printer.image;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class ImageSizingTest {
    @Test
    void automaticSizingUsesResolutionTierAndPreservesShape() {
        assertEquals(new ImageSizing.Size(1, 1), ImageSizing.automatic(128, 128, 4));
        assertEquals(new ImageSizing.Size(2, 2), ImageSizing.automatic(256, 256, 4));
        assertEquals(new ImageSizing.Size(4, 3), ImageSizing.automatic(800, 600, 4));
        assertEquals(new ImageSizing.Size(4, 2), ImageSizing.automatic(1920, 1080, 4));
        assertEquals(new ImageSizing.Size(2, 4), ImageSizing.automatic(600, 1200, 4));
    }

    @Test
    void manualSizingChangesLongEdgeWithoutDistortingTexture() {
        assertEquals(new ImageSizing.Size(6, 3), ImageSizing.forLongEdge(1920, 1080, 6));
        assertEquals(new ImageSizing.PixelSize(512, 288), ImageSizing.textureSize(1920, 1080, 4, 2));
        assertEquals(new ImageSizing.PixelSize(64, 64), ImageSizing.textureSize(64, 64, 4, 4));
    }
}
