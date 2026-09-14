package me.jamino.printer.registry;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import me.jamino.printer.advancement.PrinterActionTrigger;
import me.jamino.printer.image.ImageFailure;
import net.minecraft.advancements.Advancement;
import net.minecraft.data.registries.VanillaRegistries;
import net.minecraft.resources.RegistryOps;
import com.mojang.serialization.JsonOps;
import org.junit.jupiter.api.Test;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;

class TranslationAndAdvancementTest {
    private JsonObject read(String path) throws Exception {
        try (var input = getClass().getResourceAsStream(path)) {
            assertNotNull(input, path);
            return JsonParser.parseString(new String(input.readAllBytes(), StandardCharsets.UTF_8)).getAsJsonObject();
        }
    }

    @Test void everyErrorCategoryHasActionableTranslation() throws Exception {
        var language = read("/assets/printer/lang/en_us.json");
        for (var reason : ImageFailure.Reason.values()) {
            assertTrue(language.has(reason.key()), reason.name());
            assertTrue(language.get(reason.key()).getAsString().length() > 20);
        }
    }

    @Test void backgroundPaletteUsesResolvedVanillaColorNames() {
        for (var color : net.minecraft.world.item.DyeColor.values()) {
            assertTrue(net.minecraft.locale.Language.getInstance().has("color.minecraft." + color.getName()), color.getName());
        }
    }

    @Test void allAdvancementsDecodeAndMatchOnlyTheirServerAction() throws Exception {
        var language = read("/assets/printer/lang/en_us.json");
        var ops = RegistryOps.create(JsonOps.INSTANCE, VanillaRegistries.createLookup());
        for (String name : new String[]{"root", "craft_black_ink", "craft_color_ink", "print_bw", "print_color", "print_max_size", "automate"}) {
            var json = read("/data/printer/advancement/" + name + ".json");
            assertNotNull(Advancement.CODEC.parse(ops, json).getOrThrow());
            assertTrue(language.has("advancements.printer." + name + ".title"));
            assertTrue(language.has("advancements.printer." + name + ".description"));
            if (name.equals("root") || name.equals("craft_black_ink") || name.equals("craft_color_ink")) continue;
            var instance = new PrinterActionTrigger.Instance(Optional.empty(), name);
            assertTrue(instance.matches(name));
            assertFalse(instance.matches("failed"));
            assertFalse(instance.matches(name + "_requested"));
        }
    }
}
