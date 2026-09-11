package me.jamino.printer.job;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Server-thread rate limiter; entries are removed at disconnect and server stop. */
public final class RequestThrottle {
    private final Map<UUID, Long> lastRequest = new HashMap<>();
    public boolean acquire(UUID player, long now, long interval) {
        Long previous = lastRequest.get(player);
        if (previous != null && now - previous < interval) return false;
        lastRequest.put(player, now);
        return true;
    }
    public void remove(UUID player) { lastRequest.remove(player); }
    public void clear() { lastRequest.clear(); }
}
