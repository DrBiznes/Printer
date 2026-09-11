package me.jamino.printer.job;

import me.jamino.printer.Config;
import me.jamino.printer.Printer;
import me.jamino.printer.block.entity.PrinterBlockEntity;
import me.jamino.printer.data.*;
import me.jamino.printer.image.*;
import me.jamino.printer.network.ModNetworking;
import me.jamino.printer.registry.*;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.concurrent.*;
import java.util.function.Consumer;

import static me.jamino.printer.image.ImageFailure.Reason.*;

public final class PrinterJobService {
    public static final RequestThrottle LOAD_THROTTLE = new RequestThrottle();
    private static ThreadPoolExecutor executor;

    private PrinterJobService() {}

    private static ThreadPoolExecutor executor() {
        if (executor == null) executor = new ThreadPoolExecutor(2, 2, 0, TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(4), runnable -> {
                    Thread thread = new Thread(runnable, "Printer image worker");
                    thread.setDaemon(true);
                    return thread;
                }, new ThreadPoolExecutor.AbortPolicy());
        return executor;
    }

    public static void shutdown() {
        if (executor != null) executor.shutdownNow();
        executor = null;
        LOAD_THROTTLE.clear();
    }

    public static boolean acquireLoad(ServerPlayer player) {
        return LOAD_THROTTLE.acquire(player.getUUID(), System.nanoTime(), TimeUnit.SECONDS.toNanos(2));
    }

    public static void requestLoad(ServerLevel level, BlockPos pos, ServerPlayer player, String url, String title) {
        if (!ModNetworking.canUsePrinter(player, pos)) { tell(player, INVALID_MENU); return; }
        PrinterBlockEntity printer = (PrinterBlockEntity) level.getBlockEntity(pos);
        if (printer.isPrinting()) { tell(player, BUSY); return; }
        if (url == null || url.length() > 2048 || title == null || title.length() > 64) {
            tell(player, INVALID_URL); return;
        }
        if (!acquireLoad(player)) { tell(player, RATE_LIMIT); return; }
        long job = printer.beginJob("gui.printer.status.loading");
        submitLoad(level, printer, job, player, title, () -> ImageProcessor.downloadCanonical(url), ignored -> {});
    }

    public static void requestUploaded(ServerLevel level, PrinterBlockEntity printer, long job,
                                       ServerPlayer player, String title, byte[] bytes,
                                       Consumer<ImageFailure.Reason> completion) {
        if (!printer.isCurrentJob(job)) { completion.accept(CANCELLED); return; }
        printer.setJobState(true, "gui.printer.status.loading");
        submitLoad(level, printer, job, player, title, () -> ImageProcessor.canonicalize(bytes), completion);
    }

    private static void submitLoad(ServerLevel level, PrinterBlockEntity printer, long job,
                                   ServerPlayer player, String title, Callable<ProcessedImage> task,
                                   Consumer<ImageFailure.Reason> completion) {
        submit(level, printer, job, task, (source, error) -> {
            if (!isCurrent(level, printer, job)) { completion.accept(CANCELLED); return; }
            if (!ModNetworking.canUsePrinter(player, printer.getBlockPos())) {
                fail(printer, player, INVALID_MENU); completion.accept(INVALID_MENU); return;
            }
            if (error != null) {
                var reason = ImageFailure.classify(error);
                fail(printer, player, reason); completion.accept(reason); return;
            }
            if (!ImageStore.putSource(level.getServer(), source)) {
                fail(printer, player, STORAGE_FULL); completion.accept(STORAGE_FULL); return;
            }
            int maximumAutoEdge = Math.min(Config.SERVER.autoSizeMaxBlocks.get(), Config.SERVER.maxPlacementBlocks.get());
            ImageSizing.Size size = ImageSizing.automatic(source.width(), source.height(), maximumAutoEdge);
            printer.setPreset(new PrinterPreset(source.contentId(), title, source.width(), source.height(),
                    size.width(), size.height(), PrintFrame.OAK, source.originalWidth(), source.originalHeight()));
            if (printer.getOwner() == null) printer.setOwner(player.getUUID());
            printer.setJobState(false, "gui.printer.status.ready");
            ModCriteria.ACTION.get().trigger(player, "load");
            player.sendSystemMessage(Component.translatable("message.printer.image_saved",
                    source.originalWidth(), source.originalHeight(), size.width(), size.height()));
            completion.accept(null);
        });
    }

    public static boolean isCurrent(ServerLevel level, PrinterBlockEntity printer, long job) {
        return printer.isCurrentJob(job) && level.hasChunkAt(printer.getBlockPos())
                && level.getBlockEntity(printer.getBlockPos()) == printer;
    }

    public static void requestResize(ServerLevel level, BlockPos pos, int change) {
        if (!(level.getBlockEntity(pos) instanceof PrinterBlockEntity printer) || printer.isPrinting()) return;
        PrinterPreset preset = printer.getPreset().orElse(null);
        if (preset == null || (change != -1 && change != 1)) return;
        int edge = Math.clamp(Math.max(preset.blocksWide(), preset.blocksHigh()) + change, 1, Config.SERVER.maxPlacementBlocks.get());
        ImageSizing.Size size = ImageSizing.forLongEdge(preset.sourceWidth(), preset.sourceHeight(), edge);
        printer.setPreset(new PrinterPreset(preset.sourceId(), preset.title(), preset.sourceWidth(), preset.sourceHeight(),
                size.width(), size.height(), preset.frame(), preset.originalWidth(), preset.originalHeight()));
    }

    public static void requestFrame(ServerLevel level, BlockPos pos, int change) {
        if (!(level.getBlockEntity(pos) instanceof PrinterBlockEntity printer) || printer.isPrinting()) return;
        PrinterPreset preset = printer.getPreset().orElse(null);
        if (preset == null || (change != -1 && change != 1)) return;
        printer.setPreset(new PrinterPreset(preset.sourceId(), preset.title(), preset.sourceWidth(), preset.sourceHeight(),
                preset.blocksWide(), preset.blocksHigh(), preset.frame().next(change), preset.originalWidth(), preset.originalHeight()));
    }

    public static void requestPrint(ServerLevel level, BlockPos pos, ServerPlayer player) {
        if (player != null && !ModNetworking.canUsePrinter(player, pos)) { tell(player, INVALID_MENU); return; }
        if (!(level.getBlockEntity(pos) instanceof PrinterBlockEntity printer) || printer.isPrinting()) return;
        PrinterPreset preset = printer.getPreset().orElse(null);
        if (preset == null) { fail(printer, player, NO_IMAGE); return; }
        if (!printer.hasPrintingSupplies()) { fail(printer, player, NO_SUPPLIES); return; }
        byte[] source = ImageStore.getSource(level.getServer(), preset.sourceId());
        if (source == null) { fail(printer, player, MISSING_DATA); return; }
        PrintMode mode = printer.isMonochromeSupply() ? PrintMode.MONOCHROME : PrintMode.COLOR;
        boolean automated = player == null && printer.hasAutomatedSupplies();
        ImageSizing.PixelSize size = ImageSizing.textureSize(preset.sourceWidth(), preset.sourceHeight(),
                preset.blocksWide(), preset.blocksHigh());
        long job = printer.beginJob("gui.printer.status.printing");
        submit(level, printer, job, () -> ImageProcessor.createVariant(source, size.width(), size.height(), mode), (variant, error) -> {
            if (!isCurrent(level, printer, job)) return;
            if (error != null) { fail(printer, player, ImageFailure.classify(error)); return; }
            if (!printer.matchesPrint(preset, mode)) { fail(printer, player, SUPPLIES_CHANGED); return; }
            if (!ImageStore.putVariant(level.getServer(), variant)) { fail(printer, player, STORAGE_FULL); return; }
            ItemStack output = new ItemStack(ModItems.IMAGE.get());
            output.set(ModDataComponents.IMAGE_REFERENCE.get(), new ImageReference(variant.contentId(), variant.width(),
                    variant.height(), preset.blocksWide(), preset.blocksHigh(), preset.title(), mode, preset.frame(),
                    preset.originalWidth(), preset.originalHeight()));
            printer.consumeSupplies();
            printer.setOutput(output);
            printer.setJobState(false, "gui.printer.status.complete");
            if (automated) printer.completeAutomatedPrint();
            if (player != null && !player.hasDisconnected()) {
                ModCriteria.ACTION.get().trigger(player, "print");
                if (mode == PrintMode.COLOR) ModCriteria.ACTION.get().trigger(player, "color");
                player.sendSystemMessage(Component.translatable("message.printer.print_complete"));
            }
        });
    }

    private static void submit(ServerLevel level, PrinterBlockEntity printer, long job, Callable<ProcessedImage> task,
                               java.util.function.BiConsumer<ProcessedImage, Throwable> completion) {
        try {
            CompletableFuture.supplyAsync(() -> {
                try { return task.call(); }
                catch (Exception error) { throw new CompletionException(error); }
            }, executor()).orTimeout(Config.SERVER.fetchTimeoutSeconds.get() + 30L, TimeUnit.SECONDS)
                    .whenComplete((image, error) -> {
                        if (!level.getServer().isStopped()) level.getServer().execute(() -> completion.accept(image, error));
                    });
        } catch (RejectedExecutionException error) {
            completion.accept(null, new ImageFailure(BUSY));
        }
    }

    public static void tell(ServerPlayer player, ImageFailure.Reason reason) {
        if (player != null && !player.hasDisconnected()) player.sendSystemMessage(Component.translatable(reason.key()));
    }

    private static void fail(PrinterBlockEntity printer, ServerPlayer player, ImageFailure.Reason reason) {
        printer.cancelJob(reason.key());
        tell(player, reason);
        // Never log the URL, server response text, arbitrary filenames, or raw exception chains.
        Printer.LOGGER.debug("Printer job ended: {}", reason);
    }
}
