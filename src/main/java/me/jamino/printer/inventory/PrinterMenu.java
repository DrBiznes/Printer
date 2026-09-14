package me.jamino.printer.inventory;

import me.jamino.printer.block.entity.PrinterBlockEntity;
import me.jamino.printer.registry.ModItems;
import me.jamino.printer.registry.ModMenus;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public final class PrinterMenu extends AbstractContainerMenu {
    private final BlockPos pos;
    private final PrinterBlockEntity printer;

    public PrinterMenu(int id, Inventory inventory, FriendlyByteBuf buffer) {
        this(id, inventory, buffer.readBlockPos());
    }

    public PrinterMenu(int id, Inventory inventory, BlockPos pos) {
        super(ModMenus.PRINTER.get(), id);
        this.pos = pos;
        this.printer = inventory.player.level().getBlockEntity(pos) instanceof PrinterBlockEntity found ? found : null;

        if (printer != null) {
            addSlot(new Slot(printer, PrinterBlockEntity.PAPER_SLOT, 18, 91) {
                @Override public boolean mayPlace(ItemStack stack) { return printer.canPlaceItem(PrinterBlockEntity.PAPER_SLOT, stack); }
                @Override public boolean mayPickup(Player player) { return !printer.isPrinting(); }
            });
            addSlot(new Slot(printer, PrinterBlockEntity.INK_SLOT, 48, 91) {
                @Override public boolean mayPlace(ItemStack stack) {
                    return printer.canPlaceItem(PrinterBlockEntity.INK_SLOT, stack);
                }
                @Override public boolean mayPickup(Player player) { return !printer.isPrinting(); }
            });
            addSlot(new Slot(printer, PrinterBlockEntity.OUTPUT_SLOT, 112, 91) {
                @Override public boolean mayPlace(ItemStack stack) { return false; }
                @Override public boolean mayPickup(Player player) { return !printer.isPrinting(); }
            });
        }

        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new Slot(inventory, column + row * 9 + 9, 48 + column * 18, 155 + row * 18));
            }
        }
        for (int column = 0; column < 9; column++) {
            addSlot(new Slot(inventory, column, 48 + column * 18, 213));
        }
    }

    public BlockPos getPos() { return pos; }
    public PrinterBlockEntity getPrinter() { return printer; }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        if (printer == null || printer.isPrinting() || index < 0 || index >= slots.size()) return ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack copy = stack.copy();
        if (index < 3) {
            if (!moveItemStackTo(stack, 3, slots.size(), true)) return ItemStack.EMPTY;
        } else if (stack.is(Items.PAPER)) {
            if (!moveItemStackTo(stack, 0, 1, false)) return ItemStack.EMPTY;
        } else if (stack.is(ModItems.BLACK_CARTRIDGE.get()) || stack.is(ModItems.COLOR_CARTRIDGE.get())) {
            if (!moveItemStackTo(stack, 1, 2, false)) return ItemStack.EMPTY;
        } else {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) slot.set(ItemStack.EMPTY); else slot.setChanged();
        if (stack.getCount() == copy.getCount()) return ItemStack.EMPTY;
        slot.onTake(player, stack);
        return copy;
    }

    @Override
    public boolean stillValid(Player player) {
        return printer != null && printer.stillValid(player);
    }
}
