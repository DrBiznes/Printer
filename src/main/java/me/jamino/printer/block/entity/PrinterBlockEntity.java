package me.jamino.printer.block.entity;

import me.jamino.printer.block.PrinterBlock;
import me.jamino.printer.data.PrinterPreset;
import me.jamino.printer.data.PrintFrame;
import me.jamino.printer.inventory.PrinterMenu;
import me.jamino.printer.job.PrinterJobService;
import me.jamino.printer.registry.ModBlockEntities;
import me.jamino.printer.registry.ModDataComponents;
import me.jamino.printer.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandler;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public final class PrinterBlockEntity extends BlockEntity implements WorldlyContainer, net.minecraft.world.MenuProvider {
    public static final int PAPER_SLOT = 0;
    public static final int INK_SLOT = 1;
    public static final int OUTPUT_SLOT = 2;
    private static final int[] PAPER_SLOTS = {PAPER_SLOT};
    private static final int[] INK_SLOTS = {INK_SLOT};
    private static final int[] OUTPUT_SLOTS = {OUTPUT_SLOT};
    private static final int[] NO_SLOTS = {};

    private final NonNullList<ItemStack> items = NonNullList.withSize(3, ItemStack.EMPTY);
    private final IItemHandler paperHandler = new SidedSlotHandler(PAPER_SLOT, true, false);
    private final IItemHandler inkHandler = new SidedSlotHandler(INK_SLOT, true, false);
    private final IItemHandler outputHandler = new SidedSlotHandler(OUTPUT_SLOT, false, true);
    private final IItemHandler emptyHandler = new SidedSlotHandler(-1, false, false);

    private PrinterPreset preset;
    private boolean printing;
    private long jobGeneration;
    private java.util.UUID owner;
    private boolean automatedPaper, automatedInk, pendingAutomationAward;

    public void setOwner(java.util.UUID owner) { this.owner = owner; setChanged(); }
    public java.util.UUID getOwner() { return owner; }
    public boolean hasAutomatedSupplies() { return automatedPaper || automatedInk; }
    public void completeAutomatedPrint() { pendingAutomationAward = true; setChanged(); }

    public long beginJob(String status) {
        jobGeneration++;
        setJobState(true, status);
        return jobGeneration;
    }
    public boolean isCurrentJob(long generation) { return printing && !isRemoved() && jobGeneration == generation; }
    public void cancelJob(String status) { jobGeneration++; setJobState(false, status); }

    @Override public void setRemoved() {
        jobGeneration++;
        printing = false;
        super.setRemoved();
    }

    private boolean powered;
    private int progress;
    private String statusKey = "gui.printer.status.idle";

    public PrinterBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.PRINTER.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, PrinterBlockEntity printer) {
        if (printer.pendingAutomationAward && printer.owner != null && level instanceof ServerLevel serverLevel) {
            var player = serverLevel.getServer().getPlayerList().getPlayer(printer.owner);
            if (player != null) {
                me.jamino.printer.registry.ModCriteria.ACTION.get().trigger(player, "automate");
                printer.pendingAutomationAward = false;
                printer.setChanged();
            }
        }
        if (printer.printing) {
            printer.progress++;
            if ((printer.progress & 7) == 0) {
                printer.setChanged();
            }
        }
    }

    public IItemHandler getAutomationHandler(@Nullable Direction side) {
        if (side == null) return emptyHandler;
        Direction facing = getBlockState().getValue(PrinterBlock.FACING);
        if (side == Direction.UP) return inkHandler;
        if (side == facing.getCounterClockWise()) return paperHandler;
        if (side == facing.getClockWise() || side == Direction.DOWN) return outputHandler;
        return emptyHandler;
    }

    public void updateRedstone(boolean hasPower) {
        boolean risingEdge = hasPower && !powered;
        powered = hasPower;
        setChanged();
        if (risingEdge && level instanceof ServerLevel serverLevel) {
            PrinterJobService.requestPrint(serverLevel, worldPosition, null);
        }
    }

    public Optional<PrinterPreset> getPreset() {
        return Optional.ofNullable(preset);
    }

    public void setPreset(PrinterPreset preset) {
        this.preset = preset;
        this.statusKey = "gui.printer.status.ready";
        setChangedAndSync();
    }

    public void restorePresetFromItem(ItemStack stack) {
        PrinterPreset itemPreset = stack.get(ModDataComponents.PRINTER_PRESET.get());
        if (itemPreset != null) setPreset(itemPreset);
    }

    public void copyPresetToItem(ItemStack stack) {
        if (preset != null) stack.set(ModDataComponents.PRINTER_PRESET.get(), preset);
    }

    public boolean isPrinting() {
        return printing;
    }

    public int getProgress() {
        return progress;
    }

    public String getStatusKey() {
        return statusKey;
    }

    public void setJobState(boolean printing, String statusKey) {
        this.printing = printing;
        this.progress = printing ? 0 : progress;
        this.statusKey = statusKey;
        if (level != null && !level.isClientSide()) {
            BlockState state = getBlockState();
            if (state.hasProperty(PrinterBlock.PRINTING) && state.getValue(PrinterBlock.PRINTING) != printing) {
                level.setBlock(worldPosition, state.setValue(PrinterBlock.PRINTING, printing), 3);
            }
        }
        setChangedAndSync();
    }

    public boolean hasPrintingSupplies() {
        return preset != null && items.get(PAPER_SLOT).is(Items.PAPER)
                && items.get(PAPER_SLOT).getCount() >= preset.requiredPaper()
                && (items.get(INK_SLOT).is(Items.INK_SAC) || (items.get(INK_SLOT).is(ModItems.COLOR_CARTRIDGE.get())
                    && items.get(INK_SLOT).getDamageValue() < items.get(INK_SLOT).getMaxDamage()))
                && items.get(OUTPUT_SLOT).isEmpty();
    }

    public boolean matchesPrint(PrinterPreset expected, me.jamino.printer.data.PrintMode mode) {
        return java.util.Objects.equals(preset, expected) && hasPrintingSupplies()
                && isMonochromeSupply() == (mode == me.jamino.printer.data.PrintMode.MONOCHROME);
    }

    public void consumeSupplies() {
        automatedPaper = false;
        automatedInk = false;
        if (preset == null) return;
        items.get(PAPER_SLOT).shrink(preset.requiredPaper());
        ItemStack ink = items.get(INK_SLOT);
        if (ink.is(Items.INK_SAC)) {
            ink.shrink(1);
        } else if (ink.is(ModItems.COLOR_CARTRIDGE.get())) {
            ink.setDamageValue(ink.getDamageValue() + 1);
            if (ink.getDamageValue() >= ink.getMaxDamage()) items.set(INK_SLOT, ItemStack.EMPTY);
        }
        setChangedAndSync();
    }

    public boolean isMonochromeSupply() {
        return items.get(INK_SLOT).is(Items.INK_SAC);
    }

    public void setOutput(ItemStack stack) {
        items.set(OUTPUT_SLOT, stack);
        setChangedAndSync();
    }

    private void setChangedAndSync() {
        setChanged();
        if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        for (int i = 0; i < items.size(); i++) {
            if (!items.get(i).isEmpty()) tag.put("Item" + i, items.get(i).save(registries));
        }
        if (preset != null) {
            tag.putString("PresetSource", preset.sourceId());
            tag.putString("PresetTitle", preset.title());
            tag.putInt("PresetSourceWidth", preset.sourceWidth());
            tag.putInt("PresetSourceHeight", preset.sourceHeight());
            tag.putInt("PresetBlocksWide", preset.blocksWide());
            tag.putInt("PresetBlocksHigh", preset.blocksHigh());
            tag.putString("PresetFrame", preset.frame().getSerializedName());
            tag.putInt("PresetOriginalWidth", preset.originalWidth());
            tag.putInt("PresetOriginalHeight", preset.originalHeight());
        }
        if (owner != null) tag.putUUID("Owner", owner);
        tag.putBoolean("AutomatedPaper", automatedPaper);
        tag.putBoolean("AutomatedInk", automatedInk);
        tag.putBoolean("PendingAutomationAward", pendingAutomationAward);
        tag.putBoolean("Powered", powered);
        tag.putString("Status", printing ? "gui.printer.status.interrupted" : statusKey);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        for (int i = 0; i < items.size(); i++) {
            items.set(i, tag.contains("Item" + i)
                    ? ItemStack.parse(registries, tag.getCompound("Item" + i)).orElse(ItemStack.EMPTY)
                    : ItemStack.EMPTY);
        }
        if (tag.contains("PresetSource")) {
            preset = new PrinterPreset(tag.getString("PresetSource"), tag.getString("PresetTitle"),
                    tag.getInt("PresetSourceWidth"), tag.getInt("PresetSourceHeight"),
                    tag.getInt("PresetBlocksWide"), tag.getInt("PresetBlocksHigh"),
                    PrintFrame.byName(tag.getString("PresetFrame")),
                    tag.getInt("PresetOriginalWidth"), tag.getInt("PresetOriginalHeight"));
        } else {
            preset = null;
        }
        owner = tag.hasUUID("Owner") ? tag.getUUID("Owner") : null;
        automatedPaper = tag.getBoolean("AutomatedPaper");
        automatedInk = tag.getBoolean("AutomatedInk");
        pendingAutomationAward = tag.getBoolean("PendingAutomationAward");
        powered = tag.getBoolean("Powered");
        jobGeneration++;
        // Only synchronization tags contain these fields. Disk saves resume idle after interruption.
        printing = tag.getBoolean("ActiveJob");
        progress = tag.getInt("Progress");
        statusKey = tag.contains("Status") ? tag.getString("Status") : "gui.printer.status.idle";
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        saveAdditional(tag, registries);
        tag.putBoolean("ActiveJob", printing);
        tag.putInt("Progress", progress);
        tag.putString("Status", statusKey);
        return tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    protected void applyImplicitComponents(DataComponentInput input) {
        PrinterPreset itemPreset = input.get(ModDataComponents.PRINTER_PRESET.get());
        if (itemPreset != null) preset = itemPreset;
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder components) {
        if (preset != null) components.set(ModDataComponents.PRINTER_PRESET.get(), preset);
    }

    @Override
    public void removeComponentsFromTag(CompoundTag tag) {
        tag.remove("PresetSource");
        tag.remove("PresetTitle");
        tag.remove("PresetSourceWidth");
        tag.remove("PresetSourceHeight");
        tag.remove("PresetBlocksWide");
        tag.remove("PresetBlocksHigh");
        tag.remove("PresetFrame");
        tag.remove("PresetOriginalWidth");
        tag.remove("PresetOriginalHeight");
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.printer.printer");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new PrinterMenu(containerId, inventory, worldPosition);
    }

    @Override public int getContainerSize() { return items.size(); }
    @Override public boolean isEmpty() { return items.stream().allMatch(ItemStack::isEmpty); }
    @Override public ItemStack getItem(int slot) { return items.get(slot); }
    private void clearAutomation(int slot) {
        if (slot == PAPER_SLOT) automatedPaper = false;
        if (slot == INK_SLOT) automatedInk = false;
    }
    @Override public ItemStack removeItem(int slot, int amount) {
        clearAutomation(slot);
        ItemStack result = net.minecraft.world.ContainerHelper.removeItem(items, slot, amount);
        if (!result.isEmpty()) setChangedAndSync();
        return result;
    }
    @Override public ItemStack removeItemNoUpdate(int slot) {
        clearAutomation(slot);
        return net.minecraft.world.ContainerHelper.takeItem(items, slot);
    }
    @Override public void setItem(int slot, ItemStack stack) {
        clearAutomation(slot);
        items.set(slot, stack);
        if (stack.getCount() > getMaxStackSize(stack)) stack.setCount(getMaxStackSize(stack));
        setChangedAndSync();
    }
    @Override public boolean stillValid(Player player) {
        return level != null && level.getBlockEntity(worldPosition) == this
                && player.distanceToSqr(worldPosition.getX() + 0.5, worldPosition.getY() + 0.5,
                worldPosition.getZ() + 0.5) <= 64.0;
    }
    @Override public void clearContent() { automatedPaper = false; automatedInk = false; items.clear(); setChangedAndSync(); }
    @Override public boolean canPlaceItem(int slot, ItemStack stack) {
        return !printing && switch (slot) {
            case PAPER_SLOT -> stack.is(Items.PAPER);
            case INK_SLOT -> stack.is(Items.INK_SAC) || stack.is(ModItems.COLOR_CARTRIDGE.get());
            default -> false;
        };
    }
    @Override public int[] getSlotsForFace(Direction side) {
        Direction facing = getBlockState().getValue(PrinterBlock.FACING);
        if (side == Direction.UP) return INK_SLOTS;
        if (side == facing.getCounterClockWise()) return PAPER_SLOTS;
        if (side == facing.getClockWise() || side == Direction.DOWN) return OUTPUT_SLOTS;
        return NO_SLOTS;
    }
    @Override public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
        return canPlaceItem(slot, stack);
    }
    @Override public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return !printing && slot == OUTPUT_SLOT;
    }

    private final class SidedSlotHandler implements IItemHandler {
        private final int slot;
        private final boolean insert;
        private final boolean extract;

        private SidedSlotHandler(int slot, boolean insert, boolean extract) {
            this.slot = slot;
            this.insert = insert;
            this.extract = extract;
        }

        @Override public int getSlots() { return slot < 0 ? 0 : 1; }
        @Override public ItemStack getStackInSlot(int ignored) { return slot < 0 ? ItemStack.EMPTY : getItem(slot); }
        @Override public ItemStack insertItem(int ignored, ItemStack stack, boolean simulate) {
            if (!insert || slot < 0 || stack.isEmpty() || !canPlaceItem(slot, stack)) return stack;
            ItemStack existing = getItem(slot);
            int limit = Math.min(getSlotLimit(0), stack.getMaxStackSize());
            if (!existing.isEmpty() && !ItemStack.isSameItemSameComponents(existing, stack)) return stack;
            int accepted = Math.min(stack.getCount(), limit - existing.getCount());
            if (accepted <= 0) return stack;
            if (!simulate) {
                if (existing.isEmpty()) setItem(slot, stack.copyWithCount(accepted));
                else { existing.grow(accepted); setChangedAndSync(); }
                if (slot == PAPER_SLOT) automatedPaper = true;
                if (slot == INK_SLOT) automatedInk = true;
                setChanged();
            }
            return stack.copyWithCount(stack.getCount() - accepted);
        }
        @Override public ItemStack extractItem(int ignored, int amount, boolean simulate) {
            if (!extract || slot < 0 || amount <= 0 || printing) return ItemStack.EMPTY;
            ItemStack existing = getItem(slot);
            if (existing.isEmpty()) return ItemStack.EMPTY;
            int extracted = Math.min(amount, existing.getCount());
            ItemStack result = existing.copyWithCount(extracted);
            if (!simulate) removeItem(slot, extracted);
            return result;
        }
        @Override public int getSlotLimit(int ignored) { return slot == INK_SLOT ? 64 : slot == OUTPUT_SLOT ? 1 : 64; }
        @Override public boolean isItemValid(int ignored, ItemStack stack) { return insert && slot >= 0 && canPlaceItem(slot, stack); }
    }
}
