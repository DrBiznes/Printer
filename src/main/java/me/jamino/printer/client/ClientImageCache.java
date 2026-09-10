package me.jamino.printer.client;

import com.mojang.blaze3d.platform.NativeImage;
import me.jamino.printer.Config;
import me.jamino.printer.Printer;
import me.jamino.printer.network.ModNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public final class ClientImageCache {
    private static final Map<String, Assembly> ASSEMBLIES = new HashMap<>();
    private static final Map<String, Long> PENDING = new HashMap<>();
    private static final long RETRY_AFTER_MILLIS = 5_000L;
    private static final LinkedHashMap<String, CachedTexture> TEXTURES = new LinkedHashMap<>(16, 0.75F, true);
    private static long cachedBytes;

    private ClientImageCache() {}

    public static ResourceLocation getOrRequest(String contentId) {
        CachedTexture cached = TEXTURES.get(contentId);
        if (cached != null) return cached.location;
        if (contentId == null || !contentId.matches("[0-9a-f]{64}")) return null;
        long now = System.currentTimeMillis();
        Long requestedAt = PENDING.get(contentId);
        if (requestedAt == null || now - requestedAt >= RETRY_AFTER_MILLIS) {
            PENDING.put(contentId, now);
            ModNetworking.requestImage(contentId);
        }
        return null;
    }

    public static void accept(ModNetworking.ImageChunkPayload payload) {
        if (!payload.contentId().matches("[0-9a-f]{64}") || payload.total() <= 0 || payload.total() > 32
                || payload.index() < 0 || payload.index() >= payload.total()) return;
        Assembly assembly = ASSEMBLIES.computeIfAbsent(payload.contentId(), ignored -> new Assembly(payload.total()));
        if (assembly.total != payload.total()) {
            ASSEMBLIES.remove(payload.contentId());
            PENDING.remove(payload.contentId());
            return;
        }
        assembly.chunks.putIfAbsent(payload.index(), payload.bytes());
        if (assembly.chunks.size() != assembly.total) return;
        ASSEMBLIES.remove(payload.contentId());
        // Keep a retry deadline during decoding. A failed image must not be
        // requested again every rendered frame and flood the client task queue.
        PENDING.put(payload.contentId(), System.currentTimeMillis());
        try {
            ByteArrayOutputStream joined = new ByteArrayOutputStream();
            for (int i = 0; i < assembly.total; i++) joined.write(assembly.chunks.get(i));
            byte[] png = joined.toByteArray();
            NativeImage image = decodePng(png);
            DynamicTexture texture = new DynamicTexture(image);
            ResourceLocation location = Minecraft.getInstance().getTextureManager()
                    .register("printer_" + payload.contentId().substring(0, 12), texture);
            long textureBytes = (long) image.getWidth() * image.getHeight() * 4;
            texture.setFilter(true, false);
            CachedTexture previous = TEXTURES.put(payload.contentId(), new CachedTexture(location, texture, textureBytes));
            if (previous != null) {
                cachedBytes -= previous.bytes;
                previous.close();
            }
            cachedBytes += textureBytes;
            PENDING.remove(payload.contentId());
            Printer.LOGGER.info("Registered printer image texture {} ({} bytes)", payload.contentId(), png.length);
            evict();
        } catch (Exception exception) {
            Printer.LOGGER.warn("Failed to decode transferred printer image {}", payload.contentId(), exception);
        }
    }

    static NativeImage decodePng(byte[] png) throws IOException {
        // The byte[] overload copies the whole PNG into LWJGL's tiny native
        // stack. Real photos exceed that stack even at modest resolutions.
        // The stream overload allocates/frees a native heap buffer instead.
        return NativeImage.read(new ByteArrayInputStream(png));
    }

    public static void clear() {
        TEXTURES.values().forEach(CachedTexture::close);
        TEXTURES.clear();
        ASSEMBLIES.clear();
        PENDING.clear();
        cachedBytes = 0;
    }

    private static void evict() {
        long limit = Config.CLIENT.textureCacheMiB.get() * 1024L * 1024L;
        var iterator = TEXTURES.entrySet().iterator();
        // Keep a single large image usable even when it exceeds the soft cache
        // budget; evicting it immediately would cause an endless request loop.
        while (cachedBytes > limit && TEXTURES.size() > 1 && iterator.hasNext()) {
            CachedTexture texture = iterator.next().getValue();
            cachedBytes -= texture.bytes;
            texture.close();
            iterator.remove();
        }
    }

    private static final class Assembly {
        final int total;
        final Map<Integer, byte[]> chunks = new HashMap<>();
        Assembly(int total) { this.total = total; }
    }

    private record CachedTexture(ResourceLocation location, DynamicTexture texture, long bytes) {
        void close() { Minecraft.getInstance().getTextureManager().release(location); }
    }
}
