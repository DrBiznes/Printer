package me.jamino.printer.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import java.util.List;

public final class BlackCartridgeItem extends Item {
    public BlackCartridgeItem(Properties properties) { super(properties); }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        PrinterTooltips.appendBlackCartridge(tooltip, stack.getMaxDamage() - stack.getDamageValue(),
                PrinterTooltips.isExpanded());
    }
}
