package me.jamino.printer.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

import java.util.function.BooleanSupplier;

/** Texture-independent swatch; final GUI artwork can be changed separately. */
final class PrinterColorButton extends Button {
    private final int color;
    private final BooleanSupplier selected;

    PrinterColorButton(int x, int y, int color, Component label, BooleanSupplier selected, OnPress action) {
        super(x, y, 10, 8, label, action, DEFAULT_NARRATION);
        this.color = color;
        this.selected = selected;
    }

    int color() { return color; }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        int x = getX(), y = getY();
        graphics.fill(x, y, x + width, y + height, 0xFF242C2B);
        graphics.fill(x + 1, y + 1, x + width - 1, y + height - 1, 0xFF000000 | color);
        if (selected.getAsBoolean()) graphics.renderOutline(x, y, width, height, 0xFFF1CF7F);
        else if (active && isHoveredOrFocused()) graphics.renderOutline(x, y, width, height, 0xFFFFFFFF);
        if (!active) graphics.fill(x + 1, y + 1, x + width - 1, y + height - 1, 0x8072776A);
    }
}
