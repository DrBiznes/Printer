package me.jamino.printer.data;

import com.mojang.serialization.JsonOps;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ImageMetadataTest {
    @Test void transparentPrintRetainsArtworkAndSurvivesSaveAndNetwork() {
        for (PrintMode mode : PrintMode.values()) {
            var paper = new ImageReference("a".repeat(64), 256, 128, 2, 1, "Cutout", mode,
                    0x224466, 3000, 1500, "b".repeat(64), false);
            var transparent = paper.withoutBackground();
            assertEquals(paper.transparentContentId(), transparent.contentId());
            assertTrue(transparent.transparentBackground());
            assertEquals(paper.backgroundColor(), transparent.backgroundColor());
            assertEquals(paper.title(), transparent.title());
            assertEquals(mode, transparent.mode());
            assertSame(transparent, transparent.withoutBackground());
            for (var reference : java.util.List.of(paper, transparent)) {
                var encoded = ImageReference.CODEC.encodeStart(JsonOps.INSTANCE, reference).getOrThrow();
                assertEquals(reference, ImageReference.CODEC.parse(JsonOps.INSTANCE, encoded).getOrThrow());
                var buffer = new FriendlyByteBuf(Unpooled.buffer());
                try {
                    ImageReference.STREAM_CODEC.encode(buffer, reference);
                    assertEquals(reference, ImageReference.STREAM_CODEC.decode(buffer));
                    assertEquals(0, buffer.readableBytes());
                } finally { buffer.release(); }
            }
        }
    }

    @Test void imageMetadataRequiresBothTransparencyFields() {
        var image = new ImageReference("a".repeat(64), 16, 16, 1, 1, "Image", PrintMode.COLOR,
                0xFFFFFF, 16, 16, "b".repeat(64), false);
        for (String field : java.util.List.of("transparent_content_id", "transparent_background")) {
            var json = ImageReference.CODEC.encodeStart(JsonOps.INSTANCE, image).getOrThrow().getAsJsonObject();
            json.remove(field);
            assertTrue(ImageReference.CODEC.parse(JsonOps.INSTANCE, json).error().isPresent(), field);
        }
    }

    @Test void imageDimensionsAndBackgroundSurviveSaveAndNetworkWithoutLegacyFallback() {
        var image = new ImageReference("a".repeat(64), 256, 128, 2, 1, "Photo", PrintMode.COLOR, 0x224466, 3000, 1500, "b".repeat(64), false);
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
        var image = new ImageReference("a".repeat(64), 16, 16, 1, 1, "", PrintMode.COLOR, 0xFF123456, 16, 16, "b".repeat(64), false);
        assertEquals(0x123456, image.backgroundColor());
        assertFalse(ImageReference.CODEC.encodeStart(JsonOps.INSTANCE, image).getOrThrow().getAsJsonObject().has("frame"));
    }
}
