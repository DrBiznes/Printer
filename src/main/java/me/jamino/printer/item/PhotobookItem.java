package me.jamino.printer.item;

import me.jamino.printer.inventory.PhotobookMenu;
import me.jamino.printer.registry.ModDataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.Level;
import java.util.List;

public final class PhotobookItem extends Item {
    public PhotobookItem(Properties properties) { super(properties); }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack book = player.getItemInHand(hand);
        if (player instanceof ServerPlayer serverPlayer && !player.isSpectator()) {
            int slot = hand == InteractionHand.MAIN_HAND ? player.getInventory().selected : 40;
            serverPlayer.openMenu(new SimpleMenuProvider((id, inventory, owner) ->
                    new PhotobookMenu(id, inventory, slot), book.getHoverName()), buffer -> buffer.writeVarInt(slot));
        }
        return InteractionResultHolder.sidedSuccess(book, level.isClientSide());
    }

    @Override
    public boolean canFitInsideContainerItems() { return false; }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        int count = (int) stack.getOrDefault(ModDataComponents.PHOTOBOOK_CONTENTS.get(), ItemContainerContents.EMPTY)
                .nonEmptyStream().count();
        PrinterTooltips.appendPhotobook(tooltip, count, PrinterTooltips.isExpanded());
    }
}
