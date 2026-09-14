package me.jamino.printer.client;

import net.minecraft.client.gui.GuiGraphics;

/**
 * The slot styling baked into the printer panel texture, redrawn for screens that
 * paint their background procedurally. Keeping the colours here means the photobook
 * and the printer stay identical; sampled from textures/gui/printer.png.
 */
public final class SlotPanel {
    /** Tray the slots are recessed into. */
    public static final int TRAY = 0xFF9C9C80;
    /** Top edge capping the tray. */
    public static final int TRAY_EDGE = 0xFF596052;
    /** Slot border. */
    public static final int BORDER = 0xFF555F51;
    /** Inner shadow along the slot's top edge. */
    public static final int SHADOW = 0xFF737E69;
    /** Slot interior. */
    public static final int INTERIOR = 0xFF8D957D;
    /** Bevel along the slot's bottom and right edges. */
    public static final int BEVEL = 0xFFE8DFBA;

    private SlotPanel() {}

    /**
     * Draws one 16x16 slot with its surrounding border, where {@code x}/{@code y} are the
     * slot's item origin. Matches the printer texture pixel for pixel: a one-pixel border,
     * a shadow row under the top border, and a bevel outside the bottom and right edges.
     */
    /**
     * Fills the tray the slots are recessed into: a dark cap along the top, the tray body,
     * and the lit bevel closing its bottom and right edges. {@code x}/{@code y} are the
     * tray's top-left corner in absolute screen pixels.
     */
    public static void tray(GuiGraphics graphics, int x, int y, int width, int height) {
        graphics.fill(x, y, x + width, y + 1, TRAY_EDGE);
        graphics.fill(x, y + 1, x + width, y + height, TRAY);
        graphics.fill(x, y + height - 1, x + width, y + height, BEVEL);
        graphics.fill(x + width - 1, y + 1, x + width, y + height, BEVEL);
    }

    public static void slot(GuiGraphics graphics, int x, int y) {
        graphics.fill(x - 1, y - 1, x + 17, y + 17, BORDER);
        graphics.fill(x, y, x + 16, y + 16, INTERIOR);
        graphics.fill(x, y, x + 16, y + 1, SHADOW);
        // The bevel sits outside the border on the bottom and right, catching the light.
        graphics.fill(x - 1, y + 16, x + 18, y + 17, BEVEL);
        graphics.fill(x + 16, y - 1, x + 17, y + 17, BEVEL);
    }
}
