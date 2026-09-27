package me.jamino.printer.registry;

import me.jamino.printer.Printer;
import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModSounds {
    private static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(Registries.SOUND_EVENT, Printer.MODID);
    public static final DeferredHolder<SoundEvent, SoundEvent> BUTTON = sound("ui.button");
    public static final DeferredHolder<SoundEvent, SoundEvent> PREVIEW_LOAD = sound("ui.preview_load");
    public static final DeferredHolder<SoundEvent, SoundEvent> PRINT = sound("ui.print");
    public static final DeferredHolder<SoundEvent, SoundEvent> PRINT_RETURN = sound("ui.print_return");
    public static final DeferredHolder<SoundEvent, SoundEvent> PAPER_FEED = sound("ui.paper_feed");
    public static final DeferredHolder<SoundEvent, SoundEvent> PAPER_EJECT = sound("ui.paper_eject");

    private ModSounds() {}

    private static DeferredHolder<SoundEvent, SoundEvent> sound(String name) {
        return SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent(Printer.id(name)));
    }

    public static void register(IEventBus bus) { SOUNDS.register(bus); }
}
