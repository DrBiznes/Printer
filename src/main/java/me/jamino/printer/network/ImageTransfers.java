package me.jamino.printer.network;

import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.Map;
import java.security.MessageDigest;

/** Bounded assembly of server image responses; independent of native client textures. */
public final class ImageTransfers {
    public static final int CHUNK_BYTES = 256 * 1024;
    public static final int MAX_BYTES = 4 * 1024 * 1024;
    private static final long TIMEOUT_MS = 15_000;
    private static final int GLOBAL_BYTES = 16 * 1024 * 1024;
    private final Map<String, Assembly> active = new HashMap<>();
    private int reserved;

    public byte[] accept(String id, int index, int total, byte[] bytes, long now) {
        expire(now);
        if (id == null || !id.matches("[0-9a-f]{64}") || total < 1 || total > MAX_BYTES / CHUNK_BYTES
                || index < 0 || index >= total || bytes.length == 0 || bytes.length > CHUNK_BYTES
                || (index < total - 1 && bytes.length != CHUNK_BYTES)) { remove(id); return null; }
        Assembly assembly = active.get(id);
        if (assembly == null) {
            int capacity = total * CHUNK_BYTES;
            if (index != 0 || active.size() >= 16 || reserved + capacity > GLOBAL_BYTES) return null;
            assembly = new Assembly(total, now + TIMEOUT_MS);
            active.put(id, assembly); reserved += capacity;
        }
        if (total != assembly.total || index != assembly.next) { remove(id); return null; }
        assembly.data.writeBytes(bytes); assembly.next++;
        if (assembly.next != total) return null;
        byte[] result = assembly.data.toByteArray(); remove(id);
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(result)).equals(id) ? result : null;
        } catch (java.security.NoSuchAlgorithmException impossible) { throw new IllegalStateException(impossible); }
    }

    public void expire(long now) {
        var iterator = active.values().iterator();
        while (iterator.hasNext()) {
            var assembly = iterator.next();
            if (now >= assembly.deadline) { reserved -= assembly.total * CHUNK_BYTES; iterator.remove(); }
        }
    }
    public void remove(String id) { var old = active.remove(id); if (old != null) reserved -= old.total * CHUNK_BYTES; }
    public void clear() { active.clear(); reserved = 0; }
    int reservedBytes() { return reserved; }
    private static final class Assembly {
        final int total; final long deadline;
        final ByteArrayOutputStream data = new ByteArrayOutputStream();
        int next;
        Assembly(int total, long deadline) { this.total = total; this.deadline = deadline; }
    }
}
