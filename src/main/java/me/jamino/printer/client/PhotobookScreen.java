package me.jamino.printer.client;

import com.mojang.blaze3d.systems.RenderSystem;
import me.jamino.printer.data.ImageReference;
import me.jamino.printer.inventory.PhotobookMenu;
import me.jamino.printer.registry.ModDataComponents;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import java.util.ArrayList;
import java.util.List;

/** Page selection is local; the visible images always come from server-synchronized slots. */
public final class PhotobookScreen extends AbstractContainerScreen<PhotobookMenu> {
    private static final net.minecraft.resources.ResourceLocation GHOST_IMAGE =
            me.jamino.printer.Printer.id("textures/gui/ghost_output.png");
    private int spread;
    private Button previous;
    private Button next;
    private List<ImageReference> images = List.of();

    public PhotobookScreen(PhotobookMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 256;
        imageHeight = 238;
        inventoryLabelX = 48;
        inventoryLabelY = 145;
    }

    @Override
    protected void init() {
        super.init();
        previous = addRenderableWidget(Button.builder(Component.literal("<"), button -> turn(-1))
                .bounds(leftPos + 18, topPos + 110, 20, 14).build());
        previous.setTooltip(Tooltip.create(tr("previous")));
        next = addRenderableWidget(Button.builder(Component.literal(">"), button -> turn(1))
                .bounds(leftPos + 218, topPos + 110, 20, 14).build());
        next.setTooltip(Tooltip.create(tr("next")));
        refresh();
    }

    private void refresh() {
        List<ImageReference> current = new ArrayList<>();
        for (int i = 0; i < PhotobookMenu.CAPACITY; i++) {
            ImageReference reference = menu.slots.get(i).getItem().get(ModDataComponents.IMAGE_REFERENCE.get());
            if (reference != null) current.add(reference);
        }
        images = current;
        spread = Math.clamp(spread, 0, Math.max(0, (images.size() - 1) / 2));
        previous.active = spread > 0;
        next.active = (spread + 1) * 2 < images.size();
    }

    private void turn(int direction) {
        refresh();
        if (direction < 0 ? previous.active : next.active) {
            spread += direction;
            net.minecraft.client.Minecraft.getInstance().getSoundManager().play(
                    net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(
                            net.minecraft.sounds.SoundEvents.BOOK_PAGE_TURN, 1.0F));
            refresh();
        }
    }

    @Override
    public boolean keyPressed(int key, int scan, int modifiers) {
        if (key == 263 || key == 262) { turn(key == 263 ? -1 : 1); return true; }
        return super.keyPressed(key, scan, modifiers);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        refresh();
        int x = leftPos, y = topPos;
        graphics.fill(x, y, x + 256, y + 238, 0xFF283331);
        graphics.fill(x + 2, y + 2, x + 254, y + 236, 0xFFC6C3A0);
        graphics.fill(x + 3, y + 3, x + 253, y + 15, 0xFF596052);
        // Cloth cover, page edges, warm paper and a shaded center fold.
        graphics.fill(x + 10, y + 18, x + 246, y + 99, 0xFF365E59);
        graphics.fill(x + 13, y + 19, x + 243, y + 96, 0xFF9C9C80);
        graphics.fill(x + 15, y + 19, x + 126, y + 94, 0xFFFFF0D0);
        graphics.fill(x + 130, y + 19, x + 241, y + 94, 0xFFFFF0D0);
        graphics.fill(x + 124, y + 20, x + 127, y + 94, 0xFFE8DFBA);
        graphics.fill(x + 127, y + 19, x + 129, y + 96, 0xFF776047);
        graphics.fill(x + 129, y + 20, x + 132, y + 94, 0xFFE8DFBA);
        if (images.isEmpty()) {
            graphics.drawWordWrap(font, tr("empty"), x + 29, y + 37, 85, 0xFF596052);
            graphics.drawWordWrap(font, tr("insert"), x + 144, y + 37, 85, 0xFF596052);
        } else {
            drawPhoto(graphics, spread * 2, x + 22, y + 24);
            drawPhoto(graphics, spread * 2 + 1, x + 138, y + 24);
        }
        // Photo storage and player inventory share the printer's slot styling. The tray spans
        // x 44..213 and caps 2px above the first slot row, matching textures/gui/printer.png.
        SlotPanel.tray(graphics, x + 44, y + 109, 170, 40);
        SlotPanel.tray(graphics, x + 44, y + 155, 170, 80);
        for (var slot : menu.slots) SlotPanel.slot(graphics, x + slot.x, y + slot.y);
        // Slots now match the printer's, so the shared ghost icon needs no tint to sit right.
        for (int i = 0; i < PhotobookMenu.CAPACITY; i++) {
            var slot = menu.slots.get(i);
            if (!slot.hasItem()) graphics.blit(GHOST_IMAGE, x + slot.x, y + slot.y, 0, 0, 16, 16, 16, 16);
        }
    }

    private void drawPhoto(GuiGraphics graphics, int index, int x, int y) {
        if (index >= images.size()) return;
        ImageReference reference = images.get(index);
        var texture = ClientImageCache.getOrRequest(reference.contentId());
        float scale = Math.min(96.0F / reference.pixelWidth(), 49.0F / reference.pixelHeight());
        int width = Math.max(1, Math.round(reference.pixelWidth() * scale));
        int height = Math.max(1, Math.round(reference.pixelHeight() * scale));
        int px = x + (96 - width) / 2, py = y + (49 - height) / 2;
        graphics.fill(px - 1, py - 1, px + width + 1, py + height + 1, 0xFF9C9C80);
        graphics.fill(px, py, px + width, py + height, 0xFF000000 | reference.backgroundColor());
        if (texture != null) {
            RenderSystem.enableBlend();
            try {
                graphics.blit(texture, px, py, 0, 0, width, height, width, height);
            } finally { RenderSystem.disableBlend(); }
        } else graphics.drawCenteredString(font, tr("loading"), x + 48, y + 20, 0xFF596052);
        Component caption = reference.title().isBlank() ? Component.translatable("item.printer.image.untitled")
                : Component.literal(reference.title());
        graphics.drawString(font, net.minecraft.locale.Language.getInstance().getVisualOrder(font.substrByWidth(caption, 96)), x, y + 52, 0xFF596052, false);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        Component page = images.isEmpty() ? tr("page", 0, 0) : tr("page", spread + 1, (images.size() + 1) / 2);
        int pageX = imageWidth - 14 - font.width(page);
        graphics.drawString(font, net.minecraft.locale.Language.getInstance().getVisualOrder(
                font.substrByWidth(title, Math.max(0, pageX - 22))), 14, 5, 0xFFFFF0D0, false);
        graphics.drawString(font, page, pageX, 5, 0xFFFFF0D0, false);
        graphics.drawString(font, tr("photos", images.size(), PhotobookMenu.CAPACITY), 48, 99, 0xFF283331, false);
        graphics.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, 0xFF283331, false);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
        for (int side = 0; side < 2; side++) {
            int index = spread * 2 + side;
            if (index < images.size() && isHovering(22 + side * 116, 24, 96, 60, mouseX, mouseY)) {
                var reference = images.get(index);
                graphics.renderTooltip(font, reference.title().isBlank()
                        ? Component.translatable("item.printer.image.untitled") : Component.literal(reference.title()), mouseX, mouseY);
            }
        }
    }

    private static Component tr(String key, Object... args) {
        return Component.translatable("gui.printer.photobook." + key, args);
    }
}
