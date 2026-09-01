package me.jamino.printer.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public record ImageReference(String contentId, int pixelWidth, int pixelHeight, int blocksWide, int blocksHigh,
                             String title, PrintMode mode, PrintFrame frame) {
    public static final Codec<ImageReference> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("content_id").forGetter(ImageReference::contentId),
            Codec.INT.fieldOf("pixel_width").forGetter(ImageReference::pixelWidth),
            Codec.INT.fieldOf("pixel_height").forGetter(ImageReference::pixelHeight),
            Codec.INT.fieldOf("blocks_wide").forGetter(ImageReference::blocksWide),
            Codec.INT.fieldOf("blocks_high").forGetter(ImageReference::blocksHigh),
            Codec.STRING.optionalFieldOf("title", "").forGetter(ImageReference::title),
            PrintMode.CODEC.fieldOf("mode").forGetter(ImageReference::mode),
            PrintFrame.CODEC.fieldOf("frame").forGetter(ImageReference::frame)
    ).apply(instance, ImageReference::new));
    public static final StreamCodec<FriendlyByteBuf, ImageReference> STREAM_CODEC = StreamCodec.of(
            (buffer, reference) -> {
                ByteBufCodecs.STRING_UTF8.encode(buffer, reference.contentId());
                ByteBufCodecs.VAR_INT.encode(buffer, reference.pixelWidth());
                ByteBufCodecs.VAR_INT.encode(buffer, reference.pixelHeight());
                ByteBufCodecs.VAR_INT.encode(buffer, reference.blocksWide());
                ByteBufCodecs.VAR_INT.encode(buffer, reference.blocksHigh());
                ByteBufCodecs.STRING_UTF8.encode(buffer, reference.title());
                PrintMode.STREAM_CODEC.encode(buffer, reference.mode());
                PrintFrame.STREAM_CODEC.encode(buffer, reference.frame());
            }, buffer -> new ImageReference(
                    ByteBufCodecs.STRING_UTF8.decode(buffer), ByteBufCodecs.VAR_INT.decode(buffer),
                    ByteBufCodecs.VAR_INT.decode(buffer), ByteBufCodecs.VAR_INT.decode(buffer),
                    ByteBufCodecs.VAR_INT.decode(buffer), ByteBufCodecs.STRING_UTF8.decode(buffer),
                    PrintMode.STREAM_CODEC.decode(buffer), PrintFrame.STREAM_CODEC.decode(buffer)));

    public ImageReference {
        title = title == null ? "" : title.substring(0, Math.min(64, title.length()));
        pixelWidth = Math.max(1, pixelWidth);
        pixelHeight = Math.max(1, pixelHeight);
        blocksWide = Math.clamp(blocksWide, 1, 8);
        blocksHigh = Math.clamp(blocksHigh, 1, 8);
        frame = frame == null ? PrintFrame.NONE : frame;
    }
}
