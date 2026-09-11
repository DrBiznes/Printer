package me.jamino.printer.advancement;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.advancements.critereon.ContextAwarePredicate;
import net.minecraft.advancements.critereon.EntityPredicate;
import net.minecraft.advancements.critereon.SimpleCriterionTrigger;
import net.minecraft.server.level.ServerPlayer;

import java.util.Optional;

public final class PrinterActionTrigger extends SimpleCriterionTrigger<PrinterActionTrigger.Instance> {
    @Override public Codec<Instance> codec() { return Instance.CODEC; }

    public void trigger(ServerPlayer player, String action) {
        trigger(player, instance -> instance.matches(action));
    }

    public record Instance(Optional<ContextAwarePredicate> player, String action) implements SimpleInstance {
        public static final Codec<Instance> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("player").forGetter(Instance::player),
                Codec.STRING.fieldOf("action").forGetter(Instance::action)
        ).apply(instance, Instance::new));

        public boolean matches(String completedAction) { return action.equals(completedAction); }
    }
}
