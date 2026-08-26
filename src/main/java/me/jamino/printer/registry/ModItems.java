package me.jamino.printer.registry;

import me.jamino.printer.Printer;
import me.jamino.printer.item.ColorCartridgeItem;
import me.jamino.printer.item.ImageItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Printer.MODID);
    public static final DeferredItem<ImageItem> IMAGE = ITEMS.registerItem("image", ImageItem::new,
            new Item.Properties().stacksTo(1));
    public static final DeferredItem<ColorCartridgeItem> COLOR_CARTRIDGE = ITEMS.registerItem(
            "color_cartridge", ColorCartridgeItem::new, new Item.Properties().durability(3));
    public static final DeferredItem<BlockItem> PRINTER = ITEMS.registerSimpleBlockItem(ModBlocks.PRINTER);

    private ModItems() {}

    public static void register(IEventBus bus) {
        ITEMS.register(bus);
    }
}
