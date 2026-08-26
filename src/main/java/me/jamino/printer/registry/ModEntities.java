package me.jamino.printer.registry;

import me.jamino.printer.Printer;
import me.jamino.printer.entity.PrintedImageEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(Registries.ENTITY_TYPE, Printer.MODID);
    public static final DeferredHolder<EntityType<?>, EntityType<PrintedImageEntity>> PRINTED_IMAGE = ENTITIES.register(
            "printed_image", () -> EntityType.Builder.<PrintedImageEntity>of(PrintedImageEntity::new, MobCategory.MISC)
                    .sized(0.5F, 0.5F).clientTrackingRange(10).updateInterval(Integer.MAX_VALUE)
                    .build("printed_image"));

    private ModEntities() {}

    public static void register(IEventBus bus) {
        ENTITIES.register(bus);
    }
}
