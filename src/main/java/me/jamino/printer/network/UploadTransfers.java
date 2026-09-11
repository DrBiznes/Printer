package me.jamino.printer.network;

import me.jamino.printer.image.ImageFailure;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static me.jamino.printer.image.ImageFailure.Reason.*;

/** Bounded, ordered assembly independent of Minecraft. All access is on the server thread. */
public final class UploadTransfers<T> {
    public static final int CHUNK_BYTES = 24 * 1024;
    public static final int MAX_TRANSFER_BYTES = 64 * 1024 * 1024;
    public static final long TIMEOUT_NANOS = java.util.concurrent.TimeUnit.SECONDS.toNanos(120);
    private final Map<UUID, Transfer<T>> transfers = new HashMap<>();
    private long reservedBytes;

    public record Transfer<T>(UUID id, T target, byte[] bytes, long deadline, int[] position) {
        public int nextIndex() { return position[0]; }
        public int received() { return position[1]; }
        public boolean complete() { return received() == bytes.length; }
    }

    public Transfer<T> begin(UUID player, UUID id, int size, int policyLimit, T target, long now) throws ImageFailure {
        if (size <= 0 || size > policyLimit || size > MAX_TRANSFER_BYTES) throw new ImageFailure(TOO_LARGE);
        if (transfers.containsKey(player) || transfers.size() >= 4 || reservedBytes + size > MAX_TRANSFER_BYTES)
            throw new ImageFailure(BUSY);
        Transfer<T> transfer = new Transfer<>(id, target, new byte[size], now + TIMEOUT_NANOS, new int[2]);
        transfers.put(player, transfer);
        reservedBytes += size;
        return transfer;
    }

    public Transfer<T> accept(UUID player, UUID id, int index, byte[] bytes, long now) throws ImageFailure {
        Transfer<T> transfer = transfers.get(player);
        if (transfer == null || !transfer.id().equals(id)) throw new ImageFailure(INVALID_TRANSFER);
        if (now >= transfer.deadline()) throw new ImageFailure(TIMEOUT);
        int expectedBytes = Math.min(CHUNK_BYTES, transfer.bytes.length - transfer.received());
        if (index != transfer.nextIndex() || bytes.length != expectedBytes || expectedBytes == 0)
            throw new ImageFailure(INVALID_TRANSFER);
        System.arraycopy(bytes, 0, transfer.bytes, transfer.received(), bytes.length);
        transfer.position[0]++;
        transfer.position[1] += bytes.length;
        return transfer;
    }

    public Transfer<T> get(UUID player) { return transfers.get(player); }
    public Map<UUID, Transfer<T>> snapshot() { return Map.copyOf(transfers); }
    public Transfer<T> remove(UUID player) {
        Transfer<T> transfer = transfers.remove(player);
        if (transfer != null) reservedBytes -= transfer.bytes.length;
        return transfer;
    }
    public long reservedBytes() { return reservedBytes; }
    public void clear() { transfers.clear(); reservedBytes = 0; }
}
