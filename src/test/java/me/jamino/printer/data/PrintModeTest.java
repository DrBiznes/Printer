package me.jamino.printer.data;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

final class PrintModeTest {
    @Test void monochromeAllowsOnlyPureBlackOrWhite() {
        assertTrue(PrintMode.MONOCHROME.allowsBackground(0x000000));
        assertTrue(PrintMode.MONOCHROME.allowsBackground(0xFFFFFF));
        for (int color : new int[]{0xB02E26, 0x224466, 0x808080, 0x1D1D21, -1, 0x1000000})
            assertFalse(PrintMode.MONOCHROME.allowsBackground(color));
    }

    @Test void colorAllowsAllValidRgbBackgrounds() {
        for (int color : new int[]{0x000000, 0xFFFFFF, 0xB02E26, 0x224466, 0x808080, 0x1D1D21})
            assertTrue(PrintMode.COLOR.allowsBackground(color));
        assertFalse(PrintMode.COLOR.allowsBackground(-1));
        assertFalse(PrintMode.COLOR.allowsBackground(0x1000000));
    }
}
