package me.jamino.printer.network;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Gallery-friendly request bursts, with separate sustained response-byte limits. */
public final class ImageRequestBudget {
    private static final int REQUEST_BURST = 16;
    private static final long BYTE_BURST = 32L * 1024 * 1024;
    private final Map<UUID, Bucket> players = new HashMap<>();

    public boolean acquire(UUID player, int bytes, long now) {
        if (bytes < 0 || bytes > ImageTransfers.MAX_BYTES) return false;
        Bucket bucket = players.computeIfAbsent(player, ignored -> new Bucket(now));
        double seconds = Math.max(0, now - bucket.updated) / 1_000_000_000.0;
        bucket.requests = Math.min(REQUEST_BURST, bucket.requests + seconds * 10);
        bucket.bytes = Math.min(BYTE_BURST, bucket.bytes + seconds * 8 * 1024 * 1024);
        bucket.updated = now;
        if (bytes == 0) {
            if (bucket.requests < 1) return false;
            bucket.requests--;
        } else {
            if (bucket.bytes < bytes) return false;
            bucket.bytes -= bytes;
        }
        return true;
    }

    public void remove(UUID player) { players.remove(player); }
    public void clear() { players.clear(); }

    private static final class Bucket {
        double requests = REQUEST_BURST, bytes = BYTE_BURST;
        long updated;
        Bucket(long now) { updated = now; }
    }
}
