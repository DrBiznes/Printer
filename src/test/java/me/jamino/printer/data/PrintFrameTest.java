package me.jamino.printer.data;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PrintFrameTest {
    @Test
    void resolvesSavedNamesAndFallsBackSafely() {
        assertEquals(PrintFrame.SPRUCE, PrintFrame.byName("spruce"));
        assertEquals(PrintFrame.NONE, PrintFrame.byName("missing"));
        assertEquals(PrintFrame.NONE, PrintFrame.byName(null));
    }

    @Test
    void cyclesAcrossBothEnds() {
        assertEquals(PrintFrame.WHITE, PrintFrame.NONE.next(1));
        assertEquals(PrintFrame.COPPER, PrintFrame.NONE.next(-1));
        assertEquals(PrintFrame.NONE, PrintFrame.COPPER.next(1));
    }

    @Test
    void materialFramesUseVanillaTextures() {
        assertFalse(PrintFrame.NONE.isPresent());
        for (PrintFrame frame : PrintFrame.values()) {
            if (frame == PrintFrame.NONE) continue;
            assertTrue(frame.isPresent());
            assertNotNull(frame.texture());
            assertEquals("minecraft", frame.texture().getNamespace());
        }
    }
}
