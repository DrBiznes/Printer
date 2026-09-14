package me.jamino.printer.registry;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.registries.VanillaRegistries;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.ItemStack;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class ModCreativeTabsTest {
    @Test
    void registeredTabHasPrinterIconAndDeliberateSearchableOrder() {
        CreativeModeTab tab = ModCreativeTabs.PRINTER.get();
        assertEquals("itemGroup.printer", ((TranslatableContents) tab.getDisplayName().getContents()).getKey());
        assertTrue(tab.getIconItem().is(ModItems.PRINTER.get()));
        assertEquals(ModItems.PRINTER.get(), ModBlocks.PRINTER.get().asItem());
        var parameters = new CreativeModeTab.ItemDisplayParameters(FeatureFlags.DEFAULT_FLAGS,
                false, HolderLookup.Provider.create(Stream.empty()));
        tab.buildContents(parameters);
        var expected = List.of(ModItems.PRINTER.get(), ModItems.BLACK_CARTRIDGE.get(), ModItems.COLOR_CARTRIDGE.get(), ModItems.PHOTOBOOK.get());
        assertEquals(expected, tab.getDisplayItems().stream().map(ItemStack::getItem).toList());
        assertEquals(expected, tab.getSearchTabDisplayItems().stream().map(ItemStack::getItem).toList());
    }

    @Test
    void functionalBlocksDoesNotDuplicatePrinterItems() {
        CreativeModeTab functional = BuiltInRegistries.CREATIVE_MODE_TAB.getOrThrow(CreativeModeTabs.FUNCTIONAL_BLOCKS);
        functional.buildContents(new CreativeModeTab.ItemDisplayParameters(FeatureFlags.DEFAULT_FLAGS,
                false, VanillaRegistries.createLookup()));
        assertTrue(functional.getDisplayItems().stream().noneMatch(stack -> stack.is(ModItems.PRINTER.get())
                || stack.is(ModItems.BLACK_CARTRIDGE.get()) || stack.is(ModItems.PHOTOBOOK.get())
                || stack.is(ModItems.COLOR_CARTRIDGE.get()) || stack.is(ModItems.IMAGE.get())));
    }
}
