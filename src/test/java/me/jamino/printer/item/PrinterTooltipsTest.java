package me.jamino.printer.item;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import me.jamino.printer.data.ImageReference;
import me.jamino.printer.data.PrintFrame;
import me.jamino.printer.data.PrintMode;
import me.jamino.printer.registry.ModDataComponents;
import me.jamino.printer.registry.ModItems;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PrinterTooltipsTest {
    private static final String HINT = "tooltip.printer.hold_shift";

    @AfterEach
    void resetKeyboardState() {
        PrinterTooltips.setShiftDownSupplier(() -> false);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void registeredPrinterExplainsWorkflowAndAutomationOnlyOnShift(boolean expanded) throws Exception {
        var tooltip = tooltip(new ItemStack(ModItems.PRINTER.get()), expanded);
        assertHas(tooltip, "item.printer.printer.summary");
        assertEquals(!expanded, has(tooltip, HINT));
        for (String detail : List.of("workflow", "paper", "ink", "output", "hoppers", "extraction", "redstone", "busy")) {
            assertEquals(expanded, has(tooltip, "item.printer.printer." + detail), detail);
        }
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 2, 3})
    void cartridgeDisplaysActualRemainingChargesInBothStates(int remaining) throws Exception {
        ItemStack cartridge = new ItemStack(ModItems.COLOR_CARTRIDGE.get());
        cartridge.setDamageValue(cartridge.getMaxDamage() - remaining);
        for (boolean expanded : new boolean[]{false, true}) {
            var tooltip = tooltip(cartridge, expanded);
            assertArrayEquals(new Object[]{remaining}, translated(tooltip,
                    "item.printer.color_cartridge.charges").getArgs());
            assertHas(tooltip, "item.printer.color_cartridge.summary");
            assertEquals(!expanded, has(tooltip, HINT));
            assertEquals(expanded, has(tooltip, "item.printer.color_cartridge.insert"));
            assertEquals(expanded, has(tooltip, "item.printer.color_cartridge.usage"));
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void blankImageExplainsThatItCannotBePlaced(boolean expanded) throws Exception {
        var tooltip = tooltip(new ItemStack(ModItems.IMAGE.get()), expanded);
        assertHas(tooltip, "item.printer.image.unprinted");
        assertFalse(has(tooltip, "item.printer.image.place"));
        assertFalse(has(tooltip, "item.printer.image.dimensions"));
        assertEquals(expanded, has(tooltip, "item.printer.image.unprinted_help"));
        assertEquals(!expanded, has(tooltip, HINT));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   ", "Sunset"})
    void printedImageHasTitleAndPlacementWithAccurateExpandedMetadata(String title) throws Exception {
        String contentId = "ab".repeat(32);
        for (PrintMode mode : PrintMode.values()) {
            for (PrintFrame frame : PrintFrame.values()) {
                ItemStack image = new ItemStack(ModItems.IMAGE.get());
                image.set(ModDataComponents.IMAGE_REFERENCE.get(), new ImageReference(
                        contentId, 320, 160, 4, 2, title, mode, frame, 3000, 1500));
                for (boolean expanded : new boolean[]{false, true}) {
                    var tooltip = tooltip(image, expanded);
                    Component displayedTitle = (Component) translated(tooltip, "item.printer.image.title").getArgs()[0];
                    if (title.isBlank()) {
                        assertEquals("item.printer.image.untitled",
                                ((TranslatableContents) displayedTitle.getContents()).getKey());
                    } else {
                        assertEquals(title, displayedTitle.getString());
                    }
                    assertHas(tooltip, "item.printer.image.place");
                    assertEquals(!expanded, has(tooltip, HINT));
                    assertEquals(expanded, has(tooltip, "item.printer.image.dimensions"));
                    if (expanded) {
                        assertArrayEquals(new Object[]{3000, 1500}, translated(tooltip, "item.printer.image.source").getArgs());
                        assertArrayEquals(new Object[]{320, 160}, translated(tooltip, "item.printer.image.dimensions").getArgs());
                        assertArrayEquals(new Object[]{4, 2}, translated(tooltip, "item.printer.image.blocks").getArgs());
                        Component frameName = (Component) translated(tooltip, "item.printer.image.frame").getArgs()[0];
                        assertEquals("gui.printer.frame." + frame.getSerializedName(),
                                ((TranslatableContents) frameName.getContents()).getKey());
                        assertHas(tooltip, "item.printer.image.mode." + mode.getSerializedName());
                        assertHas(tooltip, "item.printer.image.placement_help");
                    }
                    assertFalse(tooltip.toString().contains(contentId), "Internal image IDs must not appear in help");
                }
            }
        }
    }

    @Test
    void legacyPrintedImageUsesUnknownSourceInsteadOfMislabelingTexturePixels() throws Exception {
        ItemStack image = new ItemStack(ModItems.IMAGE.get());
        image.set(ModDataComponents.IMAGE_REFERENCE.get(), new ImageReference("a".repeat(64),
                128, 128, 1, 1, "Old print", PrintMode.COLOR, PrintFrame.NONE));
        var tooltip = tooltip(image, true);
        assertHas(tooltip, "item.printer.image.source_unknown");
        assertFalse(has(tooltip, "item.printer.image.source"));
    }

    @Test
    void tooltipIsCollapsedWithoutClientKeyboardClasses() {
        // The NeoForge unit-test launch target runs on Dist.DEDICATED_SERVER.
        assertFalse(PrinterTooltips.isExpanded());
        List<Component> tooltip = new ArrayList<>();
        ModItems.PRINTER.get().appendHoverText(new ItemStack(ModItems.PRINTER.get()),
                Item.TooltipContext.EMPTY, tooltip, TooltipFlag.NORMAL);
        assertHas(tooltip, HINT);
    }

    private static List<Component> tooltip(ItemStack stack, boolean expanded) throws Exception {
        PrinterTooltips.setShiftDownSupplier(() -> expanded);
        List<Component> result = new ArrayList<>();
        stack.getItem().appendHoverText(stack, Item.TooltipContext.EMPTY, result, TooltipFlag.NORMAL);
        try (var stream = PrinterTooltipsTest.class.getResourceAsStream("/assets/printer/lang/en_us.json")) {
            assertNotNull(stream);
            JsonObject translations = JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
            for (Component component : result) assertTranslated(component, translations);
        }
        return result;
    }

    private static void assertTranslated(Component component, JsonObject translations) {
        if (component.getContents() instanceof TranslatableContents contents) {
            if (contents.getKey().contains(".printer")) {
                assertTrue(translations.has(contents.getKey()), "Missing translation: " + contents.getKey());
                String pattern = translations.get(contents.getKey()).getAsString();
                assertEquals(contents.getArgs().length, pattern.split("%s", -1).length - 1,
                        "Translation arguments: " + contents.getKey());
            }
            for (Object argument : contents.getArgs()) {
                if (argument instanceof Component nested) assertTranslated(nested, translations);
            }
        }
        for (Component sibling : component.getSiblings()) assertTranslated(sibling, translations);
    }

    private static boolean has(List<Component> tooltip, String key) {
        return tooltip.stream().anyMatch(component -> component.getContents() instanceof TranslatableContents contents
                && contents.getKey().equals(key));
    }

    private static void assertHas(List<Component> tooltip, String key) {
        assertTrue(has(tooltip, key), "Missing tooltip: " + key);
    }

    private static TranslatableContents translated(List<Component> tooltip, String key) {
        return tooltip.stream().map(Component::getContents).filter(TranslatableContents.class::isInstance)
                .map(TranslatableContents.class::cast).filter(contents -> contents.getKey().equals(key))
                .findFirst().orElseThrow(() -> new AssertionError("Missing tooltip: " + key));
    }
}
