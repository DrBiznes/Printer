package me.jamino.printer.network;

import me.jamino.printer.Config;
import me.jamino.printer.Printer;
import me.jamino.printer.block.entity.PrinterBlockEntity;
import me.jamino.printer.image.ImageFailure;
import me.jamino.printer.job.PrinterJobService;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.UUID;

import static me.jamino.printer.image.ImageFailure.Reason.*;

@EventBusSubscriber(modid = Printer.MODID)
public final class ServerUploads {
    private record Target(ServerLevel level, PrinterBlockEntity printer, long job, ServerPlayer player, String title) {}
    private static final UploadTransfers<Target> TRANSFERS = new UploadTransfers<>();
    private ServerUploads() {}

    public static void begin(ServerPlayer player, ModNetworking.BeginUploadPayload payload) {
        try {
            if (!Config.SERVER.allowLocalUploads.get()) throw new ImageFailure(UPLOAD_DISABLED);
            if (!ModNetworking.canUsePrinter(player, payload.pos())) throw new ImageFailure(INVALID_MENU);
            if (!PrinterJobService.acquireLoad(player)) throw new ImageFailure(RATE_LIMIT);
            var printer = (PrinterBlockEntity) player.serverLevel().getBlockEntity(payload.pos());
            if (printer.isPrinting()) throw new ImageFailure(BUSY);
            long job = printer.beginJob("gui.printer.status.uploading");
            try {
                TRANSFERS.begin(player.getUUID(), payload.id(), payload.size(), Config.SERVER.maxDownloadMiB.get() * 1024 * 1024,
                        new Target(player.serverLevel(), printer, job, player, payload.title()), System.nanoTime());
            } catch (ImageFailure failure) {
                printer.cancelJob(failure.reason().key());
                throw failure;
            }
            reply(player, payload.id(), 0, "gui.printer.status.uploading");
        } catch (ImageFailure failure) {
            reply(player, payload.id(), -1, failure.reason().key());
        }
    }

    public static void chunk(ServerPlayer player, ModNetworking.UploadChunkPayload payload) {
        var transfer = TRANSFERS.get(player.getUUID());
        if (transfer == null || !transfer.id().equals(payload.id())) {
            reply(player, payload.id(), -1, INVALID_TRANSFER.key()); return;
        }
        try {
            validate(transfer.target());
            transfer = TRANSFERS.accept(player.getUUID(), payload.id(), payload.index(), payload.bytes(), System.nanoTime());
            if (!transfer.complete()) {
                reply(player, payload.id(), transfer.nextIndex(), "gui.printer.status.uploading"); return;
            }
            Target target = transfer.target();
            var completedTransfer = transfer;
            reply(player, payload.id(), -2, "gui.printer.status.loading");
            PrinterJobService.requestUploaded(target.level, target.printer, target.job, player, target.title,
                    transfer.bytes(), reason -> {
                        if (TRANSFERS.get(player.getUUID()) == completedTransfer) {
                            TRANSFERS.remove(player.getUUID());
                            reply(player, payload.id(), reason == null ? -3 : -1,
                                    reason == null ? "message.printer.loaded" : reason.key());
                        }
                    });
        } catch (ImageFailure failure) {
            cancel(player.getUUID(), failure.reason());
        }
    }

    public static void cancel(ServerPlayer player, UUID id) {
        var transfer = TRANSFERS.get(player.getUUID());
        if (transfer != null && transfer.id().equals(id)) cancel(player.getUUID(), CANCELLED);
    }

    private static void validate(Target target) throws ImageFailure {
        if (!Config.SERVER.allowLocalUploads.get()) throw new ImageFailure(UPLOAD_DISABLED);
        if (!ModNetworking.canUsePrinter(target.player, target.printer.getBlockPos())
                || target.player.serverLevel() != target.level
                || !PrinterJobService.isCurrent(target.level, target.printer, target.job)) throw new ImageFailure(INVALID_MENU);
    }

    private static void cancel(UUID player, ImageFailure.Reason reason) {
        var transfer = TRANSFERS.remove(player);
        if (transfer == null) return;
        Target target = transfer.target();
        if (PrinterJobService.isCurrent(target.level, target.printer, target.job)) target.printer.cancelJob(reason.key());
        reply(target.player, transfer.id(), -1, reason.key());
    }

    public static void reply(ServerPlayer player, UUID id, int next, String status) {
        if (!player.hasDisconnected()) PacketDistributor.sendToPlayer(player, new ModNetworking.UploadReplyPayload(id, next, status));
    }

    @SubscribeEvent public static void tick(ServerTickEvent.Post event) {
        for (var entry : TRANSFERS.snapshot().entrySet()) {
            try {
                validate(entry.getValue().target());
                if (System.nanoTime() >= entry.getValue().deadline()) throw new ImageFailure(TIMEOUT);
            } catch (ImageFailure failure) { cancel(entry.getKey(), failure.reason()); }
        }
    }

    @SubscribeEvent public static void logout(PlayerEvent.PlayerLoggedOutEvent event) {
        cancel(event.getEntity().getUUID(), CANCELLED);
        PrinterJobService.LOAD_THROTTLE.remove(event.getEntity().getUUID());
        ModNetworking.clearPlayer(event.getEntity().getUUID());
    }

    @SubscribeEvent public static void stop(ServerStoppingEvent event) {
        for (UUID player : TRANSFERS.snapshot().keySet()) cancel(player, CANCELLED);
        TRANSFERS.clear();
        PrinterJobService.shutdown();
        ModNetworking.clearRequests();
    }
}
