package me.jamino.printer.client;

import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

/** A mechanical key with keyboard focus and visible click travel. */
final class PrinterButton extends Button {
    private final int faceColor;
    private long pressedUntil;
    private Icon icon;

    /** Pixel masks keep small symbols centered independently of font bearings. */
    enum Icon {
        LOAD("00011000", "00011000", "00011000", "01111110", "00111100", "00011000", "10000001", "11111111"),
        BROWSE("11100000", "10011110", "10000001", "10000001", "10000001", "10000001", "11111111", "00000000"),
        PRINT("00111100", "00100100", "11111111", "10000001", "10111101", "00100100", "00100100", "00111100"),
        MINUS("00000000", "00000000", "00000000", "01111110", "01111110", "00000000", "00000000", "00000000"),
        PLUS("00000000", "00011000", "00011000", "01111110", "01111110", "00011000", "00011000", "00000000"),
        CANCEL("10000001", "01000010", "00100100", "00011000", "00011000", "00100100", "01000010", "10000001");

        final String[] pixels;
        Icon(String... pixels) { this.pixels = pixels; }
    }

    void setIcon(Icon icon) { this.icon = icon; }

    PrinterButton(int x, int y, int width, int height, Component label, int color, OnPress action) {
        super(x, y, width, height, label, action, DEFAULT_NARRATION);
        faceColor = color;
    }

    @Override
    public void onPress() {
        pressedUntil = Util.getMillis() + 150;
        super.onPress();
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        int x = getX(), y = getY();
        int offset = active && Util.getMillis() < pressedUntil ? 2 : 0;
        graphics.fill(x, y, x + width, y + height, 0xFF242C2B);
        graphics.fill(x + 1, y + 1 + offset, x + width - 1, y + height - 3 + offset,
                active ? faceColor : 0xFF72776A);
        graphics.fill(x + 1, y + 1 + offset, x + width - 1, y + 2 + offset,
                active ? 0xFFE8E4C9 : 0xFF969B8C);
        if (active && isHoveredOrFocused()) graphics.renderOutline(x, y, width, height, 0xFFF1CF7F);
        if (icon != null) {
            int ix = x + (width - 8) / 2;
            int iy = y + (height - 2 - 8) / 2 + offset;
            for (int row = 0; row < 8; row++) {
                for (int col = 0; col < 8; col++) {
                    if (icon.pixels[row].charAt(col) == '1') {
                        graphics.fill(ix + col, iy + row, ix + col + 1, iy + row + 1,
                                active ? 0xFFF8F1D7 : 0xFFB7B8A4);
                    }
                }
            }
            return;
        }
        var font = Minecraft.getInstance().font;
        graphics.drawCenteredString(font, getMessage(), x + width / 2,
                y + (height - 9) / 2 - 1 + offset, active ? 0xFFF8F1D7 : 0xFFB7B8A4);
    }
}
