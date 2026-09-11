package me.jamino.printer.data;

import com.mojang.serialization.JsonOps;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ImageMetadataTest {
    @Test void imageSourceDimensionsSurviveSaveAndNetworkAndOldSavesRemainUnknown() {
        var image = new ImageReference("a".repeat(64), 256, 128, 2, 1, "Photo", PrintMode.COLOR, PrintFrame.OAK, 3000, 1500);
        var encoded = ImageReference.CODEC.encodeStart(JsonOps.INSTANCE, image).getOrThrow();
        assertEquals(image, ImageReference.CODEC.parse(JsonOps.INSTANCE, encoded).getOrThrow());
        var buffer = new FriendlyByteBuf(Unpooled.buffer());
        try {
            ImageReference.STREAM_CODEC.encode(buffer, image);
            assertEquals(image, ImageReference.STREAM_CODEC.decode(buffer));
        } finally { buffer.release(); }
        encoded.getAsJsonObject().remove("source_width");
        encoded.getAsJsonObject().remove("source_height");
        var legacy = ImageReference.CODEC.parse(JsonOps.INSTANCE, encoded).getOrThrow();
        assertEquals(0, legacy.sourceWidth());
        assertEquals(0, legacy.sourceHeight());
        assertEquals(256, legacy.pixelWidth());
    }

    @Test void presetKeepsCanonicalAndOriginalDimensionsSeparateAndReadsOldData() {
        var preset = new PrinterPreset("b".repeat(64), "Wide", 1024, 512, 4, 2, PrintFrame.NONE, 4096, 2048);
        var encoded = PrinterPreset.CODEC.encodeStart(JsonOps.INSTANCE, preset).getOrThrow();
        assertEquals(preset, PrinterPreset.CODEC.parse(JsonOps.INSTANCE, encoded).getOrThrow());
        var buffer = new FriendlyByteBuf(Unpooled.buffer());
        try {
            PrinterPreset.STREAM_CODEC.encode(buffer, preset);
            assertEquals(preset, PrinterPreset.STREAM_CODEC.decode(buffer));
        } finally { buffer.release(); }
        encoded.getAsJsonObject().remove("original_width");
        encoded.getAsJsonObject().remove("original_height");
        var legacy = PrinterPreset.CODEC.parse(JsonOps.INSTANCE, encoded).getOrThrow();
        assertEquals(0, legacy.originalWidth());
        assertEquals(1024, legacy.sourceWidth());
    }
}
