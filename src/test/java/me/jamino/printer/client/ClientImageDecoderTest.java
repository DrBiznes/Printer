package me.jamino.printer.client;

import com.mojang.blaze3d.platform.NativeImage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.lwjgl.system.MemoryStack;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

final class ClientImageDecoderTest {
    @ParameterizedTest
    @ValueSource(ints = {256, 512, 1024})
    void decodesPngLargerThanNativeStackWithoutConsumingIt(int side) throws Exception {
        BufferedImage source = noisyImage(side);
        byte[] png = png(source);
        assertTrue(png.length > MemoryStack.stackGet().getSize(), "Fixture must exceed the native stack");
        int stackPointer = MemoryStack.stackGet().getPointer();
        try (NativeImage decoded = ClientImageCache.decodePng(png)) {
            assertEquals(side, decoded.getWidth());
            assertEquals(side, decoded.getHeight());
            for (int[] pixel : new int[][]{{0, 0}, {side / 2, side / 2}, {side - 1, side - 1}}) {
                int argb = source.getRGB(pixel[0], pixel[1]);
                int abgr = (argb & 0xFF00FF00) | ((argb & 0xFF) << 16) | ((argb >>> 16) & 0xFF);
                assertEquals(abgr, decoded.getPixelRGBA(pixel[0], pixel[1]));
            }
        }
        assertEquals(stackPointer, MemoryStack.stackGet().getPointer());
    }

    @Test
    void transferredCanonicalPngRetainsTransparentAndHalfTransparentPixels() throws Exception {
        BufferedImage source = new BufferedImage(3, 1, BufferedImage.TYPE_INT_ARGB);
        source.setRGB(0, 0, 0x00FFFFFF);
        source.setRGB(1, 0, 0x80FF0000);
        source.setRGB(2, 0, 0xFFFFFFFF);
        try (NativeImage decoded = ClientImageCache.decodePng(png(source))) {
            assertEquals(0, decoded.getPixelRGBA(0, 0) >>> 24);
            assertEquals(0x800000FF, decoded.getPixelRGBA(1, 0));
            assertEquals(0xFFFFFFFF, decoded.getPixelRGBA(2, 0));
        }
    }

    @Test
    void decodingRemainsUsableAfterInvalidImage() throws Exception {
        assertThrows(IOException.class, () -> ClientImageCache.decodePng(new byte[32]));
        try (NativeImage decoded = ClientImageCache.decodePng(png(noisyImage(256)))) {
            assertEquals(256, decoded.getWidth());
        }
    }

    private static BufferedImage noisyImage(int side) {
        BufferedImage image = new BufferedImage(side, side, BufferedImage.TYPE_INT_RGB);
        Random random = new Random(872);
        for (int y = 0; y < side; y++) {
            for (int x = 0; x < side; x++) image.setRGB(x, y, random.nextInt(0x1000000));
        }
        return image;
    }

    private static byte[] png(BufferedImage image) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        assertTrue(ImageIO.write(image, "png", output));
        return output.toByteArray();
    }
}
