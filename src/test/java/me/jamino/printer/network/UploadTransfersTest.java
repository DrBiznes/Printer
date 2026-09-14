package me.jamino.printer.network;

import me.jamino.printer.image.ImageFailure;
import me.jamino.printer.job.RequestThrottle;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.UUID;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

class UploadTransfersTest {
    private final UploadTransfers<String> uploads = new UploadTransfers<>();
    private final UUID player = UUID.randomUUID(), id = UUID.randomUUID();

    @Test void preflightLeavesExistingSessionAndReservationUntouched() throws Exception {
        var original = uploads.begin(player, id, 10, 10, "first printer", 0);
        var failure = assertThrows(ImageFailure.class, () -> uploads.checkBegin(player, 10, 10));
        assertEquals(ImageFailure.Reason.BUSY, failure.reason());
        assertSame(original, uploads.get(player));
        assertEquals(10, uploads.reservedBytes());
        assertThrows(ImageFailure.class, () -> uploads.checkBegin(UUID.randomUUID(), 0, 10));
        assertEquals(1, uploads.snapshot().size());
    }

    @Test void assemblesExactBytesWithBoundedOrderedChunksAndReleasesReservation() throws Exception {
        byte[] source = new byte[UploadTransfers.CHUNK_BYTES + 17];
        new java.util.Random(42).nextBytes(source);
        var transfer = uploads.begin(player, id, source.length, source.length, "printer", 0);
        uploads.accept(player, id, 0, Arrays.copyOf(source, UploadTransfers.CHUNK_BYTES), 1);
        assertFalse(transfer.complete());
        assertEquals(1, transfer.nextIndex());
        uploads.accept(player, id, 1, Arrays.copyOfRange(source, UploadTransfers.CHUNK_BYTES, source.length), 2);
        assertTrue(transfer.complete());
        assertArrayEquals(source, transfer.bytes());
        uploads.remove(player);
        assertEquals(0, uploads.reservedBytes());
    }

    @ParameterizedTest @ValueSource(ints = {-1, 0, 11, Integer.MAX_VALUE})
    void rejectsInvalidDeclaredSizesBeforeReserving(int size) {
        assertThrows(ImageFailure.class, () -> uploads.begin(player, id, size, 10, "printer", 0));
        assertEquals(0, uploads.reservedBytes());
    }

    @Test void rejectsWrongPlayerIdOrderDuplicateAndTruncatedChunk() throws Exception {
        uploads.begin(player, id, UploadTransfers.CHUNK_BYTES + 1, 100000, "printer", 0);
        byte[] full = new byte[UploadTransfers.CHUNK_BYTES];
        assertThrows(ImageFailure.class, () -> uploads.accept(UUID.randomUUID(), id, 0, full, 1));
        assertThrows(ImageFailure.class, () -> uploads.accept(player, UUID.randomUUID(), 0, full, 1));
        assertThrows(ImageFailure.class, () -> uploads.accept(player, id, -1, full, 1));
        assertThrows(ImageFailure.class, () -> uploads.accept(player, id, 1, full, 1));
        assertThrows(ImageFailure.class, () -> uploads.accept(player, id, 0, new byte[5], 1));
        uploads.accept(player, id, 0, full, 1);
        assertThrows(ImageFailure.class, () -> uploads.accept(player, id, 0, full, 2));
        assertThrows(ImageFailure.class, () -> uploads.accept(player, id, 1, new byte[2], 2));
        assertFalse(uploads.get(player).complete());
    }

    @Test void incompleteTransfersExpireAndDisconnectCancellationFreesMemory() throws Exception {
        uploads.begin(player, id, 100, 100, "printer", 10);
        var failure = assertThrows(ImageFailure.class, () -> uploads.accept(player, id, 0, new byte[100],
                10 + UploadTransfers.TIMEOUT_NANOS));
        assertEquals(ImageFailure.Reason.TIMEOUT, failure.reason());
        uploads.remove(player);
        assertNull(uploads.get(player));
        assertEquals(0, uploads.reservedBytes());
        assertThrows(ImageFailure.class, () -> uploads.accept(player, id, 0, new byte[100], 11));
    }

    @Test void limitsConcurrentSessionsAndRejectsSecondSessionForSamePlayer() throws Exception {
        uploads.begin(player, id, 10, 10, "a", 0);
        assertThrows(ImageFailure.class, () -> uploads.begin(player, UUID.randomUUID(), 10, 10, "b", 0));
        for (int i = 0; i < 3; i++) uploads.begin(UUID.randomUUID(), UUID.randomUUID(), 10, 10, "c", 0);
        assertThrows(ImageFailure.class, () -> uploads.begin(UUID.randomUUID(), UUID.randomUUID(), 10, 10, "d", 0));
        uploads.clear();
        assertEquals(0, uploads.reservedBytes());
    }

    @Test void sharesCooldownAcrossLoadAttemptsAndCanCleanUpPlayers() {
        RequestThrottle throttle = new RequestThrottle();
        assertTrue(throttle.acquire(player, 0, 2000));
        assertFalse(throttle.acquire(player, 1999, 2000));
        assertTrue(throttle.acquire(player, 2000, 2000));
        throttle.remove(player);
        assertTrue(throttle.acquire(player, 2001, 2000));
    }

    @Test void uploadPacketsRoundTripAndFitServerboundPayloadLimit() {
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        try {
            var start = new ModNetworking.BeginUploadPayload(BlockPos.ZERO, id, 12345, "My image");
            ModNetworking.BeginUploadPayload.STREAM_CODEC.encode(buffer, start);
            assertEquals(start, ModNetworking.BeginUploadPayload.STREAM_CODEC.decode(buffer));
            buffer.clear();
            var chunk = new ModNetworking.UploadChunkPayload(id, 7, new byte[UploadTransfers.CHUNK_BYTES]);
            ModNetworking.UploadChunkPayload.STREAM_CODEC.encode(buffer, chunk);
            assertTrue(buffer.readableBytes() < 32767);
            var decoded = ModNetworking.UploadChunkPayload.STREAM_CODEC.decode(buffer);
            assertEquals(id, decoded.id());
            assertEquals(7, decoded.index());
            assertArrayEquals(chunk.bytes(), decoded.bytes());
        } finally { buffer.release(); }
    }

    @Test void malformedPacketLengthsAndTruncationFailBeforeAssembly() {
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        try {
            buffer.writeUUID(id); buffer.writeVarInt(0); buffer.writeVarInt(UploadTransfers.CHUNK_BYTES + 1);
            assertThrows(RuntimeException.class, () -> ModNetworking.UploadChunkPayload.STREAM_CODEC.decode(buffer));
            buffer.clear(); buffer.writeUUID(id); buffer.writeVarInt(0); buffer.writeVarInt(10); buffer.writeByte(1);
            assertThrows(RuntimeException.class, () -> ModNetworking.UploadChunkPayload.STREAM_CODEC.decode(buffer));
            buffer.clear();
            assertThrows(RuntimeException.class, () -> ModNetworking.UploadChunkPayload.STREAM_CODEC.encode(buffer,
                    new ModNetworking.UploadChunkPayload(id, 0, new byte[UploadTransfers.CHUNK_BYTES + 1])));
        } finally { buffer.release(); }
    }
}
