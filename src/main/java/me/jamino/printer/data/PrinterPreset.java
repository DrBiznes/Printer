package me.jamino.printer.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public record PrinterPreset(String sourceId, String title, int width, int height) {
    public static final Codec<PrinterPreset> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("source_id").forGetter(PrinterPreset::sourceId),
            Codec.STRING.optionalFieldOf("title", "").forGetter(PrinterPreset::title),
            Codec.INT.fieldOf("width").forGetter(PrinterPreset::width),
            Codec.INT.fieldOf("height").forGetter(PrinterPreset::height)
    ).apply(instance, PrinterPreset::new));

    public static final StreamCodec<FriendlyByteBuf, PrinterPreset> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, PrinterPreset::sourceId,
            ByteBufCodecs.STRING_UTF8, PrinterPreset::title,
            ByteBufCodecs.VAR_INT, PrinterPreset::width,
            ByteBufCodecs.VAR_INT, PrinterPreset::height,
            PrinterPreset::new);

    public PrinterPreset {
        title = title == null ? "" : title.substring(0, Math.min(64, title.length()));
    }
}
