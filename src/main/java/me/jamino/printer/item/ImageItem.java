package me.jamino.printer.item;

import me.jamino.printer.data.ImageReference;
import me.jamino.printer.registry.ModDataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.InteractionResult;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import me.jamino.printer.entity.PrintedImageEntity;
import me.jamino.printer.registry.ModEntities;

import java.util.List;

public class ImageItem extends Item {
    public ImageItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        ItemStack stack = context.getItemInHand();
        ImageReference reference = stack.get(ModDataComponents.IMAGE_REFERENCE.get());
        if (reference == null) return InteractionResult.FAIL;
        Direction direction = context.getClickedFace();
        BlockPos pos = context.getClickedPos().relative(direction);
        Player player = context.getPlayer();
        if (direction.getAxis().isVertical() || player == null || !player.mayUseItemAt(pos, direction, stack)) {
            return InteractionResult.FAIL;
        }
        PrintedImageEntity entity = new PrintedImageEntity(ModEntities.PRINTED_IMAGE.get(), context.getLevel(),
                pos, direction, reference);
        if (!entity.survives()) return InteractionResult.CONSUME;
        if (!context.getLevel().isClientSide()) {
            entity.playPlacementSound();
            context.getLevel().addFreshEntity(entity);
            if (player instanceof net.minecraft.server.level.ServerPlayer serverPlayer)
                me.jamino.printer.registry.ModCriteria.ACTION.get().trigger(serverPlayer, "place");
            context.getLevel().gameEvent(player, net.minecraft.world.level.gameevent.GameEvent.ENTITY_PLACE, entity.position());
        }
        stack.shrink(1);
        return InteractionResult.sidedSuccess(context.getLevel().isClientSide());
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        PrinterTooltips.appendImage(tooltip, stack.get(ModDataComponents.IMAGE_REFERENCE.get()),
                PrinterTooltips.isExpanded());
    }
}
