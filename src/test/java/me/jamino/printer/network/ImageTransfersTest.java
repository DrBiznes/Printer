package me.jamino.printer.network;

import org.junit.jupiter.api.Test;
import java.security.MessageDigest;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class ImageTransfersTest {
    private String hash(byte[] bytes) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
    }
    @Test void reassemblesMultipleChunksAndVerifiesContentHash() throws Exception {
        var transfers = new ImageTransfers();
        byte[] data = new byte[ImageTransfers.CHUNK_BYTES + 3]; new Random(12).nextBytes(data);
        String id = hash(data);
        assertNull(transfers.accept(id, 0, 2, Arrays.copyOf(data, ImageTransfers.CHUNK_BYTES), 0));
        assertArrayEquals(data, transfers.accept(id, 1, 2, Arrays.copyOfRange(data, ImageTransfers.CHUNK_BYTES, data.length), 1));
        assertEquals(0, transfers.reservedBytes());
        assertNull(transfers.accept("a".repeat(64), 0, 1, new byte[]{1}, 2));
        assertEquals(0, transfers.reservedBytes());
    }
    @Test void rejectsInvalidAndOutOfOrderChunksAndReleasesReservation() {
        var transfers = new ImageTransfers(); String id = "a".repeat(64);
        assertNull(transfers.accept(id, 0, Integer.MAX_VALUE, new byte[]{1}, 0));
        assertNull(transfers.accept(id, 1, 2, new byte[]{1}, 0));
        assertNull(transfers.accept(id, 0, 2, new byte[]{1}, 0));
        assertEquals(0, transfers.reservedBytes());
        transfers.accept(id, 0, 2, new byte[ImageTransfers.CHUNK_BYTES], 0);
        assertTrue(transfers.reservedBytes() > 0);
        assertNull(transfers.accept(id, 0, 2, new byte[ImageTransfers.CHUNK_BYTES], 1));
        assertEquals(0, transfers.reservedBytes());
    }
    @Test void boundsTotalMemoryAndExpiresAbandonedResponses() {
        var transfers = new ImageTransfers();
        for (int i = 0; i < 20; i++) transfers.accept(String.format("%064x", i), 0, 16, new byte[ImageTransfers.CHUNK_BYTES], 0);
        assertEquals(16 * 1024 * 1024, transfers.reservedBytes());
        transfers.expire(15_000);
        assertEquals(0, transfers.reservedBytes());
    }
    @Test void requestBudgetAllowsGalleryBurstsAndBoundsSustainedTraffic() {
        var budget = new ImageRequestBudget(); var player = UUID.randomUUID();
        for (int i = 0; i < 16; i++) assertTrue(budget.acquire(player, 0, 0));
        assertFalse(budget.acquire(player, 0, 0));
        assertTrue(budget.acquire(player, 0, 100_000_000));
        for (int i = 0; i < 8; i++) assertTrue(budget.acquire(player, 4 * 1024 * 1024, 100_000_000));
        assertFalse(budget.acquire(player, 1, 100_000_000));
        budget.remove(player);
        assertTrue(budget.acquire(player, 4 * 1024 * 1024, 100_000_000));
    }
}
