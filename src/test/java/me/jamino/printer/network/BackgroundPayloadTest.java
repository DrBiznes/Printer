package me.jamino.printer.network;

import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BackgroundPayloadTest {
    @Test void exactRgbAndPrinterPositionSurviveNetworkWithoutFramePayload() {
        var payload = new ModNetworking.SetBackgroundPayload(new BlockPos(17, 64, -22), 0x224466);
        var buffer = new FriendlyByteBuf(Unpooled.buffer());
        try {
            ModNetworking.SetBackgroundPayload.STREAM_CODEC.encode(buffer, payload);
            assertEquals(payload, ModNetworking.SetBackgroundPayload.STREAM_CODEC.decode(buffer));
            assertEquals(0, buffer.readableBytes());
            assertEquals("printer:set_background", payload.type().id().toString());
        } finally { buffer.release(); }
    }
}
