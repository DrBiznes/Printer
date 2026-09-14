package me.jamino.printer.registry;

import me.jamino.printer.Printer;
import me.jamino.printer.item.ColorCartridgeItem;
import me.jamino.printer.item.BlackCartridgeItem;
import me.jamino.printer.item.PhotobookItem;
import me.jamino.printer.item.ImageItem;
import me.jamino.printer.item.PrinterItem;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModItems {
    public static final int CARTRIDGE_CHARGES = 3;
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Printer.MODID);
    public static final DeferredItem<ImageItem> IMAGE = ITEMS.registerItem("image", ImageItem::new,
            new Item.Properties().stacksTo(1));
    public static final DeferredItem<ColorCartridgeItem> COLOR_CARTRIDGE = ITEMS.registerItem(
            "color_cartridge", ColorCartridgeItem::new, new Item.Properties().durability(CARTRIDGE_CHARGES));
    public static final DeferredItem<BlackCartridgeItem> BLACK_CARTRIDGE = ITEMS.registerItem(
            "black_cartridge", BlackCartridgeItem::new, new Item.Properties().durability(CARTRIDGE_CHARGES));
    public static final DeferredItem<PhotobookItem> PHOTOBOOK = ITEMS.registerItem(
            "photobook", PhotobookItem::new, new Item.Properties().stacksTo(1));
    public static final DeferredItem<PrinterItem> PRINTER = ITEMS.registerItem("printer",
            properties -> new PrinterItem(ModBlocks.PRINTER.get(), properties));

    private ModItems() {}

    public static void register(IEventBus bus) {
        ITEMS.register(bus);
    }
}
