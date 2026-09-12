package me.jamino.printer.image;

import me.jamino.printer.Config;
import me.jamino.printer.data.PrintMode;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.MemoryCacheImageInputStream;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Iterator;
import java.util.Locale;
import java.util.Set;

public final class ImageProcessor {
    private static final Set<String> FORMATS = Set.of("png", "jpeg", "jpg", "gif", "webp", "bmp", "tiff", "tif", "ico", "tga");
    private static final Set<String> CONTENT_TYPES = Set.of("image/png", "image/jpeg", "image/jpg",
            "image/gif", "image/webp", "image/bmp", "image/x-bmp", "image/x-ms-bmp",
            "image/tiff", "image/x-tiff", "image/x-tga", "image/tga", "image/x-targa",
            "image/vnd.microsoft.icon", "image/x-icon", "application/octet-stream", "binary/octet-stream");

    private ImageProcessor() {}

    public static ProcessedImage downloadCanonical(String rawUrl) throws Exception {
        return canonicalize(PublicImageDownloader.download(rawUrl,
                Config.SERVER.maxDownloadMiB.get() * 1024 * 1024, Config.SERVER.fetchTimeoutSeconds.get(),
                Config.SERVER.allowedHosts.get(), Config.SERVER.blockedHosts.get()));
    }

    public static ProcessedImage canonicalize(byte[] body) throws IOException {
        if (body.length == 0 || body.length > Config.SERVER.maxDownloadMiB.get() * 1024 * 1024)
            throw new ImageFailure(ImageFailure.Reason.TOO_LARGE);
        return canonicalize(body, Config.SERVER.maxImageWidth.get(), Config.SERVER.maxImageHeight.get());
    }

    static ProcessedImage canonicalize(byte[] body, int maxWidth, int maxHeight) throws IOException {
        BufferedImage decoded = decodeChecked(body);
        double scale = Math.min(1.0, Math.min(maxWidth / (double) decoded.getWidth(), maxHeight / (double) decoded.getHeight()));
        int width = Math.max(1, (int) Math.round(decoded.getWidth() * scale));
        int height = Math.max(1, (int) Math.round(decoded.getHeight() * scale));
        BufferedImage canonical = resize(decoded, width, height);
        byte[] png = encodePng(canonical);
        return new ProcessedImage(hash(png), png, width, height, decoded.getWidth(), decoded.getHeight());
    }

    public static ProcessedImage createVariant(byte[] sourcePng, int width, int height, PrintMode mode, int backgroundColor) throws IOException {
        return createVariant(sourcePng, width, height, mode == PrintMode.MONOCHROME, backgroundColor);
    }

    static ProcessedImage createVariant(byte[] sourcePng, int width, int height, boolean monochrome, int backgroundColor) throws IOException {
        if (monochrome && !PrintMode.MONOCHROME.allowsBackground(backgroundColor))
            throw new ImageFailure(ImageFailure.Reason.MONOCHROME_BACKGROUND);
        BufferedImage source = decodeChecked(sourcePng);
        BufferedImage artwork = resize(source, width, height);
        // Ink conversion is independent of paper color. Otherwise colored paper
        // becomes a noisy black/white pattern and changes the artwork's dithering.
        if (monochrome) artwork = monochromeArtwork(artwork);
        byte[] png = encodePng(composite(artwork, backgroundColor));
        return new ProcessedImage(hash(png), png, width, height);
    }

    public static BufferedImage decodeChecked(byte[] data) throws IOException {
        registerBundledReaders();
        // Do not require a writable temporary directory on a headless server.
        try (ImageInputStream stream = new MemoryCacheImageInputStream(new ByteArrayInputStream(data))) {
            Iterator<ImageReader> readers = ImageIO.getImageReaders(stream);
            ImageReader reader = null;
            while (readers.hasNext()) {
                ImageReader candidate = readers.next();
                if (FORMATS.contains(candidate.getFormatName().toLowerCase(Locale.ROOT))) {
                    reader = candidate;
                    break;
                }
                candidate.dispose();
            }
            // Other mods may install additional ImageIO providers. Accept only
            // our raster formats even when a CDN supplies a generic MIME type.
            if (reader == null) throw new ImageFailure(ImageFailure.Reason.UNSUPPORTED);
            try {
                reader.setInput(stream, true, true);
                int width = reader.getWidth(0);
                int height = reader.getHeight(0);
                if (width <= 0 || height <= 0 || width > 4096 || height > 4096
                        || (long) width * height > 16_777_216L) {
                    throw new ImageFailure(ImageFailure.Reason.DIMENSIONS);
                }
                BufferedImage result = reader.read(0);
                if (result == null) throw new IOException("Image decoder returned no pixels");
                return result;
            } finally {
                reader.dispose();
            }
        }
    }

    private static void registerBundledReaders() {
        // NeoForge puts nested libraries in its game module layer. ImageIO's
        // context-classloader discovery can miss them (especially in integrated
        // servers), so register the known providers in the current AWT registry.
        // The registry is AppContext scoped, not process scoped; do not cache a
        // global "initialized" flag across server/client thread groups.
        var registry = javax.imageio.spi.IIORegistry.getDefaultInstance();
        synchronized (registry) {
            if (registry.getServiceProviderByClass(com.twelvemonkeys.imageio.plugins.webp.WebPImageReaderSpi.class) == null) {
                registry.registerServiceProvider(new com.twelvemonkeys.imageio.plugins.webp.WebPImageReaderSpi());
                registry.registerServiceProvider(new com.twelvemonkeys.imageio.plugins.bmp.BMPImageReaderSpi());
                registry.registerServiceProvider(new com.twelvemonkeys.imageio.plugins.bmp.ICOImageReaderSpi());
                registry.registerServiceProvider(new com.twelvemonkeys.imageio.plugins.tiff.TIFFImageReaderSpi());
                registry.registerServiceProvider(new com.twelvemonkeys.imageio.plugins.tga.TGAImageReaderSpi());
                registry.registerServiceProvider(new com.twelvemonkeys.imageio.plugins.jpeg.JPEGImageReaderSpi());
            }
        }
    }

    static boolean acceptsContentType(String contentType) {
        // CDNs sometimes omit the type or send binary data. The decoder still
        // validates the actual bytes and dimensions; HTML/SVG are not accepted.
        String normalized = contentType.toLowerCase(Locale.ROOT).split(";", 2)[0].trim();
        return normalized.isEmpty() || CONTENT_TYPES.contains(normalized);
    }

    private static BufferedImage resize(BufferedImage source, int width, int height) {
        BufferedImage result = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = result.createGraphics();
        try {
            // Canonical sources retain alpha. Only a print variant chooses paper color.
            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            graphics.drawImage(source, 0, 0, width, height, null);
        } finally {
            graphics.dispose();
        }
        return result;
    }

    private static BufferedImage composite(BufferedImage source, int backgroundColor) {
        BufferedImage result = new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = result.createGraphics();
        try {
            graphics.setColor(new Color(backgroundColor & 0xFFFFFF));
            graphics.fillRect(0, 0, result.getWidth(), result.getHeight());
            graphics.drawImage(source, 0, 0, null);
        } finally {
            graphics.dispose();
        }
        return result;
    }

    private static BufferedImage monochromeArtwork(BufferedImage source) {
        int width = source.getWidth();
        int height = source.getHeight();
        int[] pixels = source.getRGB(0, 0, width, height, null, 0, width);
        float[] luminance = new float[width * height];
        for (int index = 0; index < pixels.length; index++) {
            int rgb = pixels[index];
            luminance[index] = 0.2126F * ((rgb >> 16) & 255)
                    + 0.7152F * ((rgb >> 8) & 255) + 0.0722F * (rgb & 255);
        }
        BufferedImage result = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int index = x + y * width;
                int alpha = pixels[index] >>> 24;
                // Transparent pixels are paper, not artwork: their hidden RGB
                // must neither produce ink nor carry error into another region.
                if (alpha == 0) continue;
                float oldValue = luminance[index];
                int value = oldValue < 128F ? 0 : 255;
                result.setRGB(x, y, alpha << 24 | (value == 0 ? 0 : 0xFFFFFF));
                // Keep soft cutout edges; faint coverage contributes less error.
                float error = (oldValue - value) * (alpha / 255F);
                if (x + 1 < width) diffuseArtworkError(luminance, pixels, index + 1, error * 7F / 16F);
                if (y + 1 < height) {
                    if (x > 0) diffuseArtworkError(luminance, pixels, index + width - 1, error * 3F / 16F);
                    diffuseArtworkError(luminance, pixels, index + width, error * 5F / 16F);
                    if (x + 1 < width) diffuseArtworkError(luminance, pixels, index + width + 1, error / 16F);
                }
            }
        }
        return result;
    }

    private static void diffuseArtworkError(float[] luminance, int[] pixels, int index, float error) {
        if ((pixels[index] >>> 24) != 0) luminance[index] += error;
    }

    private static byte[] encodePng(BufferedImage image) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        if (!ImageIO.write(image, "png", output)) throw new IOException("PNG encoder is unavailable");
        byte[] png = output.toByteArray();
        if (png.length > 4 * 1024 * 1024) throw new ImageFailure(ImageFailure.Reason.TOO_LARGE);
        return png;
    }

    private static String hash(byte[] data) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(data));
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }

}
