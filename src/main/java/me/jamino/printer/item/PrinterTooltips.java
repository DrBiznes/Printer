package me.jamino.printer.item;

import me.jamino.printer.data.ImageReference;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.DyeColor;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;
import java.util.function.BooleanSupplier;

/** Shared tooltip text; keyboard access is installed only by the client. */
public final class PrinterTooltips {
    private static BooleanSupplier shiftDown = () -> false;

    private PrinterTooltips() {}

    public static void setShiftDownSupplier(BooleanSupplier supplier) {
        shiftDown = Objects.requireNonNull(supplier);
    }

    public static boolean isExpanded() {
        return shiftDown.getAsBoolean();
    }

    public static void appendPrinter(List<Component> tooltip, boolean expanded) {
        tooltip.add(line("item.printer.printer.tooltip.summary"));
        if (expanded) {
            condition(tooltip, "printer", 1);
            behaviour(tooltip, "printer", 1);
            behaviour(tooltip, "printer", 2);
            behaviour(tooltip, "printer", 3);
        } else {
            appendHint(tooltip);
        }
    }

    public static void appendCartridge(List<Component> tooltip, int charges, boolean expanded) {
        tooltip.add(line("item.printer.color_cartridge.charges", Math.max(0, charges)));
        tooltip.add(line("item.printer.color_cartridge.tooltip.summary"));
        if (expanded) {
            behaviour(tooltip, "color_cartridge", 1);
        } else {
            appendHint(tooltip);
        }
    }

    public static void appendBlackCartridge(List<Component> tooltip, int charges, boolean expanded) {
        tooltip.add(line("item.printer.black_cartridge.charges", Math.max(0, charges)));
        tooltip.add(line("item.printer.black_cartridge.tooltip.summary"));
        if (expanded) behaviour(tooltip, "black_cartridge", 1); else appendHint(tooltip);
    }

    public static void appendPhotobook(List<Component> tooltip, int count, boolean expanded) {
        tooltip.add(line("item.printer.photobook.tooltip.summary", count, me.jamino.printer.inventory.PhotobookMenu.CAPACITY));
        if (expanded) behaviour(tooltip, "photobook", 1); else appendHint(tooltip);
    }

    public static void appendImage(List<Component> tooltip, @Nullable ImageReference reference, boolean expanded) {
        if (reference == null) {
            tooltip.add(line("item.printer.image.unprinted"));
            if (expanded) {
                behaviour(tooltip, "image", 1);
            } else {
                appendHint(tooltip);
            }
            return;
        }
        Component title = reference.title().isBlank()
                ? Component.translatable("item.printer.image.untitled") : Component.literal(reference.title());
        tooltip.add(Component.translatable("item.printer.image.title", title).withStyle(ChatFormatting.GOLD));
        behaviour(tooltip, "image", 4, reference.blocksWide(), reference.blocksHigh());
        if (expanded) {
            behaviour(tooltip, "image", 5, backgroundName(reference.backgroundColor()));
            behaviour(tooltip, "image", 6, Component.translatable("gui.printer.mode." + reference.mode().getSerializedName()));
            behaviour(tooltip, "image", 7);
        } else {
            appendHint(tooltip);
        }
    }

    private static Component line(String key, Object... arguments) {
        return Component.translatable(key, arguments).withStyle(ChatFormatting.GRAY);
    }

    private static void condition(List<Component> tooltip, String item, int index) {
        tooltip.add(Component.translatable("item.printer." + item + ".tooltip.condition" + index)
                .withStyle(ChatFormatting.GOLD));
    }

    private static void behaviour(List<Component> tooltip, String item, int index, Object... arguments) {
        tooltip.add(line("item.printer." + item + ".tooltip.behaviour" + index, arguments));
    }

    private static void appendHint(List<Component> tooltip) {
        tooltip.add(Component.translatable("tooltip.printer.hold_shift",
                Component.translatable("key.keyboard.left.shift").withStyle(ChatFormatting.YELLOW))
                .withStyle(ChatFormatting.DARK_GRAY));
    }

    private static Component backgroundName(int color) {
        int normalized = color & 0xFFFFFF;
        for (DyeColor dye : DyeColor.values()) {
            int dyeColor = dye == DyeColor.WHITE ? ImageReference.DEFAULT_BACKGROUND_COLOR
                    : dye == DyeColor.BLACK ? 0x000000 : dye.getFireworkColor();
            if (dyeColor == normalized) {
                return Component.translatable("color.minecraft." + dye.getName());
            }
        }
        return Component.translatable("item.printer.image.background.custom");
    }
}
