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
    private static final int CHUNK_SIZE = 256 * 1024;
    private ModNetworking() {}

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("3");
        registrar.playToServer(LoadImagePayload.TYPE, LoadImagePayload.STREAM_CODEC, (payload, context) ->
                context.enqueueWork(() -> {
                    if (context.player() instanceof ServerPlayer player && player.containerMenu instanceof PrinterMenu menu
                            && menu.getPos().equals(payload.pos()) && player.level() instanceof ServerLevel level) {
                        PrinterJobService.requestLoad(level, payload.pos(), player, payload.url(), payload.title());
                    }
                }));
        registrar.playToServer(ResizePresetPayload.TYPE, ResizePresetPayload.STREAM_CODEC, (payload, context) ->
                context.enqueueWork(() -> {
                    if (context.player() instanceof ServerPlayer player && player.containerMenu instanceof PrinterMenu menu
                            && menu.getPos().equals(payload.pos()) && player.level() instanceof ServerLevel level) {
                        PrinterJobService.requestResize(level, payload.pos(), payload.change());
                    }
                }));
        registrar.playToServer(CycleFramePayload.TYPE, CycleFramePayload.STREAM_CODEC, (payload, context) ->
                context.enqueueWork(() -> {
                    if (context.player() instanceof ServerPlayer player && player.containerMenu instanceof PrinterMenu menu
                            && menu.getPos().equals(payload.pos()) && player.level() instanceof ServerLevel level) {
                        PrinterJobService.requestFrame(level, payload.pos(), payload.change());
                    }
                }));
        registrar.playToServer(PrintPayload.TYPE, PrintPayload.STREAM_CODEC, (payload, context) ->
                context.enqueueWork(() -> {
                    if (context.player() instanceof ServerPlayer player && player.containerMenu instanceof PrinterMenu menu
                            && menu.getPos().equals(payload.pos()) && player.level() instanceof ServerLevel level) {
                        PrinterJobService.requestPrint(level, payload.pos(), player);
                    }
                }));
        registrar.playToServer(RequestImagePayload.TYPE, RequestImagePayload.STREAM_CODEC, (payload, context) ->
                context.enqueueWork(() -> {
                    if (!(context.player() instanceof ServerPlayer player) || !payload.contentId().matches("[0-9a-f]{64}")) return;
                    byte[] png = me.jamino.printer.image.ImageStore.getVariant(player.getServer(), payload.contentId());
                    // A source preview is only available through the open printer.
                    if (png == null && player.containerMenu instanceof PrinterMenu menu
                            && menu.stillValid(player) && menu.getPrinter().getPreset()
                            .filter(preset -> preset.sourceId().equals(payload.contentId())).isPresent()) {
                        png = me.jamino.printer.image.ImageStore.getSource(player.getServer(), payload.contentId());
                    }
                    if (png == null) {
                        Printer.LOGGER.debug("No stored printer variant found for client request {}", payload.contentId());
                        return;
                    }
                    sendImage(player, payload.contentId(), png);
                }));
        registrar.playToClient(ImageChunkPayload.TYPE, ImageChunkPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> CLIENT_IMAGE_CHUNK_HANDLER.accept(payload)));
    }

    public static void sendLoad(BlockPos pos, String url, String title) {
        PacketDistributor.sendToServer(new LoadImagePayload(pos, url, title));
    }

    public static void sendResize(BlockPos pos, int change) {
        PacketDistributor.sendToServer(new ResizePresetPayload(pos, change));
    }

    public static void sendCycleFrame(BlockPos pos, int change) {
        PacketDistributor.sendToServer(new CycleFramePayload(pos, change));
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

    public record CycleFramePayload(BlockPos pos, int change) implements CustomPacketPayload {
        public static final Type<CycleFramePayload> TYPE = new Type<>(Printer.id("cycle_frame"));
        public static final StreamCodec<FriendlyByteBuf, CycleFramePayload> STREAM_CODEC = StreamCodec.composite(
                BlockPos.STREAM_CODEC, CycleFramePayload::pos,
                ByteBufCodecs.VAR_INT, CycleFramePayload::change,
                CycleFramePayload::new);
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
}
