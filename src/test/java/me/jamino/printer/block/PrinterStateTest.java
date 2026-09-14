package me.jamino.printer.block;

import me.jamino.printer.block.entity.PrinterBlockEntity;
import me.jamino.printer.data.*;
import me.jamino.printer.registry.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.data.registries.VanillaRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PrinterStateTest {
    private PrinterBlockEntity printer(Direction facing) {
        return new PrinterBlockEntity(BlockPos.ZERO, ModBlocks.PRINTER.get().defaultBlockState().setValue(PrinterBlock.FACING, facing));
    }

    @Test
    void rawInkSacsAreNotPrinterSupplies() {
        var printer = new me.jamino.printer.block.entity.PrinterBlockEntity(
                net.minecraft.core.BlockPos.ZERO, me.jamino.printer.registry.ModBlocks.PRINTER.get().defaultBlockState());
        assertFalse(printer.canPlaceItem(1, new ItemStack(Items.INK_SAC)));
        assertTrue(printer.canPlaceItem(1, new ItemStack(ModItems.BLACK_CARTRIDGE.get())));
    }

    @Test void syncKeepsBusyButDiskLoadInterruptsJobsAndPreservesPresetAndOwner() {
        var printer = printer(Direction.NORTH);
        var preset = new PrinterPreset("a".repeat(64), "Photo", 512, 256, 4, 2, 0x224466, 3000, 1500);
        printer.setPreset(preset); printer.setOwner(java.util.UUID.randomUUID());
        long job = printer.beginJob("gui.printer.status.loading");
        var lookup = VanillaRegistries.createLookup();
        var client = printer(Direction.NORTH);
        client.loadWithComponents(printer.getUpdateTag(lookup), lookup);
        assertTrue(client.isPrinting());
        assertEquals("gui.printer.status.loading", client.getStatusKey());
        var disk = printer(Direction.NORTH);
        disk.loadWithComponents(printer.saveWithoutMetadata(lookup), lookup);
        assertFalse(disk.isPrinting());
        assertEquals("gui.printer.status.interrupted", disk.getStatusKey());
        assertEquals(preset, disk.getPreset().orElseThrow());
        assertEquals(printer.getOwner(), disk.getOwner());
        printer.setRemoved();
        assertFalse(printer.isCurrentJob(job));
    }

    @Test void allFourOrientationsAcceptOnlyCorrectSuppliesAndOutput() {
        for (Direction facing : Direction.Plane.HORIZONTAL) {
            var printer = printer(facing);
            for (Direction side : Direction.values()) {
                var handler = printer.getAutomationHandler(side);
                boolean paper = side == facing.getCounterClockWise(), ink = side == Direction.UP;
                if (handler.getSlots() > 0) {
                    assertEquals(paper, handler.insertItem(0, new ItemStack(Items.PAPER), true).isEmpty());
                    assertEquals(ink, handler.insertItem(0, new ItemStack(ModItems.BLACK_CARTRIDGE.get()), true).isEmpty());
                    assertFalse(handler.insertItem(0, new ItemStack(Items.DIRT), true).isEmpty());
                } else assertTrue(side == facing || side == facing.getOpposite());
            }
            assertEquals(0, printer.getAutomationHandler(null).getSlots());
            assertFalse(printer.hasAutomatedSupplies(), "Simulations must not grant automation credit");
            printer.getAutomationHandler(Direction.UP).insertItem(0, new ItemStack(ModItems.BLACK_CARTRIDGE.get()), false);
            assertTrue(printer.hasAutomatedSupplies());
            printer.removeItem(PrinterBlockEntity.INK_SLOT, 1);
            assertFalse(printer.hasAutomatedSupplies());
        }
    }

    @Test void emptyCartridgesCannotPrintAndModeChangesInvalidateCompletion() {
        var printer = printer(Direction.NORTH);
        var preset = new PrinterPreset("a".repeat(64), "", 32, 16, 1, 1, ImageReference.DEFAULT_BACKGROUND_COLOR, 32, 16);
        printer.setPreset(preset); printer.setItem(0, new ItemStack(Items.PAPER, 2));
        ItemStack cartridge = new ItemStack(ModItems.COLOR_CARTRIDGE.get());
        cartridge.setDamageValue(3); printer.setItem(1, cartridge);
        assertFalse(printer.hasPrintingSupplies());
        printer.setItem(1, new ItemStack(ModItems.COLOR_CARTRIDGE.get()));
        assertTrue(printer.matchesPrint(preset, PrintMode.COLOR));
        printer.setItem(1, new ItemStack(ModItems.BLACK_CARTRIDGE.get()));
        assertFalse(printer.matchesPrint(preset, PrintMode.COLOR));
        assertTrue(printer.matchesPrint(preset, PrintMode.MONOCHROME));
        ItemStack emptyBlackCartridge = new ItemStack(ModItems.BLACK_CARTRIDGE.get());
        emptyBlackCartridge.setDamageValue(ModItems.CARTRIDGE_CHARGES);
        printer.setItem(1, emptyBlackCartridge);
        assertFalse(printer.hasPrintingSupplies());
    }

    @Test void blackCartridgeProvidesThreePrintCharges() {
        var printer = printer(Direction.NORTH);
        var preset = new PrinterPreset("a".repeat(64), "", 32, 16, 1, 1,
                ImageReference.DEFAULT_BACKGROUND_COLOR, 32, 16);
        printer.setPreset(preset);
        printer.setItem(0, new ItemStack(Items.PAPER, 3));
        printer.setItem(1, new ItemStack(ModItems.BLACK_CARTRIDGE.get()));

        assertEquals(ModItems.CARTRIDGE_CHARGES, printer.getItem(1).getMaxDamage());
        for (int charge = 1; charge <= ModItems.CARTRIDGE_CHARGES; charge++) {
            assertTrue(printer.hasPrintingSupplies());
            printer.consumeSupplies();
            if (charge < ModItems.CARTRIDGE_CHARGES) {
                assertEquals(charge, printer.getItem(1).getDamageValue());
            } else {
                assertTrue(printer.getItem(1).isEmpty());
            }
        }
        assertEquals(0, printer.getItem(0).getCount());
    }

    @Test void droppedPrinterComponentRetainsBackgroundAndDoesNotDuplicatePresetNbt() {
        var printer = printer(Direction.NORTH);
        var preset = new PrinterPreset("a".repeat(64), "Color", 32, 16, 1, 1, 0x123456, 32, 16);
        printer.setPreset(preset);
        var stack = new ItemStack(ModItems.PRINTER.get());
        printer.copyPresetToItem(stack);
        var restored = printer(Direction.NORTH);
        restored.restorePresetFromItem(stack);
        assertEquals(preset, restored.getPreset().orElseThrow());
        var lookup = VanillaRegistries.createLookup();
        var nbt = printer.saveWithoutMetadata(lookup);
        assertEquals(0x123456, nbt.getInt("PresetBackgroundColor"));
        assertFalse(nbt.contains("PresetFrame"));
        printer.removeComponentsFromTag(nbt);
        assertFalse(nbt.contains("PresetBackgroundColor"));
        assertFalse(nbt.contains("PresetSource"));
    }

    @Test void coloredPresetCannotPrintWithInkSacButBlackAndWhiteCan() {
        var printer = printer(Direction.NORTH);
        var colored = new PrinterPreset("a".repeat(64), "", 32, 16, 1, 1, 0xB02E26, 32, 16);
        printer.setPreset(colored);
        printer.setItem(0, new ItemStack(Items.PAPER, 3));
        printer.setItem(1, new ItemStack(ModItems.COLOR_CARTRIDGE.get()));
        assertTrue(printer.hasPrintingSupplies());
        assertTrue(printer.canUseBackgroundColor(0xB02E26));
        printer.setItem(1, new ItemStack(ModItems.BLACK_CARTRIDGE.get()));
        assertFalse(printer.hasPrintingSupplies());
        assertFalse(printer.canPrintBackground());
        assertFalse(printer.canUseBackgroundColor(0xB02E26));
        assertFalse(printer.matchesPrint(colored, PrintMode.MONOCHROME));
        assertEquals(colored, printer.getPreset().orElseThrow(), "Ink swap must not silently rewrite the preset");
        for (int background : new int[]{0x000000, 0xFFFFFF}) {
            printer.setPreset(new PrinterPreset("a".repeat(64), "", 32, 16, 1, 1, background, 32, 16));
            assertTrue(printer.canUseBackgroundColor(background));
            assertTrue(printer.canPrintBackground());
            assertTrue(printer.hasPrintingSupplies());
        }
        assertEquals(3, printer.getItem(0).getCount());
        assertEquals(1, printer.getItem(1).getCount());
    }

}
