package me.jamino.printer.image;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import java.net.InetAddress;
import java.net.URI;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class PublicImageDownloaderTest {
    @ParameterizedTest
    @ValueSource(strings = {"0.1.2.3", "10.1.2.3", "100.64.0.1", "127.0.0.1", "169.254.169.254", "172.31.1.1",
            "192.168.0.1", "192.0.0.1", "192.0.2.1", "198.18.0.1", "198.51.100.1", "203.0.113.1", "224.0.0.1", "240.0.0.1",
            "::1", "::", "fc00::1", "fe80::1", "2001:db8::1", "2002:7f00:1::", "64:ff9b::7f00:1", "2001::1", "3fff::1"})
    void rejectsPrivateReservedDocumentationAndTransitionAddresses(String address) throws Exception {
        assertFalse(PublicImageDownloader.isPublic(InetAddress.getByName(address).getAddress()));
    }

    @ParameterizedTest @ValueSource(strings = {"8.8.8.8", "1.1.1.1", "2606:4700::1111", "2001:4860:4860::8888"})
    void acceptsNativePublicUnicast(String address) throws Exception {
        assertTrue(PublicImageDownloader.isPublic(InetAddress.getByName(address).getAddress()));
    }

    @Test void rejectsCredentialsNonHttpAndFragmentsAndMatchesHostBoundaries() throws Exception {
        for (String uri : List.of("file:///tmp/image.png", "ftp://example.com/a", "https://user:password@example.com/a", "https://example.com/#secret"))
            assertThrows(ImageFailure.class, () -> PublicImageDownloader.validateUri(URI.create(uri)));
        PublicImageDownloader.validateHostName("images.example.com", List.of("example.com"), List.of());
        assertThrows(ImageFailure.class, () -> PublicImageDownloader.validateHostName("notexample.com", List.of("example.com"), List.of()));
        assertThrows(ImageFailure.class, () -> PublicImageDownloader.validateHostName("a.example.com", List.of(), List.of("example.com")));
    }
}
