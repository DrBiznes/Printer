package me.jamino.printer.client;

import me.jamino.printer.image.ImageFailure;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import static org.junit.jupiter.api.Assertions.*;

class LocalFileReadTest {
    @TempDir Path directory;
    @Test void readsOnlyRegularFilesWithinServerByteLimit() throws Exception {
        var file = directory.resolve("local.png"); Files.write(file, new byte[]{1,2,3});
        assertArrayEquals(new byte[]{1,2,3}, ClientFileUpload.readFile(file, 3));
        assertEquals(ImageFailure.Reason.TOO_LARGE, assertThrows(ImageFailure.class, () -> ClientFileUpload.readFile(file, 2)).reason());
        assertEquals(ImageFailure.Reason.READ_FAILED, assertThrows(ImageFailure.class, () -> ClientFileUpload.readFile(directory, 3)).reason());
        Files.write(file, new byte[0]);
        assertEquals(ImageFailure.Reason.TOO_LARGE, assertThrows(ImageFailure.class, () -> ClientFileUpload.readFile(file, 3)).reason());
    }
}
