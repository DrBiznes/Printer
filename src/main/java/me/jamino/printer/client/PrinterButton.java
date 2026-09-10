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
        var font = Minecraft.getInstance().font;
        graphics.drawCenteredString(font, getMessage(), x + width / 2,
                y + (height - 9) / 2 - 1 + offset, active ? 0xFFF8F1D7 : 0xFFB7B8A4);
    }
}
