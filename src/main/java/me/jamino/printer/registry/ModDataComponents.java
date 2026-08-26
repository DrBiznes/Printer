package me.jamino.printer.registry;

import me.jamino.printer.Printer;
import me.jamino.printer.data.ImageReference;
import me.jamino.printer.data.PrinterPreset;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModDataComponents {
    public static final DeferredRegister<DataComponentType<?>> COMPONENTS =
            DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, Printer.MODID);
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<ImageReference>> IMAGE_REFERENCE =
            COMPONENTS.register("image_reference", () -> DataComponentType.<ImageReference>builder()
                    .persistent(ImageReference.CODEC).networkSynchronized(ImageReference.STREAM_CODEC).build());
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<PrinterPreset>> PRINTER_PRESET =
            COMPONENTS.register("printer_preset", () -> DataComponentType.<PrinterPreset>builder()
                    .persistent(PrinterPreset.CODEC).networkSynchronized(PrinterPreset.STREAM_CODEC).build());

    private ModDataComponents() {}

    public static void register(IEventBus bus) {
        COMPONENTS.register(bus);
    }
}
