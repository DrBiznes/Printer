package me.jamino.printer.image;

import me.jamino.printer.Config;
import me.jamino.printer.data.PrintMode;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.HexFormat;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class ImageProcessor {
    private static final Set<Integer> REDIRECTS = Set.of(301, 302, 303, 307, 308);

    private ImageProcessor() {}

    public static ProcessedImage downloadCanonical(String rawUrl) throws Exception {
        URI uri = validateUri(URI.create(rawUrl));
        int timeout = Config.SERVER.fetchTimeoutSeconds.get();
        int byteLimit = Config.SERVER.maxDownloadMiB.get() * 1024 * 1024;
        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(Math.min(timeout, 15)))
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();

        byte[] body = null;
        for (int redirects = 0; redirects <= 3; redirects++) {
            validateHost(uri);
            HttpRequest request = HttpRequest.newBuilder(uri)
                    .timeout(Duration.ofSeconds(timeout))
                    .header("User-Agent", "PrinterMod/1.0")
                    .header("Accept", "image/png,image/jpeg,image/gif")
                    .GET().build();
            HttpResponse<InputStream> response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());
            if (REDIRECTS.contains(response.statusCode())) {
                response.body().close();
                if (redirects == 3) throw new IOException("Too many redirects");
                String location = response.headers().firstValue("location")
                        .orElseThrow(() -> new IOException("Redirect without a Location header"));
                uri = validateUri(uri.resolve(location));
                continue;
            }
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                response.body().close();
                throw new IOException("Image server returned HTTP " + response.statusCode());
            }
            String contentType = response.headers().firstValue("content-type").orElse("")
                    .toLowerCase(Locale.ROOT).split(";", 2)[0].trim();
            if (!contentType.equals("image/png") && !contentType.equals("image/jpeg")
                    && !contentType.equals("image/gif")) {
                response.body().close();
                throw new IOException("Unsupported image content type: " + contentType);
            }
            try (InputStream stream = response.body()) {
                body = stream.readNBytes(byteLimit + 1);
            }
            if (body.length > byteLimit) throw new IOException("Image download exceeds the server limit");
            break;
        }
        if (body == null) throw new IOException("Image download failed");

        BufferedImage decoded = decodeChecked(body);
        int maxWidth = Config.SERVER.maxImageWidth.get();
        int maxHeight = Config.SERVER.maxImageHeight.get();
        double scale = Math.min(1.0, Math.min(maxWidth / (double) decoded.getWidth(), maxHeight / (double) decoded.getHeight()));
        int width = Math.max(1, (int) Math.round(decoded.getWidth() * scale));
        int height = Math.max(1, (int) Math.round(decoded.getHeight() * scale));
        BufferedImage canonical = resize(decoded, width, height);
        byte[] png = encodePng(canonical);
        return new ProcessedImage(hash(png), png, width, height);
    }

    public static ProcessedImage createVariant(byte[] sourcePng, int width, int height, PrintMode mode) throws IOException {
        return createVariant(sourcePng, width, height, mode == PrintMode.MONOCHROME);
    }

    static ProcessedImage createVariant(byte[] sourcePng, int width, int height, boolean monochrome) throws IOException {
        BufferedImage source = decodeChecked(sourcePng);
        BufferedImage resized = resize(source, width, height);
        if (monochrome) resized = monochrome(resized);
        byte[] png = encodePng(resized);
        return new ProcessedImage(hash(png), png, width, height);
    }

    public static BufferedImage decodeChecked(byte[] data) throws IOException {
        try (ImageInputStream stream = ImageIO.createImageInputStream(new ByteArrayInputStream(data))) {
            Iterator<ImageReader> readers = ImageIO.getImageReaders(stream);
            if (!readers.hasNext()) throw new IOException("The response is not a supported image");
            ImageReader reader = readers.next();
            try {
                reader.setInput(stream, true, true);
                int width = reader.getWidth(0);
                int height = reader.getHeight(0);
                if (width <= 0 || height <= 0 || width > 4096 || height > 4096
                        || (long) width * height > 16_777_216L) {
                    throw new IOException("Decoded image dimensions exceed the server limit");
                }
                BufferedImage result = reader.read(0);
                if (result == null) throw new IOException("Image decoder returned no pixels");
                return result;
            } finally {
                reader.dispose();
            }
        }
    }

    private static BufferedImage resize(BufferedImage source, int width, int height) {
        BufferedImage result = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = result.createGraphics();
        try {
            graphics.setColor(Color.WHITE);
            graphics.fillRect(0, 0, width, height);
            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            graphics.drawImage(source, 0, 0, width, height, null);
        } finally {
            graphics.dispose();
        }
        return result;
    }

    private static BufferedImage monochrome(BufferedImage source) {
        int width = source.getWidth();
        int height = source.getHeight();
        float[] luminance = new float[width * height];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int rgb = source.getRGB(x, y);
                luminance[x + y * width] = 0.2126F * ((rgb >> 16) & 255)
                        + 0.7152F * ((rgb >> 8) & 255) + 0.0722F * (rgb & 255);
            }
        }
        BufferedImage result = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int index = x + y * width;
                float oldValue = luminance[index];
                int value = oldValue < 128F ? 0 : 255;
                result.setRGB(x, y, value == 0 ? 0xFF000000 : 0xFFFFFFFF);
                float error = oldValue - value;
                if (x + 1 < width) luminance[index + 1] += error * 7F / 16F;
                if (y + 1 < height) {
                    if (x > 0) luminance[index + width - 1] += error * 3F / 16F;
                    luminance[index + width] += error * 5F / 16F;
                    if (x + 1 < width) luminance[index + width + 1] += error / 16F;
                }
            }
        }
        return result;
    }

    private static byte[] encodePng(BufferedImage image) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        if (!ImageIO.write(image, "png", output)) throw new IOException("PNG encoder is unavailable");
        byte[] png = output.toByteArray();
        if (png.length > 4 * 1024 * 1024) throw new IOException("Processed image exceeds 4 MiB");
        return png;
    }

    private static String hash(byte[] data) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(data));
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }

    private static URI validateUri(URI uri) throws IOException {
        String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase(Locale.ROOT);
        if (!scheme.equals("http") && !scheme.equals("https")) throw new IOException("Only HTTP and HTTPS URLs are allowed");
        if (uri.getUserInfo() != null) throw new IOException("URLs containing credentials are not allowed");
        if (uri.getHost() == null || uri.getHost().isBlank()) throw new IOException("URL has no valid host");
        return uri;
    }

    private static void validateHost(URI uri) throws IOException {
        String host = uri.getHost().toLowerCase(Locale.ROOT);
        List<? extends String> allowed = Config.SERVER.allowedHosts.get();
        List<? extends String> blocked = Config.SERVER.blockedHosts.get();
        if (!allowed.isEmpty() && allowed.stream().noneMatch(value -> hostMatches(host, value))) {
            throw new IOException("Image host is not on the server allowlist");
        }
        if (blocked.stream().anyMatch(value -> hostMatches(host, value))) {
            throw new IOException("Image host is blocked by the server");
        }
        for (InetAddress address : InetAddress.getAllByName(host)) {
            byte[] bytes = address.getAddress();
            boolean carrierGradeNat = bytes.length == 4 && (bytes[0] & 255) == 100 && ((bytes[1] & 192) == 64);
            boolean ipv6UniqueLocal = bytes.length == 16 && ((bytes[0] & 254) == 0xFC);
            if (address.isAnyLocalAddress() || address.isLoopbackAddress() || address.isLinkLocalAddress()
                    || address.isSiteLocalAddress() || address.isMulticastAddress() || carrierGradeNat || ipv6UniqueLocal) {
                throw new IOException("Image URL resolves to a non-public address");
            }
        }
    }

    private static boolean hostMatches(String host, String configured) {
        String value = configured.toLowerCase(Locale.ROOT).trim();
        return !value.isEmpty() && (host.equals(value) || host.endsWith("." + value));
    }
}
