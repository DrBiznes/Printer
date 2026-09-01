package me.jamino.printer.job;

import me.jamino.printer.Config;
import me.jamino.printer.Printer;
import me.jamino.printer.block.entity.PrinterBlockEntity;
import me.jamino.printer.data.ImageReference;
import me.jamino.printer.data.PrintFrame;
import me.jamino.printer.data.PrintMode;
import me.jamino.printer.data.PrinterPreset;
import me.jamino.printer.image.ImageProcessor;
import me.jamino.printer.image.ImageSizing;
import me.jamino.printer.image.ImageStore;
import me.jamino.printer.network.ModNetworking;
import me.jamino.printer.registry.ModDataComponents;
import me.jamino.printer.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class PrinterJobService {
    private static final ExecutorService IMAGE_EXECUTOR = Executors.newFixedThreadPool(2, runnable -> {
        Thread thread = new Thread(runnable, "Printer image worker");
        thread.setDaemon(true);
        return thread;
    });

    private PrinterJobService() {}

    public static void requestLoad(ServerLevel level, BlockPos pos, ServerPlayer player, String url, String title) {
        if (!(level.getBlockEntity(pos) instanceof PrinterBlockEntity printer) || printer.isPrinting()) return;
        if (url == null || url.length() > 2048 || title == null || title.length() > 64) {
            fail(printer, player, "Invalid URL or title length");
            return;
        }
        printer.setJobState(true, "gui.printer.status.loading");
        CompletableFuture.supplyAsync(() -> {
            try { return ImageProcessor.downloadCanonical(url); }
            catch (Exception exception) { throw new RuntimeException(exception); }
        }, IMAGE_EXECUTOR).whenComplete((source, error) -> level.getServer().execute(() -> {
            if (!(level.getBlockEntity(pos) instanceof PrinterBlockEntity current)) return;
            if (error != null) {
                Printer.LOGGER.warn("Failed to load printer image from {}", url, error);
                fail(current, player, readable(error));
                return;
            }
            if (!ImageStore.putSource(level.getServer(), source)) {
                fail(current, player, "Printer image storage is full");
                return;
            }
            int maximumAutoEdge = Math.min(Config.SERVER.autoSizeMaxBlocks.get(),
                    Config.SERVER.maxPlacementBlocks.get());
            ImageSizing.Size size = ImageSizing.automatic(source.width(), source.height(), maximumAutoEdge);
            current.setPreset(new PrinterPreset(source.contentId(), title, source.width(), source.height(),
                    size.width(), size.height(), PrintFrame.OAK));
            current.setJobState(false, "gui.printer.status.ready");
            player.sendSystemMessage(Component.translatable("message.printer.image_saved",
                    source.width(), source.height(), size.width(), size.height()));
        }));
    }

    public static void requestResize(ServerLevel level, BlockPos pos, int change) {
        if (!(level.getBlockEntity(pos) instanceof PrinterBlockEntity printer) || printer.isPrinting()) return;
        PrinterPreset preset = printer.getPreset().orElse(null);
        if (preset == null || (change != -1 && change != 1)) return;
        int currentLongEdge = Math.max(preset.blocksWide(), preset.blocksHigh());
        int maximum = Config.SERVER.maxPlacementBlocks.get();
        int requestedLongEdge = Math.clamp(currentLongEdge + change, 1, maximum);
        ImageSizing.Size size = ImageSizing.forLongEdge(preset.sourceWidth(), preset.sourceHeight(), requestedLongEdge);
        printer.setPreset(new PrinterPreset(preset.sourceId(), preset.title(), preset.sourceWidth(),
                preset.sourceHeight(), size.width(), size.height(), preset.frame()));
    }

    public static void requestFrame(ServerLevel level, BlockPos pos, int change) {
        if (!(level.getBlockEntity(pos) instanceof PrinterBlockEntity printer) || printer.isPrinting()) return;
        PrinterPreset preset = printer.getPreset().orElse(null);
        if (preset == null || (change != -1 && change != 1)) return;
        printer.setPreset(new PrinterPreset(preset.sourceId(), preset.title(), preset.sourceWidth(),
                preset.sourceHeight(), preset.blocksWide(), preset.blocksHigh(), preset.frame().next(change)));
    }

    public static void requestPrint(ServerLevel level, BlockPos pos, ServerPlayer player) {
        if (!(level.getBlockEntity(pos) instanceof PrinterBlockEntity printer) || printer.isPrinting()) return;
        PrinterPreset preset = printer.getPreset().orElse(null);
        if (preset == null) { fail(printer, player, "No saved image is configured"); return; }
        if (!printer.hasPrintingSupplies()) {
            fail(printer, player, "This print needs " + preset.requiredPaper()
                    + " paper, ink, and an empty output slot");
            return;
        }
        byte[] source = ImageStore.getSource(level.getServer(), preset.sourceId());
        if (source == null) { fail(printer, player, "Saved image data is unavailable"); return; }
        PrintMode mode = printer.isMonochromeSupply() ? PrintMode.MONOCHROME : PrintMode.COLOR;
        ImageSizing.PixelSize textureSize = ImageSizing.textureSize(preset.sourceWidth(), preset.sourceHeight(),
                preset.blocksWide(), preset.blocksHigh());
        printer.setJobState(true, "gui.printer.status.printing");
        CompletableFuture.supplyAsync(() -> {
            try { return ImageProcessor.createVariant(source, textureSize.width(), textureSize.height(), mode); }
            catch (Exception exception) { throw new RuntimeException(exception); }
        }, IMAGE_EXECUTOR).whenComplete((variant, error) -> level.getServer().execute(() -> {
            if (!(level.getBlockEntity(pos) instanceof PrinterBlockEntity current)) return;
            if (error != null) {
                Printer.LOGGER.warn("Failed to process saved printer image {}", preset.sourceId(), error);
                fail(current, player, readable(error));
                return;
            }
            if (!current.hasPrintingSupplies()) { fail(current, player, "Printing supplies changed while the job was running"); return; }
            if (!ImageStore.putVariant(level.getServer(), variant)) { fail(current, player, "Printer image storage is full"); return; }
            ItemStack output = new ItemStack(ModItems.IMAGE.get());
            output.set(ModDataComponents.IMAGE_REFERENCE.get(), new ImageReference(
                    variant.contentId(), variant.width(), variant.height(), preset.blocksWide(), preset.blocksHigh(),
                    preset.title(), mode, preset.frame()));
            current.consumeSupplies();
            current.setOutput(output);
            current.setJobState(false, "gui.printer.status.complete");
            if (player != null) {
                ModNetworking.sendImage(player, variant.contentId(), variant.png());
                player.sendSystemMessage(Component.translatable("message.printer.print_complete"));
            }
        }));
    }

    private static void fail(PrinterBlockEntity printer, ServerPlayer player, String message) {
        printer.setJobState(false, "gui.printer.status.error");
        if (player != null) player.sendSystemMessage(Component.literal(message));
    }

    private static String readable(Throwable throwable) {
        Throwable cause = throwable;
        while (cause.getCause() != null) cause = cause.getCause();
        return cause.getMessage() == null ? cause.getClass().getSimpleName() : cause.getMessage();
    }
}
