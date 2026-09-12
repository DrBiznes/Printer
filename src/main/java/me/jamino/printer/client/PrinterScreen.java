package me.jamino.printer.client;

import com.mojang.blaze3d.systems.RenderSystem;
import me.jamino.printer.Config;
import me.jamino.printer.Printer;
import me.jamino.printer.data.PrinterPreset;
import me.jamino.printer.data.ImageReference;
import me.jamino.printer.inventory.PrinterMenu;
import me.jamino.printer.network.ModNetworking;
import net.minecraft.Util;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.DyeColor;

import java.util.ArrayList;
import java.util.List;

public final class PrinterScreen extends AbstractContainerScreen<PrinterMenu> {
    private static final ResourceLocation PANEL = Printer.id("textures/gui/printer.png");
    private static final int CREAM = 0xFFF2E8CB;
    private EditBox url;
    private EditBox titleBox;
    private PrinterButton smaller, larger, load, print, browse;
    private final List<PrinterColorButton> colors = new ArrayList<>();
    private final ClientFileUpload upload = new ClientFileUpload(this);

    public PrinterScreen(PrinterMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 256;
        imageHeight = 238;
        inventoryLabelX = 48;
        inventoryLabelY = 143;
    }

    @Override
    protected void init() {
        String previousUrl = url == null ? "" : url.getValue();
        String previousTitle = titleBox == null ? (preset() == null ? "" : preset().title()) : titleBox.getValue();
        super.init();
        url = field(14, 32, 156, "url", 2048, 0xFFBDE0CD);
        url.setValue(previousUrl);
        url.setTooltip(Tooltip.create(tr("formats")));
        titleBox = field(14, 55, 62, "title", 64, 0xFFF1D699);
        titleBox.setValue(previousTitle);
        load = key(80, 51, 44, 18, tr("load"), 0xFF4E807B,
                button -> { upload.clearStatus(); ModNetworking.sendLoad(menu.getPos(), url.getValue().trim(), titleBox.getValue()); });
        browse = key(126, 51, 48, 18, tr("browse"), 0xFF4E807B,
                button -> { if (upload.busy()) upload.cancel(); else upload.select(titleBox.getValue()); });
        browse.setTooltip(Tooltip.create(tr("browse_help")));
        smaller = key(154, 91, 18, 18, tr("decrease"), 0xFF736C59,
                button -> ModNetworking.sendResize(menu.getPos(), -1));
        larger = key(228, 91, 18, 18, tr("increase"), 0xFF736C59,
                button -> ModNetworking.sendResize(menu.getPos(), 1));
        smaller.setTooltip(Tooltip.create(tr("smaller")));
        larger.setTooltip(Tooltip.create(tr("larger")));
        print = key(100, 114, 44, 18, tr("print"), 0xFFAF653F,
                button -> { upload.clearStatus(); ModNetworking.sendPrint(menu.getPos()); });
        colors.clear();
        DyeColor[] palette = {DyeColor.WHITE, DyeColor.LIGHT_GRAY, DyeColor.GRAY, DyeColor.BLACK,
                DyeColor.BROWN, DyeColor.RED, DyeColor.ORANGE, DyeColor.YELLOW,
                DyeColor.LIME, DyeColor.GREEN, DyeColor.CYAN, DyeColor.LIGHT_BLUE,
                DyeColor.BLUE, DyeColor.PURPLE, DyeColor.MAGENTA, DyeColor.PINK};
        for (int i = 0; i < palette.length; i++) {
            DyeColor dye = palette[i];
            int color = dye == DyeColor.WHITE ? ImageReference.DEFAULT_BACKGROUND_COLOR : dye.getFireworkColor();
            Component label = Component.translatable("gui.printer.background_option",
                    Component.translatable("color.minecraft." + dye.getName()));
            var swatch = addRenderableWidget(new PrinterColorButton(leftPos + 159 + i % 8 * 11,
                    topPos + 114 + i / 8 * 9, color, label,
                    () -> preset() != null && preset().backgroundColor() == color,
                    button -> ModNetworking.sendBackground(menu.getPos(), color)));
            swatch.setTooltip(Tooltip.create(label));
            colors.add(swatch);
        }
        updateControls();
    }

    private EditBox field(int x, int y, int width, String label, int limit, int color) {
        EditBox box = addRenderableWidget(new EditBox(font, leftPos + x, topPos + y, width, 12, tr(label)));
        box.setBordered(false);
        box.setTextColor(color);
        box.setTextColorUneditable(0xFF92998B);
        box.setHint(tr(label));
        box.setMaxLength(limit);
        return box;
    }

    private PrinterButton key(int x, int y, int width, int height, Component label, int color,
                              net.minecraft.client.gui.components.Button.OnPress action) {
        return addRenderableWidget(new PrinterButton(leftPos + x, topPos + y, width, height, label, color, action));
    }

    private static Component tr(String key) { return Component.translatable("gui.printer." + key); }
    private PrinterPreset preset() {
        return menu.getPrinter() == null ? null : menu.getPrinter().getPreset().orElse(null);
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        upload.tick();
        updateControls();
    }

    public void acceptUpload(ModNetworking.UploadReplyPayload payload) { upload.accept(payload); updateControls(); }

    @Override public void removed() { upload.cancel(); super.removed(); }

    private void updateControls() {
        PrinterPreset preset = preset();
        boolean busy = upload.busy() || menu.getPrinter() == null || menu.getPrinter().isPrinting();
        smaller.active = !busy && preset != null && Math.max(preset.blocksWide(), preset.blocksHigh()) > 1;
        larger.active = !busy && preset != null
                && Math.max(preset.blocksWide(), preset.blocksHigh()) < Config.SERVER.maxPlacementBlocks.get();
        colors.forEach(swatch -> swatch.active = !busy && preset != null);
        browse.setMessage(tr(upload.busy() ? "cancel" : "browse"));
        browse.active = upload.busy() || (!busy && Config.SERVER.allowLocalUploads.get());
        browse.setTooltip(Tooltip.create(Config.SERVER.allowLocalUploads.get() ? tr("browse_help")
                : Component.translatable("message.printer.error.upload_disabled")));
        load.active = !busy && !url.getValue().isBlank();
        print.active = !busy && menu.getPrinter().hasPrintingSupplies();
        print.setTooltip(Tooltip.create(tr(preset == null ? "empty" : print.active ? "print_help" : "supplies_help")));
        url.setEditable(!busy);
        titleBox.setEditable(!busy);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(PANEL, leftPos, topPos, 0, 0, imageWidth, imageHeight, imageWidth, imageHeight);
        if (url.isFocused()) graphics.renderOutline(leftPos + 10, topPos + 27, 164, 20, 0xFF83B8A6);
        if (titleBox.isFocused()) graphics.renderOutline(leftPos + 10, topPos + 50, 68, 20, 0xFFD3AD68);
        String[] ghosts = {"paper", "ink", "output"};
        for (int i = 0; i < 3 && menu.getPrinter() != null; i++) {
            var slot = menu.slots.get(i);
            if (!slot.hasItem()) {
                graphics.blit(Printer.id("textures/gui/ghost_" + ghosts[i] + ".png"),
                        leftPos + slot.x, topPos + slot.y, 0, 0, 16, 16, 16, 16);
            }
            if (isHovering(slot.x, slot.y, 16, 16, mouseX, mouseY)) {
                graphics.renderOutline(leftPos + slot.x - 1, topPos + slot.y - 1, 18, 18, 0xFFF1CF7F);
            }
        }
        renderPreview(graphics);
        boolean busy = menu.getPrinter() != null && menu.getPrinter().isPrinting();
        int light = busy ? 0xFFE1AE59 : preset() != null ? 0xFF9DC69A : 0xFF687A6C;
        graphics.fill(leftPos + 12, topPos + 9, leftPos + 15, topPos + 12, light);
        if (busy) {
            int phase = (int) (Util.getMillis() / 120 % 8);
            graphics.fill(leftPos + 72 + phase * 4, topPos + 98, leftPos + 75 + phase * 4, topPos + 100, light);
        }
    }

    private void renderPreview(GuiGraphics graphics) {
        PrinterPreset preset = preset();
        if (preset == null) {
            graphics.blit(Printer.id("textures/item/image.png"), leftPos + 208, topPos + 44, 0, 0, 16, 16, 16, 16);
            return;
        }
        ResourceLocation texture = ClientImageCache.getOrRequest(preset.sourceId());
        if (texture == null) {
            graphics.drawCenteredString(font, tr("preview_loading"), leftPos + 216, topPos + 47, CREAM);
            return;
        }
        float scale = Math.min(48F / preset.sourceWidth(), 44F / preset.sourceHeight());
        int width = Math.max(1, Math.round(preset.sourceWidth() * scale));
        int height = Math.max(1, Math.round(preset.sourceHeight() * scale));
        int x = leftPos + 192 + (48 - width) / 2;
        int y = topPos + 30 + (44 - height) / 2;
        graphics.fill(x - 1, y - 1, x + width + 1, y + height + 1, CREAM);
        graphics.fill(x, y, x + width, y + height, 0xFF000000 | preset.backgroundColor());
        // Ordinary GUI texture blits do not enable alpha blending. Draw the
        // background first, then composite the canonical RGBA source over it.
        graphics.flush();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        try {
            graphics.blit(texture, x, y, 0, 0, width, height, width, height);
        } finally {
            RenderSystem.disableBlend();
        }
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, 20, 7, CREAM, false);
        graphics.drawString(font, tr("model"), 171, 7, 0xFFB7B7A0, false);
        graphics.drawString(font, tr("source"), 12, 18, 0xFF424C43, false);
        centeredLabel(graphics, tr("preview"), 216, 18, 0xFF424C43);
        graphics.drawString(font, tr("paper_short"), 12, 79, 0xFF424C43, false);
        graphics.drawString(font, tr("ink_short"), 48, 79, 0xFF424C43, false);
        graphics.drawString(font, tr("output_short"), 104, 79, 0xFF424C43, false);
        centeredLabel(graphics, tr("size"), 200, 79, 0xFF424C43);
        PrinterPreset preset = preset();
        graphics.drawCenteredString(font, preset == null ? tr("no_size")
                : Component.translatable("gui.printer.block_dimensions", preset.blocksWide(), preset.blocksHigh()), 200, 96, CREAM);
        graphics.drawString(font, preset == null ? tr("paper_wait")
                : Component.translatable("gui.printer.paper_cost", preset.requiredPaper()), 12, 118, 0xFF424C43, false);
        graphics.drawString(font, tr("background_short"), 146, 119, 0xFF424C43, false);
        String status = menu.getPrinter() == null ? "gui.printer.empty" : menu.getPrinter().getStatusKey();
        graphics.drawString(font, font.plainSubstrByWidth((upload.status() == null ? Component.translatable(status) : upload.status()).getString(), 230),
                12, 134, 0xFFBDE0CD, false);
        graphics.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, 0xFF424C43, false);
    }

    private void centeredLabel(GuiGraphics graphics, Component label, int x, int y, int color) {
        graphics.drawString(font, label, x - font.width(label) / 2, y, color, false);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode != 256 && (url.isFocused() || titleBox.isFocused())) {
            if (keyCode == 257 || keyCode == 335) {
                if (load.active) load.onPress();
                return true;
            }
            // Dispatch to the field directly: the container's inventory shortcut
            // otherwise closes the screen when typing an E in a URL.
            if (keyCode != 258) {
                (url.isFocused() ? url : titleBox).keyPressed(keyCode, scanCode, modifiers);
                return true;
            }
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
        String[] tips = {"paper", "ink", "output"};
        for (int i = 0; i < 3 && menu.getPrinter() != null; i++) {
            var slot = menu.slots.get(i);
            if (!slot.hasItem() && isHovering(slot.x, slot.y, 16, 16, mouseX, mouseY)) {
                graphics.renderTooltip(font, tr(tips[i]), mouseX, mouseY);
            }
        }
        if (isHovering(10, 132, 236, 12, mouseX, mouseY)) {
            Component status = upload.status() != null ? upload.status() : Component.translatable(
                    menu.getPrinter() == null ? "gui.printer.empty" : menu.getPrinter().getStatusKey());
            graphics.renderTooltip(font, status, mouseX, mouseY);
        }
        if (preset() != null && isHovering(188, 27, 56, 50, mouseX, mouseY)) {
            PrinterPreset preset = preset();
            graphics.renderTooltip(font, Component.translatable("gui.printer.preview_details",
                    preset.title().isBlank() ? Component.translatable("item.printer.image.untitled") : Component.literal(preset.title()),
                    preset.sourceWidth(), preset.sourceHeight()), mouseX, mouseY);
        }
    }
}
