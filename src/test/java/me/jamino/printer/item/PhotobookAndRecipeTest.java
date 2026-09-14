package me.jamino.printer.item;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import io.netty.buffer.Unpooled;
import me.jamino.printer.data.ImageReference;
import me.jamino.printer.data.PrintMode;
import me.jamino.printer.inventory.PhotobookMenu;
import me.jamino.printer.registry.ModDataComponents;
import me.jamino.printer.registry.ModItems;
import net.minecraft.core.NonNullList;
import net.minecraft.data.registries.VanillaRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.RegistryOps;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.item.crafting.Recipe;
import org.junit.jupiter.api.Test;
import java.nio.charset.StandardCharsets;
import static org.junit.jupiter.api.Assertions.*;

class PhotobookAndRecipeTest {
    @Test void contentsCopySparseSlotsAndRoundTripOverNetwork() {
        var photo = new ItemStack(ModItems.IMAGE.get());
        var reference = new ImageReference("ab".repeat(32), 80, 40, 2, 1, "Title", PrintMode.COLOR, 0x224466, 160, 80);
        photo.set(ModDataComponents.IMAGE_REFERENCE.get(), reference);
        var slots = NonNullList.withSize(PhotobookMenu.CAPACITY, ItemStack.EMPTY);
        slots.set(17, photo);
        var book = new ItemStack(ModItems.PHOTOBOOK.get());
        book.set(ModDataComponents.PHOTOBOOK_CONTENTS.get(), ItemContainerContents.fromItems(slots));
        photo.shrink(1);
        var buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), net.minecraft.core.RegistryAccess.fromRegistryOfRegistries(
                net.minecraft.core.registries.BuiltInRegistries.REGISTRY));
        try {
            ItemStack.STREAM_CODEC.encode(buffer, book);
            var received = ItemStack.STREAM_CODEC.decode(buffer);
            var copied = NonNullList.withSize(PhotobookMenu.CAPACITY, ItemStack.EMPTY);
            received.get(ModDataComponents.PHOTOBOOK_CONTENTS.get()).copyInto(copied);
            assertTrue(copied.get(0).isEmpty());
            assertEquals(reference, copied.get(17).get(ModDataComponents.IMAGE_REFERENCE.get()));
            copied.get(17).shrink(1);
            assertEquals(1, received.get(ModDataComponents.PHOTOBOOK_CONTENTS.get()).nonEmptyStream().count());
        } finally { buffer.release(); }
    }

    @Test void photobookAcceptsOnlyPrintedImages() {
        assertFalse(PhotobookMenu.acceptsPhoto(new ItemStack(Items.PAPER)));
        assertFalse(PhotobookMenu.acceptsPhoto(new ItemStack(ModItems.IMAGE.get())));
        assertFalse(PhotobookMenu.acceptsPhoto(new ItemStack(ModItems.PHOTOBOOK.get())));
        assertEquals(1, new ItemStack(ModItems.PHOTOBOOK.get()).getMaxStackSize());
    }

    @Test void cartridgeAndPhotobookRecipesDecodeWithExactIngredients() throws Exception {
        var lookup = VanillaRegistries.createLookup();
        for (String name : new String[]{"black_cartridge", "color_cartridge", "photobook"}) {
            try (var stream = getClass().getResourceAsStream("/data/printer/recipe/" + name + ".json")) {
                assertNotNull(stream);
                var json = JsonParser.parseString(new String(stream.readAllBytes(), StandardCharsets.UTF_8));
                var recipe = Recipe.CODEC.parse(RegistryOps.create(JsonOps.INSTANCE, lookup), json).getOrThrow();
                assertEquals("printer:" + name, net.minecraft.core.registries.BuiltInRegistries.ITEM
                        .getKey(recipe.getResultItem(lookup).getItem()).toString());
                var ingredients = recipe.getIngredients();
                if (name.equals("photobook")) {
                    assertEquals(3, ingredients.size());
                    assertEquals(2, ingredients.stream().filter(i -> i.test(new ItemStack(Items.LEATHER))).count());
                    assertEquals(1, ingredients.stream().filter(i -> i.test(new ItemStack(Items.BOOK))).count());
                } else {
                    assertEquals(name.equals("black_cartridge") ? 2 : 5, ingredients.size());
                    assertEquals(1, ingredients.stream().filter(i -> i.test(new ItemStack(Items.INK_SAC))).count());
                    assertEquals(1, ingredients.stream().filter(i -> i.test(new ItemStack(Items.IRON_NUGGET))).count());
                    assertTrue(ingredients.stream().noneMatch(i -> i.test(new ItemStack(Items.BLACK_DYE))));
                    if (name.equals("color_cartridge")) for (var dye : new net.minecraft.world.item.Item[]{
                            Items.CYAN_DYE, Items.MAGENTA_DYE, Items.YELLOW_DYE})
                        assertEquals(1, ingredients.stream().filter(i -> i.test(new ItemStack(dye))).count());
                }
            }
        }
    }
}
