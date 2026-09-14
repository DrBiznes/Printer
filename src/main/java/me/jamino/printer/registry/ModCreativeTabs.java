package me.jamino.printer.registry;

import me.jamino.printer.Printer;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModCreativeTabs {
    private static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, Printer.MODID);
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> PRINTER = TABS.register("printer",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.printer"))
                    .icon(() -> ModItems.PRINTER.get().getDefaultInstance())
                    .displayItems((parameters, output) -> {
                        output.accept(ModItems.PRINTER.get());
                        output.accept(ModItems.BLACK_CARTRIDGE.get());
                        output.accept(ModItems.COLOR_CARTRIDGE.get());
                        output.accept(ModItems.PHOTOBOOK.get());
                    })
                    .build());

    private ModCreativeTabs() {}

    public static void register(IEventBus bus) {
        TABS.register(bus);
    }
}
