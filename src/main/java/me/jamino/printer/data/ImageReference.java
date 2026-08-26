package me.jamino.printer.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public record ImageReference(String contentId, int width, int height, String title, PrintMode mode) {
    public static final Codec<ImageReference> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("content_id").forGetter(ImageReference::contentId),
            Codec.INT.fieldOf("width").forGetter(ImageReference::width),
            Codec.INT.fieldOf("height").forGetter(ImageReference::height),
            Codec.STRING.optionalFieldOf("title", "").forGetter(ImageReference::title),
            PrintMode.CODEC.fieldOf("mode").forGetter(ImageReference::mode)
    ).apply(instance, ImageReference::new));

    public static final StreamCodec<FriendlyByteBuf, ImageReference> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, ImageReference::contentId,
            ByteBufCodecs.VAR_INT, ImageReference::width,
            ByteBufCodecs.VAR_INT, ImageReference::height,
            ByteBufCodecs.STRING_UTF8, ImageReference::title,
            PrintMode.STREAM_CODEC, ImageReference::mode,
            ImageReference::new);

    public ImageReference {
        title = title == null ? "" : title.substring(0, Math.min(64, title.length()));
    }

    public int blocksWide() {
        return Math.max(1, (width + 127) / 128);
    }

    public int blocksHigh() {
        return Math.max(1, (height + 127) / 128);
    }
}
