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

    public boolean allowsBackground(int color) {
        return color >= 0 && color <= 0xFFFFFF
                && (this == COLOR || color == 0x000000 || color == 0xFFFFFF);
    }

    @Override
    public String getSerializedName() {
        return serializedName;
    }
}
