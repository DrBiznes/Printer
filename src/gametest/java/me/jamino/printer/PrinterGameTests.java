package me.jamino.printer;

import me.jamino.printer.block.PrinterBlock;
import me.jamino.printer.block.entity.PrinterBlockEntity;
import me.jamino.printer.data.*;
import me.jamino.printer.entity.PrintedImageEntity;
import me.jamino.printer.image.*;
import me.jamino.printer.inventory.PrinterMenu;
import me.jamino.printer.job.PrinterJobService;
import me.jamino.printer.network.*;
import me.jamino.printer.registry.*;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.*;
import net.neoforged.neoforge.gametest.*;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.UUID;

@GameTestHolder(Printer.MODID)
@PrefixGameTestTemplate(false)
public final class PrinterGameTests {
    private static final BlockPos MACHINE = new BlockPos(3, 2, 3);

    private static PrinterBlockEntity machine(GameTestHelper helper) {
        helper.setBlock(MACHINE, ModBlocks.PRINTER.get().defaultBlockState());
        return helper.getBlockEntity(MACHINE);
    }

    private static ServerPlayer player(GameTestHelper helper, PrinterBlockEntity printer) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        net.neoforged.neoforge.network.registration.NetworkRegistry.onMinecraftRegister(player.connection.getConnection(),
                java.util.Set.of(ModNetworking.UploadReplyPayload.TYPE.id(), ModNetworking.ImageChunkPayload.TYPE.id()));
        var pos = printer.getBlockPos();
        player.setPos(pos.getX() + 0.5, pos.getY(), pos.getZ() + 1.5);
        player.containerMenu = new PrinterMenu(42, player.getInventory(), pos);
        printer.setOwner(player.getUUID());
        return player;
    }

    private static byte[] png() {
        try {
            BufferedImage image = new BufferedImage(32, 16, BufferedImage.TYPE_INT_ARGB);
            image.setRGB(2, 3, 0xFFFF0000);
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            ImageIO.write(image, "png", bytes);
            return bytes.toByteArray();
        } catch (Exception error) { throw new RuntimeException(error); }
    }

    private static boolean done(ServerPlayer player, String name) {
        var advancement = player.server.getAdvancements().get(Printer.id(name));
        if (advancement == null) throw new IllegalStateException("Missing advancement " + name);
        return player.getAdvancements().getOrStartProgress(advancement).isDone();
    }

    @GameTest(template = "empty", timeoutTicks = 100)
    public static void photobookStoresEighteenAndPersistsSparsePages(GameTestHelper helper) {
        var player = player(helper, machine(helper));
        var inventory = player.getInventory();
        inventory.clearContent();
        inventory.selected = 0;
        ItemStack book = new ItemStack(ModItems.PHOTOBOOK.get());
        inventory.setItem(0, book);
        var menu = new me.jamino.printer.inventory.PhotobookMenu(43, inventory, 0);
        player.containerMenu = menu;
        for (int i = 0; i < 19; i++) {
            ItemStack photo = new ItemStack(ModItems.IMAGE.get());
            photo.set(ModDataComponents.IMAGE_REFERENCE.get(), new ImageReference("ab".repeat(32),
                    32, 16, 2, 1, "Photo " + i, PrintMode.COLOR, 0x224466, 32, 16));
            inventory.setItem(9, photo);
            menu.clicked(18, 0, net.minecraft.world.inventory.ClickType.QUICK_MOVE, player);
            helper.assertTrue(inventory.getItem(9).isEmpty() == (i < 18), "Exactly 18 prints fit");
        }
        helper.assertValueEqual(menu.slots.size(), 54, "18 photo slots plus 36 player slots");
        helper.assertTrue(!menu.slots.get(0).mayPlace(new ItemStack(ModItems.IMAGE.get())), "Reject blanks");
        helper.assertTrue(!menu.slots.get(0).mayPlace(book), "Reject nested books");
        helper.assertTrue(!menu.slots.get(0).mayPlace(new ItemStack(Items.PAPER)), "Reject other items");
        // Remove a middle image and verify its hole survives save/reopen.
        menu.clicked(5, 0, net.minecraft.world.inventory.ClickType.PICKUP, player);
        helper.assertTrue(menu.getCarried().is(ModItems.IMAGE.get()), "Photo can be removed");
        var saved = ItemStack.parseOptional(helper.getLevel().registryAccess(),
                (CompoundTag) book.save(helper.getLevel().registryAccess()));
        menu.removed(player);
        inventory.setItem(0, saved);
        var reopened = new me.jamino.printer.inventory.PhotobookMenu(44, inventory, 0);
        helper.assertTrue(reopened.slots.get(5).getItem().isEmpty(), "Empty page slot survives NBT");
        helper.assertValueEqual(reopened.slots.get(17).getItem().get(ModDataComponents.IMAGE_REFERENCE.get()).title(),
                "Photo 17", "Last page survives NBT");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 100)
    public static void photobookLocksMainAndOffhandAgainstAllMovement(GameTestHelper helper) {
        var player = player(helper, machine(helper));
        var inventory = player.getInventory();
        for (int bookSlot : new int[]{0, 40}) {
            inventory.clearContent();
            ItemStack book = new ItemStack(ModItems.PHOTOBOOK.get());
            inventory.setItem(bookSlot, book);
            inventory.setItem(9, new ItemStack(Items.DIAMOND));
            var menu = new me.jamino.printer.inventory.PhotobookMenu(43, inventory, bookSlot);
            player.containerMenu = menu;
            helper.assertTrue(menu.stillValid(player), "Opens in either hand");
            menu.clicked(18, bookSlot, net.minecraft.world.inventory.ClickType.SWAP, player);
            helper.assertTrue(inventory.getItem(bookSlot) == book, "Swap cannot move the open book");
            helper.assertTrue(inventory.getItem(9).is(Items.DIAMOND), "Swap cannot overwrite inventory");
            if (bookSlot == 0) {
                for (var type : new net.minecraft.world.inventory.ClickType[]{
                        net.minecraft.world.inventory.ClickType.PICKUP, net.minecraft.world.inventory.ClickType.QUICK_MOVE,
                        net.minecraft.world.inventory.ClickType.THROW, net.minecraft.world.inventory.ClickType.SWAP}) {
                    menu.clicked(45, 1, type, player);
                    helper.assertTrue(inventory.getItem(0) == book, "Carrier locked for " + type);
                }
            }
            inventory.setItem(bookSlot, book.copy());
            helper.assertTrue(!menu.stillValid(player), "Replacement book invalidates old menu");
            menu.clicked(18, 0, net.minecraft.world.inventory.ClickType.PICKUP, player);
            helper.assertTrue(inventory.getItem(9).is(Items.DIAMOND), "Stale menu cannot edit inventory");
        }
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 100)
    public static void invalidUploadStartDoesNotChangePrinterState(GameTestHelper helper) {
        var printer = machine(helper);
        var player = player(helper, printer);
        String status = printer.getStatusKey();
        ServerUploads.begin(player, new ModNetworking.BeginUploadPayload(printer.getBlockPos(), UUID.randomUUID(), 0, "invalid"));
        helper.assertValueEqual(printer.getStatusKey(), status, "Rejected reservation leaves printer state unchanged");
        helper.assertTrue(!printer.isPrinting(), "Rejected upload does not start a job");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 100)
    public static void bundledReadersWorkInTheGameModuleLayer(GameTestHelper helper) throws Exception {
        for (String name : java.util.List.of("lossless.webp", "lossy.webp", "alpha.webp", "animated.webp", "sample.ico", "sample.tga")) {
            try (var stream = PrinterGameTests.class.getResourceAsStream("/images/" + name)) {
                var source = ImageProcessor.canonicalize(stream.readAllBytes());
                helper.assertValueEqual(source.originalWidth(), 8, name + " width");
                helper.assertValueEqual(source.originalHeight(), 6, name + " height");
            }
        }
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 200)
    public static void uploadFailuresLeaveSuppliesAndAdvancementsUntouched(GameTestHelper helper) {
        var printer = machine(helper); var player = player(helper, printer);
        printer.setItem(0, new ItemStack(Items.PAPER, 3));
        printer.setItem(1, new ItemStack(ModItems.BLACK_CARTRIDGE.get()));
        UUID id = UUID.randomUUID();
        byte[] invalid = "not an image".getBytes(java.nio.charset.StandardCharsets.UTF_8);
        ServerUploads.begin(player, new ModNetworking.BeginUploadPayload(printer.getBlockPos(), id, invalid.length, "Bad"));
        ServerUploads.chunk(player, new ModNetworking.UploadChunkPayload(id, 0, invalid));
        helper.startSequence().thenWaitUntil(() -> helper.assertFalse(printer.isPrinting(), "Wait for invalid decode"))
                .thenExecute(() -> {
                    helper.assertTrue(printer.getPreset().isEmpty(), "Invalid bytes cannot save a preset");
                    helper.assertValueEqual(printer.getItem(0).getCount(), 3, "Paper untouched");
                    helper.assertValueEqual(printer.getItem(1).getCount(), 1, "Ink untouched");
                    PrinterJobService.LOAD_THROTTLE.remove(player.getUUID());
                    UUID next = UUID.randomUUID();
                    ServerUploads.begin(player, new ModNetworking.BeginUploadPayload(printer.getBlockPos(), next, 100, "Interrupted"));
                    helper.assertTrue(printer.isPrinting(), "Next transfer can start");
                    ServerUploads.logout(new net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent(player));
                    helper.assertFalse(printer.isPrinting(), "Disconnect releases printer");
                    UUID retry = UUID.randomUUID();
                    ServerUploads.begin(player, new ModNetworking.BeginUploadPayload(printer.getBlockPos(), retry, 100, "Retry"));
                    helper.assertTrue(printer.isPrinting(), "Disconnect releases transfer reservation");
                    ServerUploads.cancel(player, retry);
                    helper.assertFalse(printer.isPrinting(), "Explicit cancel releases printer");
                }).thenSucceed();
    }

    @GameTest(template = "empty", timeoutTicks = 400)
    public static void uploadPrintPlaceAndAdvancements(GameTestHelper helper) {
        var printer = machine(helper);
        var player = player(helper, printer);
        byte[] png = png(); UUID id = UUID.randomUUID();
        ServerUploads.begin(player, new ModNetworking.BeginUploadPayload(printer.getBlockPos(), id, png.length, "Local picture"));
        helper.assertTrue(printer.isPrinting(), "Upload should lock the machine");
        ServerUploads.chunk(player, new ModNetworking.UploadChunkPayload(id, 0, png));
        helper.startSequence().thenWaitUntil(() -> {
            helper.assertFalse(printer.isPrinting(), "Wait for upload decode");
            helper.assertTrue(printer.getPreset().isPresent(), "Upload must save a preset");
        }).thenExecute(() -> {
            helper.assertFalse(done(player, "print_bw"), "Loading must not award a print advancement");
            helper.assertValueEqual(printer.getPreset().orElseThrow().originalWidth(), 32, "Source width");
            helper.assertValueEqual(printer.getPreset().orElseThrow().backgroundColor(), ImageReference.DEFAULT_BACKGROUND_COLOR, "New preset defaults to white");
            PrinterJobService.requestBackground(helper.getLevel(), printer.getBlockPos(), 0x224466);
            PrinterJobService.requestBackground(helper.getLevel(), printer.getBlockPos(), -1);
            PrinterJobService.requestBackground(helper.getLevel(), printer.getBlockPos(), 0x1000000);
            helper.assertValueEqual(printer.getPreset().orElseThrow().backgroundColor(), 0x224466, "Invalid colors cannot change the preset");
            PrinterJobService.requestResize(helper.getLevel(), printer.getBlockPos(), 1);
            helper.assertValueEqual(printer.getPreset().orElseThrow().backgroundColor(), 0x224466, "Resize retains background");
            PrinterJobService.requestResize(helper.getLevel(), printer.getBlockPos(), -1);
            printer.setItem(0, new ItemStack(Items.PAPER, 3));
            printer.setItem(1, new ItemStack(ModItems.COLOR_CARTRIDGE.get()));
            PrinterJobService.requestPrint(helper.getLevel(), printer.getBlockPos(), player);
            PrinterJobService.requestBackground(helper.getLevel(), printer.getBlockPos(), 0xFFFFFF);
            helper.assertValueEqual(printer.getPreset().orElseThrow().backgroundColor(), 0x224466, "Busy printer rejects color changes");
        }).thenWaitUntil(() -> helper.assertFalse(printer.isPrinting(), "Wait for printing"))
                .thenExecute(() -> {
                    helper.assertTrue(printer.getItem(2).is(ModItems.IMAGE.get()), "Print output");
                    helper.assertFalse(done(player, "print_bw"), "Color print must not award print_bw");
                    helper.assertTrue(done(player, "print_color"), "Color print awards print_color");
                    helper.assertFalse(done(player, "automate"), "Manual print must not award automation");
                    helper.assertValueEqual(printer.getItem(0).getCount(), 2, "Paper consumed once");
                    helper.assertValueEqual(printer.getItem(1).getDamageValue(), 1, "One color charge");
                    var reference = printer.getItem(2).get(ModDataComponents.IMAGE_REFERENCE.get());
                    helper.assertValueEqual(reference.sourceWidth(), 32, "Output source metadata");
                    helper.assertValueEqual(reference.backgroundColor(), 0x224466, "Output background metadata");
                    try {
                        var output = ImageProcessor.decodeChecked(ImageStore.getVariant(helper.getLevel().getServer(), reference.contentId()));
                        helper.assertValueEqual(output.getRGB(0, 0), 0xFF224466, "Transparent source uses selected background");
                    } catch (Exception error) { throw new RuntimeException(error); }
                    var wall = helper.absolutePos(new BlockPos(2, 2, 1));
                    helper.getLevel().setBlock(wall, Blocks.STONE.defaultBlockState(), 3);
                    player.setItemInHand(InteractionHand.MAIN_HAND, printer.getItem(2).copy());
                    ModItems.IMAGE.get().useOn(new UseOnContext(player, InteractionHand.MAIN_HAND,
                            new BlockHitResult(Vec3.atCenterOf(wall), Direction.SOUTH, wall, false)));
                    var display = helper.getLevel().getEntitiesOfClass(PrintedImageEntity.class,
                            new AABB(wall).inflate(2)).getFirst();
                    CompoundTag saved = new CompoundTag(); display.saveWithoutId(saved);
                    var restored = new PrintedImageEntity(ModEntities.PRINTED_IMAGE.get(), helper.getLevel());
                    restored.load(saved);
                    helper.assertValueEqual(restored.getReference(), reference, "Wall save/load metadata");
                    helper.assertValueEqual(restored.getPos(), display.getPos(), "Wall save/load position");
                    helper.assertValueEqual(restored.getPickResult().get(ModDataComponents.IMAGE_REFERENCE.get()), reference,
                            "Picked display metadata");
                    helper.assertFalse(saved.contains("Frame"), "New entity save contains no frame metadata");
                    display.dropItem(null);
                    var drops = helper.getLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,
                            display.getBoundingBox().inflate(2));
                    helper.assertTrue(drops.stream().anyMatch(drop -> reference.equals(drop.getItem().get(ModDataComponents.IMAGE_REFERENCE.get()))),
                            "Dropped wall Image retains background and metadata");
                    var frame = new net.minecraft.world.entity.decoration.ItemFrame(helper.getLevel(), wall.south(), Direction.SOUTH);
                    frame.setItem(printer.getItem(2).copy());
                    helper.assertValueEqual(frame.getItem().get(ModDataComponents.IMAGE_REFERENCE.get()), reference, "Item-frame metadata");
                }).thenSucceed();
    }

    @GameTest(template = "empty", timeoutTicks = 100)
    public static void uploadsCancelOnClosedMenuAndRejectInvalidPolicy(GameTestHelper helper) {
        var printer = machine(helper); var player = player(helper, printer);
        UUID id = UUID.randomUUID();
        ServerUploads.begin(player, new ModNetworking.BeginUploadPayload(printer.getBlockPos(), id, 100, "Cancelled"));
        helper.assertTrue(printer.isPrinting(), "Started transfer");
        player.containerMenu = player.inventoryMenu;
        helper.runAfterDelay(2, () -> {
            helper.assertFalse(printer.isPrinting(), "Closing menu cancels transfer");
            helper.assertTrue(printer.getPreset().isEmpty(), "No partial preset");
            PrinterJobService.LOAD_THROTTLE.remove(player.getUUID());
            player.containerMenu = new PrinterMenu(43, player.getInventory(), printer.getBlockPos());
            ServerUploads.begin(player, new ModNetworking.BeginUploadPayload(printer.getBlockPos(), UUID.randomUUID(), -1, "Bad"));
            helper.assertFalse(printer.isPrinting(), "Invalid size must not leave busy state");
            helper.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 400)
    public static void inkSacBackgroundPolicyCannotBeBypassed(GameTestHelper helper) throws Exception {
        var printer = machine(helper);
        var player = player(helper, printer);
        var source = ImageProcessor.canonicalize(png());
        ImageStore.putSource(helper.getLevel().getServer(), source);
        printer.setPreset(new PrinterPreset(source.contentId(), "Ink policy", 32, 16, 1, 1, 0xB02E26, 32, 16));
        printer.setItem(0, new ItemStack(Items.PAPER, 4));
        printer.setItem(1, new ItemStack(ModItems.BLACK_CARTRIDGE.get()));
        PrinterJobService.requestBackground(helper.getLevel(), printer.getBlockPos(), 0x224466);
        helper.assertValueEqual(printer.getPreset().orElseThrow().backgroundColor(), 0xB02E26,
                "Colored background payload rejected with ink sac");
        PrinterJobService.requestPrint(helper.getLevel(), printer.getBlockPos(), player);
        helper.assertFalse(printer.isPrinting(), "Manual colored print rejected before processing");
        helper.assertValueEqual(printer.getStatusKey(), ImageFailure.Reason.MONOCHROME_BACKGROUND.key(),
                "Actionable background policy error");
        printer.updateRedstone(true);
        helper.assertFalse(printer.isPrinting(), "Redstone cannot bypass background policy");
        helper.assertTrue(printer.getItem(2).isEmpty(), "Rejected prints produce no output");
        helper.assertValueEqual(printer.getItem(0).getCount(), 4, "Rejected prints do not consume paper");
        helper.assertValueEqual(printer.getItem(1).getCount(), 1, "Rejected prints do not consume ink");
        printer.updateRedstone(false);
        PrinterJobService.requestBackground(helper.getLevel(), printer.getBlockPos(), 0x000000);
        PrinterJobService.requestBackground(helper.getLevel(), printer.getBlockPos(), 0x808080);
        helper.assertValueEqual(printer.getPreset().orElseThrow().backgroundColor(), 0x000000,
                "Pure black accepted, gray rejected");
        PrinterJobService.requestPrint(helper.getLevel(), printer.getBlockPos(), player);
        helper.assertTrue(printer.isPrinting(), "Manual black background print starts");
        helper.startSequence().thenWaitUntil(() -> helper.assertFalse(printer.isPrinting(), "Wait for black print"))
                .thenExecute(() -> {
                    assertMonochromeBackground(helper, printer, 0x000000);
                    printer.removeItem(2, 1);
                    PrinterJobService.requestBackground(helper.getLevel(), printer.getBlockPos(), 0xFFFFFF);
                    printer.updateRedstone(true);
                    helper.assertTrue(printer.isPrinting(), "Redstone white background print starts");
                }).thenWaitUntil(() -> helper.assertFalse(printer.isPrinting(), "Wait for white print"))
                .thenExecute(() -> {
                    assertMonochromeBackground(helper, printer, 0xFFFFFF);
                    helper.assertValueEqual(printer.getItem(0).getCount(), 2, "Two valid prints consume two paper");
                    helper.assertValueEqual(printer.getItem(1).getDamageValue(), 2, "Two valid prints consume two charges");
                    helper.assertTrue(done(player, "print_bw"), "Ink sac prints grant print_bw");
                    helper.assertFalse(done(player, "print_color"), "Ink sac prints cannot grant print_color advancement");
                }).thenSucceed();
    }

    private static void assertMonochromeBackground(GameTestHelper helper, PrinterBlockEntity printer, int background) {
        var reference = printer.getItem(2).get(ModDataComponents.IMAGE_REFERENCE.get());
        helper.assertTrue(reference != null, "Print output has reference");
        helper.assertValueEqual(reference.mode(), PrintMode.MONOCHROME, "Output mode is monochrome");
        helper.assertValueEqual(reference.backgroundColor(), background, "Output saves allowed background");
        try {
            var image = ImageProcessor.decodeChecked(ImageStore.getVariant(helper.getLevel().getServer(), reference.contentId()));
            helper.assertValueEqual(image.getRGB(0, 0), 0xFF000000 | background, "Transparent area uses allowed background");
        } catch (Exception error) { throw new RuntimeException(error); }
    }

    @GameTest(template = "empty", timeoutTicks = 100)
    public static void jobIdentityAndSupplyModeCannotCrossReplacement(GameTestHelper helper) {
        var printer = machine(helper);
        var preset = new PrinterPreset("a".repeat(64), "Preset", 32, 16, 1, 1, 0x224466, 3000, 1500);
        printer.setPreset(preset); printer.setItem(0, new ItemStack(Items.PAPER, 3));
        printer.setItem(1, new ItemStack(ModItems.COLOR_CARTRIDGE.get()));
        long job = printer.beginJob("gui.printer.status.printing");
        helper.assertTrue(printer.matchesPrint(preset, PrintMode.COLOR), "Initial supplies match");
        printer.setItem(1, new ItemStack(ModItems.BLACK_CARTRIDGE.get()));
        helper.assertFalse(printer.matchesPrint(preset, PrintMode.COLOR), "Ink swap must reject stale result");
        ItemStack carried = new ItemStack(ModItems.PRINTER.get()); printer.copyPresetToItem(carried);
        helper.setBlock(MACHINE, Blocks.AIR); helper.setBlock(MACHINE, ModBlocks.PRINTER.get());
        var replacement = (PrinterBlockEntity) helper.getBlockEntity(MACHINE);
        helper.assertFalse(PrinterJobService.isCurrent(helper.getLevel(), printer, job), "Old result cannot target replacement");
        replacement.restorePresetFromItem(carried);
        helper.assertValueEqual(replacement.getPreset().orElseThrow(), preset, "Carried preset retains original dimensions");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 400)
    public static void automationUsesRealHoppersAndOnlyRisingEdges(GameTestHelper helper) {
        var printer = machine(helper); var owner = player(helper, printer);
        try {
            var source = ImageProcessor.canonicalize(png());
            ImageStore.putSource(helper.getLevel().getServer(), source);
            printer.setPreset(new PrinterPreset(source.contentId(), "Automatic", 32, 16, 1, 1, ImageReference.DEFAULT_BACKGROUND_COLOR, 32, 16));
        } catch (Exception error) { throw new RuntimeException(error); }
        helper.setBlock(MACHINE.above(), Blocks.HOPPER);
        helper.setBlock(MACHINE.west(), Blocks.HOPPER.defaultBlockState().setValue(net.minecraft.world.level.block.HopperBlock.FACING, Direction.EAST));
        var ink = (net.minecraft.world.level.block.entity.HopperBlockEntity) helper.getBlockEntity(MACHINE.above());
        var paper = (net.minecraft.world.level.block.entity.HopperBlockEntity) helper.getBlockEntity(MACHINE.west());
        ink.setItem(0, new ItemStack(ModItems.BLACK_CARTRIDGE.get())); paper.setItem(0, new ItemStack(Items.PAPER, 3));
        helper.startSequence().thenWaitUntil(() -> helper.assertTrue(printer.hasPrintingSupplies(), "Hoppers supply printer"))
                .thenExecute(() -> {
                    helper.assertTrue(printer.hasAutomatedSupplies(), "Real hopper input records automation");
                    helper.setBlock(MACHINE.south(), Blocks.REDSTONE_BLOCK);
                }).thenWaitUntil(() -> helper.assertFalse(printer.isPrinting(), "Wait for redstone print"))
                .thenExecute(() -> {
                    helper.assertTrue(printer.getItem(2).is(ModItems.IMAGE.get()), "Redstone output");
                    printer.getAutomationHandler(Direction.DOWN).extractItem(0, 1, false);
                    printer.updateRedstone(true);
                    helper.assertFalse(printer.isPrinting(), "Held power cannot repeat");
                    helper.setBlock(MACHINE.south(), Blocks.AIR);
                }).thenWaitUntil(() -> helper.assertTrue(printer.hasPrintingSupplies(), "Wait for hopper refill"))
                .thenIdle(3).thenExecute(() -> {
                    helper.assertTrue(done(owner, "automate"), "Automation credits printer owner");
                    helper.setBlock(MACHINE.south(), Blocks.REDSTONE_BLOCK);
                    helper.assertTrue(printer.isPrinting(), "New edge starts next print");
                }).thenWaitUntil(() -> helper.assertFalse(printer.isPrinting(), "Wait for second print"))
                .thenExecute(() -> helper.assertTrue(printer.getItem(2).is(ModItems.IMAGE.get()), "Second output"))
                .thenSucceed();
    }
}
