package me.jamino.printer.client;

import me.jamino.printer.inventory.PrinterMenu;
import me.jamino.printer.network.ModNetworking;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import me.jamino.printer.data.PrinterPreset;

public final class PrinterScreen extends AbstractContainerScreen<PrinterMenu> {
    private EditBox url;
    private EditBox titleBox;
    private Button smaller;
    private Button larger;
    private Button frame;

    public PrinterScreen(PrinterMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 176;
        imageHeight = 228;
        inventoryLabelY = 134;
    }

    @Override
    protected void init() {
        super.init();
        url = addRenderableWidget(new EditBox(font, leftPos + 10, topPos + 18, 156, 16,
                Component.translatable("gui.printer.url")));
        url.setHint(Component.translatable("gui.printer.url"));
        url.setMaxLength(2048);
        titleBox = addRenderableWidget(new EditBox(font, leftPos + 10, topPos + 38, 96, 16,
                Component.translatable("gui.printer.title")));
        titleBox.setHint(Component.translatable("gui.printer.title"));
        titleBox.setMaxLength(64);
        addRenderableWidget(Button.builder(Component.translatable("gui.printer.load"), button ->
                        ModNetworking.sendLoad(menu.getPos(), url.getValue(), titleBox.getValue()))
                .bounds(leftPos + 110, topPos + 38, 56, 16).build());
        smaller = addRenderableWidget(Button.builder(Component.literal("−"), button ->
                        ModNetworking.sendResize(menu.getPos(), -1))
                .bounds(leftPos + 8, topPos + 79, 18, 18).build());
        larger = addRenderableWidget(Button.builder(Component.literal("+"), button ->
                        ModNetworking.sendResize(menu.getPos(), 1))
                .bounds(leftPos + 72, topPos + 79, 18, 18).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.printer.print"), button ->
                        ModNetworking.sendPrint(menu.getPos()))
                .bounds(leftPos + 118, topPos + 79, 48, 18).build());
        frame = addRenderableWidget(Button.builder(Component.translatable("gui.printer.frame.none"), button ->
                        ModNetworking.sendCycleFrame(menu.getPos(), 1))
                .bounds(leftPos + 8, topPos + 99, 158, 16).build());
        updateSizeButtons();
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        updateSizeButtons();
    }

    private void updateSizeButtons() {
        PrinterPreset preset = menu.getPrinter() == null ? null : menu.getPrinter().getPreset().orElse(null);
        boolean available = preset != null && !menu.getPrinter().isPrinting();
        if (smaller != null) smaller.active = available && Math.max(preset.blocksWide(), preset.blocksHigh()) > 1;
        if (larger != null) larger.active = available && Math.max(preset.blocksWide(), preset.blocksHigh()) < 8;
        if (frame != null) {
            frame.active = available;
            frame.setMessage(preset == null
                    ? Component.translatable("gui.printer.frame.none")
                    : Component.translatable("gui.printer.frame", Component.translatable(
                    "gui.printer.frame." + preset.frame().getSerializedName())));
        }
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, 0xFFB8B0A0);
        graphics.fill(leftPos + 4, topPos + 4, leftPos + imageWidth - 4, topPos + 130, 0xFF3A3A36);
        graphics.fill(leftPos + 8, topPos + 8, leftPos + imageWidth - 8, topPos + 126, 0xFF6F6B60);
        graphics.fill(leftPos + 24, topPos + 57, leftPos + 44, topPos + 77, 0xFF242421);
        graphics.fill(leftPos + 48, topPos + 57, leftPos + 68, topPos + 77, 0xFF242421);
        graphics.fill(leftPos + 138, topPos + 57, leftPos + 158, topPos + 77, 0xFF242421);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, this.title, titleLabelX, titleLabelY, 0xFFF4EAD0, false);
        if (menu.getPrinter() != null) {
            PrinterPreset preset = menu.getPrinter().getPreset().orElse(null);
            if (preset != null) {
                graphics.drawCenteredString(font, Component.translatable("gui.printer.size_compact",
                        preset.blocksWide(), preset.blocksHigh(), preset.requiredPaper()), 49, 84, 0xFFF4EAD0);
            } else {
                graphics.drawCenteredString(font, Component.literal("—"), 49, 84, 0xFFB8B0A0);
            }
            graphics.drawString(font, Component.translatable(menu.getPrinter().getStatusKey()), 8, 120, 0xFFF4EAD0, false);
        }
        graphics.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, 0xFF202020, false);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }
}
