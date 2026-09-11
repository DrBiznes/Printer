package me.jamino.printer.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public record PrinterPreset(String sourceId, String title, int sourceWidth, int sourceHeight,
                            int blocksWide, int blocksHigh, PrintFrame frame, int originalWidth, int originalHeight) {
    public static final Codec<PrinterPreset> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("source_id").forGetter(PrinterPreset::sourceId),
            Codec.STRING.optionalFieldOf("title", "").forGetter(PrinterPreset::title),
            Codec.INT.fieldOf("source_width").forGetter(PrinterPreset::sourceWidth),
            Codec.INT.fieldOf("source_height").forGetter(PrinterPreset::sourceHeight),
            Codec.INT.fieldOf("blocks_wide").forGetter(PrinterPreset::blocksWide),
            Codec.INT.fieldOf("blocks_high").forGetter(PrinterPreset::blocksHigh),
            PrintFrame.CODEC.fieldOf("frame").forGetter(PrinterPreset::frame),
            Codec.INT.optionalFieldOf("original_width", 0).forGetter(PrinterPreset::originalWidth),
            Codec.INT.optionalFieldOf("original_height", 0).forGetter(PrinterPreset::originalHeight)
    ).apply(instance, PrinterPreset::new));
    public static final StreamCodec<FriendlyByteBuf, PrinterPreset> STREAM_CODEC = StreamCodec.of(
            (buffer, preset) -> {
                ByteBufCodecs.STRING_UTF8.encode(buffer, preset.sourceId());
                ByteBufCodecs.STRING_UTF8.encode(buffer, preset.title());
                ByteBufCodecs.VAR_INT.encode(buffer, preset.sourceWidth());
                ByteBufCodecs.VAR_INT.encode(buffer, preset.sourceHeight());
                ByteBufCodecs.VAR_INT.encode(buffer, preset.blocksWide());
                ByteBufCodecs.VAR_INT.encode(buffer, preset.blocksHigh());
                PrintFrame.STREAM_CODEC.encode(buffer, preset.frame());
                buffer.writeVarInt(preset.originalWidth());
                buffer.writeVarInt(preset.originalHeight());
            }, buffer -> new PrinterPreset(
                    ByteBufCodecs.STRING_UTF8.decode(buffer), ByteBufCodecs.STRING_UTF8.decode(buffer),
                    ByteBufCodecs.VAR_INT.decode(buffer), ByteBufCodecs.VAR_INT.decode(buffer),
                    ByteBufCodecs.VAR_INT.decode(buffer), ByteBufCodecs.VAR_INT.decode(buffer),
                    PrintFrame.STREAM_CODEC.decode(buffer), buffer.readVarInt(), buffer.readVarInt()));

    public PrinterPreset(String sourceId, String title, int sourceWidth, int sourceHeight,
                         int blocksWide, int blocksHigh, PrintFrame frame) {
        this(sourceId, title, sourceWidth, sourceHeight, blocksWide, blocksHigh, frame, 0, 0);
    }

    public PrinterPreset {
        originalWidth = Math.clamp(originalWidth, 0, 4096);
        originalHeight = Math.clamp(originalHeight, 0, 4096);
        title = title == null ? "" : title.substring(0, Math.min(64, title.length()));
        sourceWidth = Math.max(1, sourceWidth);
        sourceHeight = Math.max(1, sourceHeight);
        blocksWide = Math.clamp(blocksWide, 1, 8);
        blocksHigh = Math.clamp(blocksHigh, 1, 8);
        frame = frame == null ? PrintFrame.NONE : frame;
    }

    public int requiredPaper() { return Math.multiplyExact(blocksWide, blocksHigh); }
}
