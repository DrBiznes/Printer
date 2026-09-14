package me.jamino.printer.registry;

import me.jamino.printer.Printer;
import me.jamino.printer.inventory.PrinterMenu;
import me.jamino.printer.inventory.PhotobookMenu;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModMenus {
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, Printer.MODID);
    public static final DeferredHolder<MenuType<?>, MenuType<PrinterMenu>> PRINTER =
            MENUS.register("printer", () -> IMenuTypeExtension.create(PrinterMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<PhotobookMenu>> PHOTOBOOK =
            MENUS.register("photobook", () -> IMenuTypeExtension.create(PhotobookMenu::new));

    private ModMenus() {}

    public static void register(IEventBus bus) {
        MENUS.register(bus);
    }
}
