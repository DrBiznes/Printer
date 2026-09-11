package me.jamino.printer.item;

import me.jamino.printer.data.ImageReference;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
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
        tooltip.add(line("item.printer.printer.summary"));
        if (expanded) {
            tooltip.add(line("item.printer.printer.workflow"));
            tooltip.add(line("item.printer.printer.paper"));
            tooltip.add(line("item.printer.printer.ink"));
            tooltip.add(line("item.printer.printer.output"));
            tooltip.add(line("item.printer.printer.hoppers"));
            tooltip.add(line("item.printer.printer.extraction"));
            tooltip.add(line("item.printer.printer.redstone"));
            tooltip.add(line("item.printer.printer.busy"));
        } else {
            appendHint(tooltip);
        }
    }

    public static void appendCartridge(List<Component> tooltip, int charges, boolean expanded) {
        tooltip.add(line("item.printer.color_cartridge.charges", Math.max(0, charges)));
        tooltip.add(line("item.printer.color_cartridge.summary"));
        if (expanded) {
            tooltip.add(line("item.printer.color_cartridge.insert"));
            tooltip.add(line("item.printer.color_cartridge.usage"));
        } else {
            appendHint(tooltip);
        }
    }

    public static void appendImage(List<Component> tooltip, @Nullable ImageReference reference, boolean expanded) {
        if (reference == null) {
            tooltip.add(line("item.printer.image.unprinted"));
            if (expanded) {
                tooltip.add(line("item.printer.image.unprinted_help"));
            } else {
                appendHint(tooltip);
            }
            return;
        }
        Component title = reference.title().isBlank()
                ? Component.translatable("item.printer.image.untitled") : Component.literal(reference.title());
        tooltip.add(Component.translatable("item.printer.image.title", title).withStyle(ChatFormatting.GOLD));
        tooltip.add(line("item.printer.image.place"));
        if (expanded) {
            tooltip.add(line("item.printer.image.dimensions", reference.pixelWidth(), reference.pixelHeight()));
            tooltip.add(line("item.printer.image.blocks", reference.blocksWide(), reference.blocksHigh()));
            tooltip.add(line("item.printer.image.frame",
                    Component.translatable("gui.printer.frame." + reference.frame().getSerializedName())));
            tooltip.add(line("item.printer.image.mode." + reference.mode().getSerializedName()));
            tooltip.add(line("item.printer.image.placement_help"));
        } else {
            appendHint(tooltip);
        }
    }

    private static Component line(String key, Object... arguments) {
        return Component.translatable(key, arguments).withStyle(ChatFormatting.GRAY);
    }

    private static void appendHint(List<Component> tooltip) {
        tooltip.add(Component.translatable("tooltip.printer.hold_shift",
                Component.translatable("key.keyboard.left.shift").withStyle(ChatFormatting.YELLOW))
                .withStyle(ChatFormatting.DARK_GRAY));
    }
}
