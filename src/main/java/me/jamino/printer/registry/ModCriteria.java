package me.jamino.printer.registry;

import me.jamino.printer.Printer;
import me.jamino.printer.advancement.PrinterActionTrigger;
import net.minecraft.advancements.CriterionTrigger;
import net.minecraft.core.registries.Registries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModCriteria {
    private static final DeferredRegister<CriterionTrigger<?>> CRITERIA =
            DeferredRegister.create(Registries.TRIGGER_TYPE, Printer.MODID);
    public static final DeferredHolder<CriterionTrigger<?>, PrinterActionTrigger> ACTION =
            CRITERIA.register("action", PrinterActionTrigger::new);

    private ModCriteria() {}
    public static void register(IEventBus bus) { CRITERIA.register(bus); }
}
