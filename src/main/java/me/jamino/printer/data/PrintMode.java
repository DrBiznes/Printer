package me.jamino.printer.data;

import com.mojang.serialization.Codec;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.StringRepresentable;

public enum PrintMode implements StringRepresentable {
    COLOR("color"),
    MONOCHROME("monochrome");

    public static final Codec<PrintMode> CODEC = StringRepresentable.fromEnum(PrintMode::values);
    public static final StreamCodec<FriendlyByteBuf, PrintMode> STREAM_CODEC = StreamCodec.of(
            (buffer, value) -> buffer.writeEnum(value), buffer -> buffer.readEnum(PrintMode.class));

    private final String serializedName;

    PrintMode(String serializedName) {
        this.serializedName = serializedName;
    }

    @Override
    public String getSerializedName() {
        return serializedName;
    }
}
