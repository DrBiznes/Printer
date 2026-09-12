package me.jamino.printer.item;

import me.jamino.printer.data.ImageReference;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;
import java.util.Locale;
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
            condition(tooltip, "printer", 2);
            behaviour(tooltip, "printer", 3);
            behaviour(tooltip, "printer", 4);
            condition(tooltip, "printer", 3);
            behaviour(tooltip, "printer", 5);
            behaviour(tooltip, "printer", 6);
        } else {
            appendHint(tooltip);
        }
    }

    public static void appendCartridge(List<Component> tooltip, int charges, boolean expanded) {
        tooltip.add(line("item.printer.color_cartridge.charges", Math.max(0, charges)));
        tooltip.add(line("item.printer.color_cartridge.tooltip.summary"));
        if (expanded) {
            condition(tooltip, "color_cartridge", 1);
            behaviour(tooltip, "color_cartridge", 1);
            behaviour(tooltip, "color_cartridge", 2);
        } else {
            appendHint(tooltip);
        }
    }

    public static void appendImage(List<Component> tooltip, @Nullable ImageReference reference, boolean expanded) {
        tooltip.add(line("item.printer.image.tooltip.summary"));
        if (reference == null) {
            tooltip.add(line("item.printer.image.unprinted"));
            if (expanded) {
                condition(tooltip, "image", 1);
                behaviour(tooltip, "image", 1);
            } else {
                appendHint(tooltip);
            }
            return;
        }
        Component title = reference.title().isBlank()
                ? Component.translatable("item.printer.image.untitled") : Component.literal(reference.title());
        tooltip.add(Component.translatable("item.printer.image.title", title).withStyle(ChatFormatting.GOLD));
        if (expanded) {
            condition(tooltip, "image", 2);
            behaviour(tooltip, "image", 2, reference.sourceWidth(), reference.sourceHeight());
            behaviour(tooltip, "image", 3, reference.pixelWidth(), reference.pixelHeight());
            behaviour(tooltip, "image", 4, reference.blocksWide(), reference.blocksHigh());
            behaviour(tooltip, "image", 5, String.format(Locale.ROOT, "#%06X", reference.backgroundColor()));
            behaviour(tooltip, "image", 6, Component.translatable("gui.printer.mode." + reference.mode().getSerializedName()));
            condition(tooltip, "image", 3);
            behaviour(tooltip, "image", 7);
            behaviour(tooltip, "image", 8);
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
}
