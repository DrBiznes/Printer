package me.jamino.printer.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public record PrinterPreset(String sourceId, String title, int sourceWidth, int sourceHeight,
                            int blocksWide, int blocksHigh, int backgroundColor, int originalWidth, int originalHeight) {
    public static final Codec<PrinterPreset> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("source_id").forGetter(PrinterPreset::sourceId),
            Codec.STRING.optionalFieldOf("title", "").forGetter(PrinterPreset::title),
            Codec.INT.fieldOf("source_width").forGetter(PrinterPreset::sourceWidth),
            Codec.INT.fieldOf("source_height").forGetter(PrinterPreset::sourceHeight),
            Codec.INT.fieldOf("blocks_wide").forGetter(PrinterPreset::blocksWide),
            Codec.INT.fieldOf("blocks_high").forGetter(PrinterPreset::blocksHigh),
            Codec.intRange(0, 0xFFFFFF).fieldOf("background_color").forGetter(PrinterPreset::backgroundColor),
            Codec.INT.fieldOf("original_width").forGetter(PrinterPreset::originalWidth),
            Codec.INT.fieldOf("original_height").forGetter(PrinterPreset::originalHeight)
    ).apply(instance, PrinterPreset::new));
    public static final StreamCodec<FriendlyByteBuf, PrinterPreset> STREAM_CODEC = StreamCodec.of(
            (buffer, preset) -> {
                ByteBufCodecs.STRING_UTF8.encode(buffer, preset.sourceId());
                ByteBufCodecs.STRING_UTF8.encode(buffer, preset.title());
                ByteBufCodecs.VAR_INT.encode(buffer, preset.sourceWidth());
                ByteBufCodecs.VAR_INT.encode(buffer, preset.sourceHeight());
                ByteBufCodecs.VAR_INT.encode(buffer, preset.blocksWide());
                ByteBufCodecs.VAR_INT.encode(buffer, preset.blocksHigh());
                buffer.writeVarInt(preset.backgroundColor());
                buffer.writeVarInt(preset.originalWidth());
                buffer.writeVarInt(preset.originalHeight());
            }, buffer -> new PrinterPreset(
                    ByteBufCodecs.STRING_UTF8.decode(buffer), ByteBufCodecs.STRING_UTF8.decode(buffer),
                    ByteBufCodecs.VAR_INT.decode(buffer), ByteBufCodecs.VAR_INT.decode(buffer),
                    ByteBufCodecs.VAR_INT.decode(buffer), ByteBufCodecs.VAR_INT.decode(buffer),
                    buffer.readVarInt(), buffer.readVarInt(), buffer.readVarInt()));

    public PrinterPreset {
        originalWidth = Math.clamp(originalWidth, 1, 4096);
        originalHeight = Math.clamp(originalHeight, 1, 4096);
        title = title == null ? "" : title.substring(0, Math.min(64, title.length()));
        sourceWidth = Math.max(1, sourceWidth);
        sourceHeight = Math.max(1, sourceHeight);
        blocksWide = Math.clamp(blocksWide, 1, 8);
        blocksHigh = Math.clamp(blocksHigh, 1, 8);
        backgroundColor &= 0xFFFFFF;
    }

    public int requiredPaper() { return Math.multiplyExact(blocksWide, blocksHigh); }
}
