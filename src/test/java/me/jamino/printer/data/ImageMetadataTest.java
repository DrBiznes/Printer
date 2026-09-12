package me.jamino.printer.data;

import com.mojang.serialization.JsonOps;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ImageMetadataTest {
    @Test void imageDimensionsAndBackgroundSurviveSaveAndNetworkWithoutLegacyFallback() {
        var image = new ImageReference("a".repeat(64), 256, 128, 2, 1, "Photo", PrintMode.COLOR, 0x224466, 3000, 1500);
        var encoded = ImageReference.CODEC.encodeStart(JsonOps.INSTANCE, image).getOrThrow();
        assertEquals(image, ImageReference.CODEC.parse(JsonOps.INSTANCE, encoded).getOrThrow());
        var buffer = new FriendlyByteBuf(Unpooled.buffer());
        try {
            ImageReference.STREAM_CODEC.encode(buffer, image);
            assertEquals(image, ImageReference.STREAM_CODEC.decode(buffer));
        } finally { buffer.release(); }
        encoded.getAsJsonObject().remove("source_width");
        encoded.getAsJsonObject().remove("source_height");
        assertTrue(ImageReference.CODEC.parse(JsonOps.INSTANCE, encoded).error().isPresent());
        encoded = ImageReference.CODEC.encodeStart(JsonOps.INSTANCE, image).getOrThrow();
        encoded.getAsJsonObject().remove("background_color");
        encoded.getAsJsonObject().addProperty("frame", "oak");
        assertTrue(ImageReference.CODEC.parse(JsonOps.INSTANCE, encoded).error().isPresent());
    }

    @Test void presetKeepsBackgroundAndCanonicalAndOriginalDimensionsWithoutLegacyFallback() {
        var preset = new PrinterPreset("b".repeat(64), "Wide", 1024, 512, 4, 2, 0xCC8844, 4096, 2048);
        var encoded = PrinterPreset.CODEC.encodeStart(JsonOps.INSTANCE, preset).getOrThrow();
        assertEquals(preset, PrinterPreset.CODEC.parse(JsonOps.INSTANCE, encoded).getOrThrow());
        var buffer = new FriendlyByteBuf(Unpooled.buffer());
        try {
            PrinterPreset.STREAM_CODEC.encode(buffer, preset);
            assertEquals(preset, PrinterPreset.STREAM_CODEC.decode(buffer));
        } finally { buffer.release(); }
        encoded.getAsJsonObject().remove("original_width");
        encoded.getAsJsonObject().remove("original_height");
        assertTrue(PrinterPreset.CODEC.parse(JsonOps.INSTANCE, encoded).error().isPresent());
        encoded = PrinterPreset.CODEC.encodeStart(JsonOps.INSTANCE, preset).getOrThrow();
        encoded.getAsJsonObject().remove("background_color");
        encoded.getAsJsonObject().addProperty("frame", "none");
        assertTrue(PrinterPreset.CODEC.parse(JsonOps.INSTANCE, encoded).error().isPresent());
    }

    @Test void backgroundColorsAreNative24BitValuesAndWhiteIsTheDefinedDefault() {
        assertEquals(0xFFFFFF, ImageReference.DEFAULT_BACKGROUND_COLOR);
        var image = new ImageReference("a".repeat(64), 16, 16, 1, 1, "", PrintMode.COLOR, 0xFF123456, 16, 16);
        assertEquals(0x123456, image.backgroundColor());
        assertFalse(ImageReference.CODEC.encodeStart(JsonOps.INSTANCE, image).getOrThrow().getAsJsonObject().has("frame"));
    }
}
