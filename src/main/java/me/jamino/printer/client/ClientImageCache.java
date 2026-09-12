package me.jamino.printer.client;

import com.mojang.blaze3d.platform.NativeImage;
import me.jamino.printer.Config;
import me.jamino.printer.Printer;
import me.jamino.printer.network.ModNetworking;
import me.jamino.printer.network.ImageTransfers;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FastColor;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.HashMap;

public final class ClientImageCache {
    private static final ImageTransfers ASSEMBLIES = new ImageTransfers();
    private static final Map<String, Long> PENDING = new HashMap<>();
    private static final Map<String, CachedTexture> MONOCHROME_TEXTURES = new HashMap<>();
    private static final long RETRY_AFTER_MILLIS = 5_000L;
    private static final LinkedHashMap<String, CachedTexture> TEXTURES = new LinkedHashMap<>(16, 0.75F, true);
    private static long cachedBytes;

    private ClientImageCache() {}

    public static ResourceLocation getOrRequest(String contentId) {
        CachedTexture cached = TEXTURES.get(contentId);
        if (cached != null) return cached.location;
        if (contentId == null || !contentId.matches("[0-9a-f]{64}")) return null;
        long now = System.currentTimeMillis();
        ASSEMBLIES.expire(now);
        PENDING.entrySet().removeIf(entry -> now - entry.getValue() >= RETRY_AFTER_MILLIS);
        Long requestedAt = PENDING.get(contentId);
        if (requestedAt == null && PENDING.size() < 16) {
            ASSEMBLIES.remove(contentId);
            PENDING.put(contentId, now);
            ModNetworking.requestImage(contentId);
        }
        return null;
    }

    public static ResourceLocation getOrRequest(String contentId, boolean monochrome) {
        ResourceLocation canonical = getOrRequest(contentId);
        if (!monochrome || canonical == null) return canonical;

        CachedTexture cached = MONOCHROME_TEXTURES.get(contentId);
        if (cached != null) return cached.location;
        CachedTexture source = TEXTURES.get(contentId);
        if (source == null || source.texture.getPixels() == null) return canonical;

        NativeImage image = monochrome(source.texture.getPixels());
        DynamicTexture texture = new DynamicTexture(image);
        texture.setFilter(true, false);
        ResourceLocation location = Minecraft.getInstance().getTextureManager()
                .register("printer_monochrome_" + contentId.substring(0, 12), texture);
        CachedTexture derived = new CachedTexture(location, texture,
                (long) image.getWidth() * image.getHeight() * 4);
        MONOCHROME_TEXTURES.put(contentId, derived);
        cachedBytes += derived.bytes;
        evict();
        return location;
    }

    public static void accept(ModNetworking.ImageChunkPayload payload) {
        if (!PENDING.containsKey(payload.contentId())) return;
        byte[] png = ASSEMBLIES.accept(payload.contentId(), payload.index(), payload.total(), payload.bytes(), System.currentTimeMillis());
        if (png == null) return;
        // Keep a retry deadline during decoding. A failed image must not be
        // requested again every rendered frame and flood the client task queue.
        PENDING.put(payload.contentId(), System.currentTimeMillis());
        try {
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
            closeMonochrome(payload.contentId());
            cachedBytes += textureBytes;
            PENDING.remove(payload.contentId());
            Printer.LOGGER.debug("Registered printer image texture ({} bytes)", png.length);
            evict();
        } catch (Exception exception) {
            Printer.LOGGER.debug("Failed to decode transferred printer image");
        }
    }

    static NativeImage decodePng(byte[] png) throws IOException {
        // Bound the native allocation before passing the server's PNG to STB.
        if (png.length < 24 || png.length > ImageTransfers.MAX_BYTES
                || java.nio.ByteBuffer.wrap(png).getLong() != 0x89504E470D0A1A0AL)
            throw new IOException("Invalid printer PNG");
        var header = java.nio.ByteBuffer.wrap(png);
        int width = header.getInt(16), height = header.getInt(20);
        if (width < 1 || height < 1 || width > 4096 || height > 4096)
            throw new IOException("Printer PNG dimensions exceed limits");
        // The byte[] overload copies the whole PNG into LWJGL's tiny native
        // stack. Real photos exceed that stack even at modest resolutions.
        // The stream overload allocates/frees a native heap buffer instead.
        return NativeImage.read(new ByteArrayInputStream(png));
    }

    public static void clear() {
        MONOCHROME_TEXTURES.values().forEach(CachedTexture::close);
        MONOCHROME_TEXTURES.clear();
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
            var entry = iterator.next();
            String contentId = entry.getKey();
            CachedTexture texture = entry.getValue();
            cachedBytes -= texture.bytes;
            closeMonochrome(contentId);
            texture.close();
            iterator.remove();
        }
    }

    private static void closeMonochrome(String contentId) {
        CachedTexture texture = MONOCHROME_TEXTURES.remove(contentId);
        if (texture != null) {
            cachedBytes -= texture.bytes;
            texture.close();
        }
    }

    private static NativeImage monochrome(NativeImage source) {
        int width = source.getWidth();
        int height = source.getHeight();
        int[] pixels = source.getPixelsRGBA();
        float[] luminance = new float[pixels.length];
        for (int index = 0; index < pixels.length; index++) {
            int pixel = pixels[index];
            luminance[index] = 0.2126F * FastColor.ABGR32.red(pixel)
                    + 0.7152F * FastColor.ABGR32.green(pixel)
                    + 0.0722F * FastColor.ABGR32.blue(pixel);
        }

        NativeImage result = new NativeImage(width, height, true);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int index = x + y * width;
                int alpha = FastColor.ABGR32.alpha(pixels[index]);
                if (alpha == 0) continue;
                float oldValue = luminance[index];
                int value = oldValue < 128F ? 0 : 255;
                result.setPixelRGBA(x, y, FastColor.ABGR32.color(alpha, value, value, value));
                float error = (oldValue - value) * (alpha / 255F);
                if (x + 1 < width) diffuseError(luminance, pixels, index + 1, error * 7F / 16F);
                if (y + 1 < height) {
                    if (x > 0) diffuseError(luminance, pixels, index + width - 1, error * 3F / 16F);
                    diffuseError(luminance, pixels, index + width, error * 5F / 16F);
                    if (x + 1 < width) diffuseError(luminance, pixels, index + width + 1, error / 16F);
                }
            }
        }
        return result;
    }

    private static void diffuseError(float[] luminance, int[] pixels, int index, float error) {
        if (FastColor.ABGR32.alpha(pixels[index]) != 0) luminance[index] += error;
    }

    private record CachedTexture(ResourceLocation location, DynamicTexture texture, long bytes) {
        void close() { Minecraft.getInstance().getTextureManager().release(location); }
    }
}
