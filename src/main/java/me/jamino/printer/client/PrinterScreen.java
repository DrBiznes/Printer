package me.jamino.printer.client;

import me.jamino.printer.inventory.PrinterMenu;
import me.jamino.printer.network.ModNetworking;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public final class PrinterScreen extends AbstractContainerScreen<PrinterMenu> {
    private EditBox url;
    private EditBox titleBox;
    private EditBox width;
    private EditBox height;

    public PrinterScreen(PrinterMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 176;
        imageHeight = 178;
        inventoryLabelY = 84;
    }

    @Override
    protected void init() {
        super.init();
        url = addRenderableWidget(new EditBox(font, leftPos + 10, topPos + 18, 156, 16,
                Component.translatable("gui.printer.url")));
        url.setHint(Component.translatable("gui.printer.url"));
        url.setMaxLength(2048);
        titleBox = addRenderableWidget(new EditBox(font, leftPos + 10, topPos + 38, 76, 16,
                Component.translatable("gui.printer.title")));
        titleBox.setHint(Component.translatable("gui.printer.title"));
        titleBox.setMaxLength(64);
        width = addRenderableWidget(new EditBox(font, leftPos + 90, topPos + 38, 35, 16, Component.literal("W")));
        height = addRenderableWidget(new EditBox(font, leftPos + 130, topPos + 38, 35, 16, Component.literal("H")));
        width.setValue("128");
        height.setValue("128");
        addRenderableWidget(Button.builder(Component.translatable("gui.printer.load"), button ->
                        ModNetworking.sendLoad(menu.getPos(), url.getValue(), titleBox.getValue(), number(width), number(height)))
                .bounds(leftPos + 78, topPos + 57, 48, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.printer.print"), button ->
                        ModNetworking.sendPrint(menu.getPos()))
                .bounds(leftPos + 78, topPos + 78, 48, 16).build());
    }

    private static int number(EditBox box) {
        try { return Integer.parseInt(box.getValue()); }
        catch (NumberFormatException ignored) { return 128; }
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, 0xFFB8B0A0);
        graphics.fill(leftPos + 4, topPos + 4, leftPos + imageWidth - 4, topPos + 92, 0xFF3A3A36);
        graphics.fill(leftPos + 8, topPos + 8, leftPos + imageWidth - 8, topPos + 88, 0xFF6F6B60);
        graphics.fill(leftPos + 24, topPos + 57, leftPos + 44, topPos + 77, 0xFF242421);
        graphics.fill(leftPos + 48, topPos + 57, leftPos + 68, topPos + 77, 0xFF242421);
        graphics.fill(leftPos + 138, topPos + 57, leftPos + 158, topPos + 77, 0xFF242421);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, this.title, titleLabelX, titleLabelY, 0xFFF4EAD0, false);
        if (menu.getPrinter() != null) {
            graphics.drawString(font, Component.translatable(menu.getPrinter().getStatusKey()), 8, 82, 0xFF202020, false);
        }
        graphics.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, 0xFF202020, false);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }
}
