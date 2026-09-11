package me.jamino.printer.image;

import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClients;

import java.io.InputStream;
import java.net.InetAddress;
import java.net.URI;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static me.jamino.printer.image.ImageFailure.Reason.*;

/** Direct connections resolve and validate through the SAME resolver used by the socket factory. */
public final class PublicImageDownloader {
    private static final Set<Integer> REDIRECTS = Set.of(301, 302, 303, 307, 308);
    private static final java.util.concurrent.ScheduledExecutorService DEADLINES =
            Executors.newSingleThreadScheduledExecutor(r -> {
                Thread thread = new Thread(r, "Printer download deadlines");
                thread.setDaemon(true);
                return thread;
            });

    private PublicImageDownloader() {}

    public static byte[] download(String url, int byteLimit, int timeoutSeconds,
                                  List<? extends String> allowed, List<? extends String> blocked) throws Exception {
        URI uri;
        try { uri = validateUri(URI.create(url)); }
        catch (IllegalArgumentException exception) { throw new ImageFailure(INVALID_URL); }
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(timeoutSeconds);
        // Apache HttpClient is part of Minecraft's server and client runtime. No proxy/system routing,
        // automatic redirects, retries, cookies or second DNS lookup bypasses this policy.
        try (var client = HttpClients.custom().disableRedirectHandling().disableAutomaticRetries()
                .disableCookieManagement().setDnsResolver(host -> {
                    InetAddress[] addresses = InetAddress.getAllByName(host);
                    for (InetAddress address : addresses) {
                        if (!isPublic(address.getAddress())) throw new java.net.UnknownHostException("Image host denied");
                    }
                    return addresses;
                }).build()) {
            for (int redirect = 0; redirect <= 3; redirect++) {
                validateHostName(uri.getHost(), allowed, blocked);
                long remaining = TimeUnit.NANOSECONDS.toMillis(deadline - System.nanoTime());
                if (remaining <= 0) throw new ImageFailure(TIMEOUT);
                HttpGet request = new HttpGet(uri);
                request.setConfig(RequestConfig.custom().setConnectTimeout((int) Math.min(15000, remaining))
                        .setSocketTimeout((int) remaining).setConnectionRequestTimeout((int) remaining).build());
                request.setHeader("User-Agent", "PrinterMod/0.1.0");
                request.setHeader("Accept", "image/*,application/octet-stream");
                var expiry = DEADLINES.schedule(request::abort, remaining, TimeUnit.MILLISECONDS);
                try (var response = client.execute(request)) {
                    int status = response.getStatusLine().getStatusCode();
                    if (REDIRECTS.contains(status)) {
                        var location = response.getFirstHeader("Location");
                        if (redirect == 3 || location == null) throw new ImageFailure(HTTP_ERROR);
                        uri = validateUri(uri.resolve(location.getValue()));
                        continue;
                    }
                    if (status < 200 || status >= 300 || response.getEntity() == null) throw new ImageFailure(HTTP_ERROR);
                    var contentType = response.getFirstHeader("Content-Type");
                    if (!ImageProcessor.acceptsContentType(contentType == null ? "" : contentType.getValue()))
                        throw new ImageFailure(UNSUPPORTED);
                    if (response.getEntity().getContentLength() > byteLimit) throw new ImageFailure(TOO_LARGE);
                    try (InputStream body = response.getEntity().getContent()) {
                        byte[] bytes = body.readNBytes(byteLimit + 1);
                        if (System.nanoTime() >= deadline) throw new ImageFailure(TIMEOUT);
                        if (bytes.length > byteLimit) throw new ImageFailure(TOO_LARGE);
                        return bytes;
                    }
                } catch (java.io.IOException exception) {
                    if (System.nanoTime() >= deadline || request.isAborted()) throw new ImageFailure(TIMEOUT);
                    if (exception instanceof java.net.UnknownHostException) throw new ImageFailure(HOST_DENIED);
                    if (exception instanceof ImageFailure) throw exception;
                    throw new ImageFailure(HTTP_ERROR);
                } finally {
                    expiry.cancel(false);
                    request.releaseConnection();
                }
            }
        }
        throw new ImageFailure(HTTP_ERROR);
    }

    static URI validateUri(URI uri) throws ImageFailure {
        String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase(Locale.ROOT);
        if ((!scheme.equals("https") && !scheme.equals("http")) || uri.getHost() == null
                || uri.getUserInfo() != null || uri.getFragment() != null) throw new ImageFailure(INVALID_URL);
        return uri;
    }

    static void validateHostName(String host, List<? extends String> allowed, List<? extends String> blocked)
            throws ImageFailure {
        if ((!allowed.isEmpty() && allowed.stream().noneMatch(value -> matches(host, value)))
                || blocked.stream().anyMatch(value -> matches(host, value))) throw new ImageFailure(HOST_DENIED);
    }

    private static boolean matches(String host, String pattern) {
        String normalized = pattern.trim().toLowerCase(Locale.ROOT);
        host = host.toLowerCase(Locale.ROOT);
        return !normalized.isEmpty() && (host.equals(normalized) || host.endsWith("." + normalized));
    }

    static boolean isPublic(byte[] bytes) {
        if (bytes.length == 4) {
            int a = bytes[0] & 255, b = bytes[1] & 255, c = bytes[2] & 255;
            return !(a == 0 || a == 10 || a == 127 || a >= 224 || (a == 100 && b >= 64 && b <= 127)
                    || (a == 169 && b == 254) || (a == 172 && b >= 16 && b <= 31) || (a == 192 && b == 168)
                    || (a == 192 && b == 0 && (c == 0 || c == 2)) || (a == 192 && b == 88 && c == 99)
                    || (a == 198 && (b == 18 || b == 19 || (b == 51 && c == 100)))
                    || (a == 203 && b == 0 && c == 113));
        }
        if (bytes.length != 16) return false;
        // Accept native global unicast only; exclude transition, documentation and special-purpose prefixes.
        int first = bytes[0] & 255, second = bytes[1] & 255;
        if ((first & 0xE0) != 0x20 || (first == 0x20 && second == 0x02)) return false;
        if (first == 0x20 && second == 0x01) {
            int third = bytes[2] & 255, fourth = bytes[3] & 255;
            if (third < 2 || (third == 0x0D && fourth == 0xB8)) return false;
        }
        return !(first == 0x3F && (second & 0xF0) == 0xF0);
    }
}
