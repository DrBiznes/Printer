package me.jamino.printer.network;

import me.jamino.printer.Printer;
import me.jamino.printer.inventory.PrinterMenu;
import me.jamino.printer.job.PrinterJobService;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

import java.util.Arrays;
import java.util.function.Consumer;

public final class ModNetworking {
    public static Consumer<ImageChunkPayload> CLIENT_IMAGE_CHUNK_HANDLER = payload -> {};
    public static Consumer<UploadReplyPayload> CLIENT_UPLOAD_REPLY_HANDLER = payload -> {};
    private static final ImageRequestBudget IMAGE_REQUESTS = new ImageRequestBudget();
    public static void clearPlayer(java.util.UUID player) { IMAGE_REQUESTS.remove(player); }
    public static void clearRequests() { IMAGE_REQUESTS.clear(); }
    public static boolean canUsePrinter(ServerPlayer player, BlockPos pos) {
        return !player.hasDisconnected() && player.isAlive() && !player.isSpectator()
                && player.containerMenu instanceof PrinterMenu menu && menu.getPrinter() != null
                && menu.getPrinter().getLevel() == player.level()
                && menu.getPos().equals(pos) && menu.stillValid(player);
    }
    private static final int CHUNK_SIZE = 256 * 1024;
    private ModNetworking() {}

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("0.1.0");
        registrar.playToServer(LoadImagePayload.TYPE, LoadImagePayload.STREAM_CODEC, (payload, context) ->
                context.enqueueWork(() -> {
                    if (context.player() instanceof ServerPlayer player && player.containerMenu instanceof PrinterMenu menu
                            && canUsePrinter(player, payload.pos()) && player.level() instanceof ServerLevel level) {
                        PrinterJobService.requestLoad(level, payload.pos(), player, payload.url(), payload.title());
                    }
                }));
        registrar.playToServer(ResizePresetPayload.TYPE, ResizePresetPayload.STREAM_CODEC, (payload, context) ->
                context.enqueueWork(() -> {
                    if (context.player() instanceof ServerPlayer player && player.containerMenu instanceof PrinterMenu menu
                            && canUsePrinter(player, payload.pos()) && player.level() instanceof ServerLevel level) {
                        PrinterJobService.requestResize(level, payload.pos(), payload.change());
                    }
                }));
        registrar.playToServer(SetBackgroundPayload.TYPE, SetBackgroundPayload.STREAM_CODEC, (payload, context) ->
                context.enqueueWork(() -> {
                    if (context.player() instanceof ServerPlayer player && player.containerMenu instanceof PrinterMenu menu
                            && canUsePrinter(player, payload.pos()) && player.level() instanceof ServerLevel level) {
                        PrinterJobService.requestBackground(level, payload.pos(), payload.color());
                    }
                }));
        registrar.playToServer(PrintPayload.TYPE, PrintPayload.STREAM_CODEC, (payload, context) ->
                context.enqueueWork(() -> {
                    if (context.player() instanceof ServerPlayer player && player.containerMenu instanceof PrinterMenu menu
                            && canUsePrinter(player, payload.pos()) && player.level() instanceof ServerLevel level) {
                        PrinterJobService.requestPrint(level, payload.pos(), player);
                    }
                }));
        registrar.playToServer(RequestImagePayload.TYPE, RequestImagePayload.STREAM_CODEC, (payload, context) ->
                context.enqueueWork(() -> {
                    if (!(context.player() instanceof ServerPlayer player) || !payload.contentId().matches("[0-9a-f]{64}")) return;
                    if (!IMAGE_REQUESTS.acquire(player.getUUID(), 0, System.nanoTime())) return;
                    byte[] png = me.jamino.printer.image.ImageStore.getVariant(player.getServer(), payload.contentId());
                    // A source preview is only available through the open printer.
                    if (png == null && player.containerMenu instanceof PrinterMenu menu
                            && menu.stillValid(player) && menu.getPrinter().getPreset()
                            .filter(preset -> preset.sourceId().equals(payload.contentId())).isPresent()) {
                        png = me.jamino.printer.image.ImageStore.getSource(player.getServer(), payload.contentId());
                    }
                    if (png == null) {
                        Printer.LOGGER.debug("No stored printer image found for client request");
                        return;
                    }
                    if (IMAGE_REQUESTS.acquire(player.getUUID(), png.length, System.nanoTime()))
                        sendImage(player, payload.contentId(), png);
                }));
        registrar.playToServer(BeginUploadPayload.TYPE, BeginUploadPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> {
                    if (context.player() instanceof ServerPlayer player) ServerUploads.begin(player, payload);
                }));
        registrar.playToServer(UploadChunkPayload.TYPE, UploadChunkPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> {
                    if (context.player() instanceof ServerPlayer player) ServerUploads.chunk(player, payload);
                }));
        registrar.playToServer(CancelUploadPayload.TYPE, CancelUploadPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> {
                    if (context.player() instanceof ServerPlayer player) ServerUploads.cancel(player, payload.id());
                }));
        registrar.playToClient(UploadReplyPayload.TYPE, UploadReplyPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> CLIENT_UPLOAD_REPLY_HANDLER.accept(payload)));
        registrar.playToClient(ImageChunkPayload.TYPE, ImageChunkPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> CLIENT_IMAGE_CHUNK_HANDLER.accept(payload)));
    }

    public static void sendLoad(BlockPos pos, String url, String title) {
        PacketDistributor.sendToServer(new LoadImagePayload(pos, url, title));
    }

    public static void sendResize(BlockPos pos, int change) {
        PacketDistributor.sendToServer(new ResizePresetPayload(pos, change));
    }

    public static void sendBackground(BlockPos pos, int color) {
        PacketDistributor.sendToServer(new SetBackgroundPayload(pos, color));
    }

    public static void sendPrint(BlockPos pos) {
        PacketDistributor.sendToServer(new PrintPayload(pos));
    }

    public static void requestImage(String contentId) {
        PacketDistributor.sendToServer(new RequestImagePayload(contentId));
    }

    public static void sendImage(ServerPlayer player, String contentId, byte[] png) {
        int total = Math.max(1, (png.length + CHUNK_SIZE - 1) / CHUNK_SIZE);
        for (int index = 0; index < total; index++) {
            int start = index * CHUNK_SIZE;
            byte[] chunk = Arrays.copyOfRange(png, start, Math.min(png.length, start + CHUNK_SIZE));
            PacketDistributor.sendToPlayer(player, new ImageChunkPayload(contentId, index, total, chunk));
        }
    }

    public record LoadImagePayload(BlockPos pos, String url, String title)
            implements CustomPacketPayload {
        public static final Type<LoadImagePayload> TYPE = new Type<>(Printer.id("load_image"));
        public static final StreamCodec<FriendlyByteBuf, LoadImagePayload> STREAM_CODEC = StreamCodec.composite(
                BlockPos.STREAM_CODEC, LoadImagePayload::pos,
                ByteBufCodecs.stringUtf8(2048), LoadImagePayload::url,
                ByteBufCodecs.stringUtf8(64), LoadImagePayload::title,
                LoadImagePayload::new);
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record ResizePresetPayload(BlockPos pos, int change) implements CustomPacketPayload {
        public static final Type<ResizePresetPayload> TYPE = new Type<>(Printer.id("resize_preset"));
        public static final StreamCodec<FriendlyByteBuf, ResizePresetPayload> STREAM_CODEC = StreamCodec.composite(
                BlockPos.STREAM_CODEC, ResizePresetPayload::pos,
                ByteBufCodecs.VAR_INT, ResizePresetPayload::change,
                ResizePresetPayload::new);
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record SetBackgroundPayload(BlockPos pos, int color) implements CustomPacketPayload {
        public static final Type<SetBackgroundPayload> TYPE = new Type<>(Printer.id("set_background"));
        public static final StreamCodec<FriendlyByteBuf, SetBackgroundPayload> STREAM_CODEC = StreamCodec.composite(
                BlockPos.STREAM_CODEC, SetBackgroundPayload::pos,
                ByteBufCodecs.VAR_INT, SetBackgroundPayload::color,
                SetBackgroundPayload::new);
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record PrintPayload(BlockPos pos) implements CustomPacketPayload {
        public static final Type<PrintPayload> TYPE = new Type<>(Printer.id("print"));
        public static final StreamCodec<FriendlyByteBuf, PrintPayload> STREAM_CODEC = StreamCodec.composite(
                BlockPos.STREAM_CODEC, PrintPayload::pos, PrintPayload::new);
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record RequestImagePayload(String contentId) implements CustomPacketPayload {
        public static final Type<RequestImagePayload> TYPE = new Type<>(Printer.id("request_image"));
        public static final StreamCodec<FriendlyByteBuf, RequestImagePayload> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.stringUtf8(64), RequestImagePayload::contentId, RequestImagePayload::new);
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record ImageChunkPayload(String contentId, int index, int total, byte[] bytes) implements CustomPacketPayload {
        public static final Type<ImageChunkPayload> TYPE = new Type<>(Printer.id("image_chunk"));
        private static final StreamCodec<FriendlyByteBuf, byte[]> BYTES = StreamCodec.of(
                (buffer, value) -> {
                    if (value.length > CHUNK_SIZE) throw new IllegalArgumentException("Image chunk is too large");
                    buffer.writeByteArray(value);
                }, buffer -> buffer.readByteArray(CHUNK_SIZE));
        public static final StreamCodec<FriendlyByteBuf, ImageChunkPayload> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.stringUtf8(64), ImageChunkPayload::contentId,
                ByteBufCodecs.VAR_INT, ImageChunkPayload::index,
                ByteBufCodecs.VAR_INT, ImageChunkPayload::total,
                BYTES, ImageChunkPayload::bytes,
                ImageChunkPayload::new);
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record BeginUploadPayload(BlockPos pos, java.util.UUID id, int size, String title) implements CustomPacketPayload {
        public static final Type<BeginUploadPayload> TYPE = new Type<>(Printer.id("begin_upload"));
        public static final StreamCodec<FriendlyByteBuf, BeginUploadPayload> STREAM_CODEC = StreamCodec.of(
                (b, p) -> { b.writeBlockPos(p.pos); b.writeUUID(p.id); b.writeVarInt(p.size); b.writeUtf(p.title, 64); },
                b -> new BeginUploadPayload(b.readBlockPos(), b.readUUID(), b.readVarInt(), b.readUtf(64)));
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record UploadChunkPayload(java.util.UUID id, int index, byte[] bytes) implements CustomPacketPayload {
        public static final Type<UploadChunkPayload> TYPE = new Type<>(Printer.id("upload_chunk"));
        public static final StreamCodec<FriendlyByteBuf, UploadChunkPayload> STREAM_CODEC = StreamCodec.of(
                (b, p) -> {
                    if (p.bytes.length > UploadTransfers.CHUNK_BYTES) throw new IllegalArgumentException("Upload chunk too large");
                    b.writeUUID(p.id); b.writeVarInt(p.index); b.writeByteArray(p.bytes);
                }, b -> new UploadChunkPayload(b.readUUID(), b.readVarInt(), b.readByteArray(UploadTransfers.CHUNK_BYTES)));
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record CancelUploadPayload(java.util.UUID id) implements CustomPacketPayload {
        public static final Type<CancelUploadPayload> TYPE = new Type<>(Printer.id("cancel_upload"));
        public static final StreamCodec<FriendlyByteBuf, CancelUploadPayload> STREAM_CODEC = StreamCodec.of(
                (b, p) -> b.writeUUID(p.id), b -> new CancelUploadPayload(b.readUUID()));
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    /** Nonnegative next chunk index; -1 error/cancel, -2 decoding, -3 complete. */
    public record UploadReplyPayload(java.util.UUID id, int next, String status) implements CustomPacketPayload {
        public static final Type<UploadReplyPayload> TYPE = new Type<>(Printer.id("upload_reply"));
        public static final StreamCodec<FriendlyByteBuf, UploadReplyPayload> STREAM_CODEC = StreamCodec.of(
                (b, p) -> { b.writeUUID(p.id); b.writeVarInt(p.next); b.writeUtf(p.status, 128); },
                b -> new UploadReplyPayload(b.readUUID(), b.readVarInt(), b.readUtf(128)));
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }
}
