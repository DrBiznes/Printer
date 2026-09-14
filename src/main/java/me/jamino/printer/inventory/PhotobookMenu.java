package me.jamino.printer.inventory;

import me.jamino.printer.registry.ModDataComponents;
import me.jamino.printer.registry.ModItems;
import me.jamino.printer.registry.ModMenus;
import net.minecraft.core.NonNullList;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;

/** The server persists a copied snapshot after every edit, including edits before a disconnect. */
public final class PhotobookMenu extends AbstractContainerMenu {
    public static final int CAPACITY = 18;
    private final Inventory inventory;
    private final int bookSlot;
    private final ItemStack book;
    private final SimpleContainer photos = new SimpleContainer(CAPACITY);

    public PhotobookMenu(int id, Inventory inventory, FriendlyByteBuf buffer) {
        this(id, inventory, buffer.readVarInt());
    }

    public PhotobookMenu(int id, Inventory inventory, int bookSlot) {
        super(ModMenus.PHOTOBOOK.get(), id);
        this.inventory = inventory;
        this.bookSlot = bookSlot;
        this.book = validBookSlot(bookSlot) ? inventory.getItem(bookSlot) : ItemStack.EMPTY;
        if (!inventory.player.level().isClientSide()) {
            NonNullList<ItemStack> saved = NonNullList.withSize(CAPACITY, ItemStack.EMPTY);
            book.getOrDefault(ModDataComponents.PHOTOBOOK_CONTENTS.get(), ItemContainerContents.EMPTY).copyInto(saved);
            for (int i = 0; i < CAPACITY; i++) photos.setItem(i, saved.get(i));
            photos.addListener(container -> save());
        }
        for (int i = 0; i < CAPACITY; i++) {
            addSlot(new Slot(photos, i, 48 + i % 9 * 18, 111 + i / 9 * 18) {
                @Override public boolean mayPlace(ItemStack stack) { return acceptsPhoto(stack); }
                @Override public int getMaxStackSize() { return 1; }
            });
        }
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) addInventorySlot(col + row * 9 + 9, 48 + col * 18, 157 + row * 18);
        }
        for (int col = 0; col < 9; col++) addInventorySlot(col, 48 + col * 18, 215);
    }

    private static boolean validBookSlot(int slot) { return slot >= 0 && slot < 9 || slot == 40; }

    public static boolean acceptsPhoto(ItemStack stack) {
        return stack.is(ModItems.IMAGE.get()) && stack.has(ModDataComponents.IMAGE_REFERENCE.get());
    }

    private void addInventorySlot(int index, int x, int y) {
        addSlot(new Slot(inventory, index, x, y) {
            @Override public boolean mayPickup(Player player) { return index != bookSlot; }
            @Override public boolean mayPlace(ItemStack stack) { return index != bookSlot; }
        });
    }

    private void save() {
        // Save to the original object even if death or another mod has just moved it.
        if (!book.is(ModItems.PHOTOBOOK.get()) || inventory.player.level().isClientSide()) return;
        NonNullList<ItemStack> snapshot = NonNullList.withSize(CAPACITY, ItemStack.EMPTY);
        for (int i = 0; i < CAPACITY; i++) snapshot.set(i, photos.getItem(i));
        book.set(ModDataComponents.PHOTOBOOK_CONTENTS.get(), ItemContainerContents.fromItems(snapshot));
        inventory.setChanged();
    }

    @Override
    public boolean stillValid(Player player) {
        return player == inventory.player && player.isAlive() && !player.isSpectator() && validBookSlot(bookSlot)
                && book.is(ModItems.PHOTOBOOK.get()) && inventory.getItem(bookSlot) == book;
    }

    @Override
    public void clicked(int slot, int button, ClickType type, Player player) {
        if (!player.level().isClientSide() && !stillValid(player)) return;
        // SWAP accesses the hotbar/offhand directly, bypassing that slot's mayPickup check.
        if (type == ClickType.SWAP && button == bookSlot) return;
        if (slot >= 0 && slot < slots.size() && slots.get(slot).container == inventory
                && slots.get(slot).getContainerSlot() == bookSlot) return;
        super.clicked(slot, button, type, player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        if (index < 0 || index >= slots.size()
                || (!player.level().isClientSide() && !stillValid(player))) return ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (!slot.hasItem() || !slot.mayPickup(player)) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack copy = stack.copy();
        if (index < CAPACITY) {
            if (!moveItemStackTo(stack, CAPACITY, slots.size(), true)) return ItemStack.EMPTY;
        } else if (!acceptsPhoto(stack) || !moveItemStackTo(stack, 0, CAPACITY, false)) return ItemStack.EMPTY;
        if (stack.isEmpty()) slot.set(ItemStack.EMPTY); else slot.setChanged();
        slot.onTake(player, stack);
        return copy;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        save();
    }
}
