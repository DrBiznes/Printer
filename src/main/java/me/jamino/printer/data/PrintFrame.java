package me.jamino.printer.data;

import com.mojang.serialization.Codec;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.StringRepresentable;

import java.util.Arrays;

/**
 * Frame material selected when a print is configured. Texture paths point at
 * vanilla block textures so resource packs automatically restyle the frame.
 */
public enum PrintFrame implements StringRepresentable {
    NONE("none", null),
    WHITE("white", "white_concrete"),
    OAK("oak", "oak_planks"),
    SPRUCE("spruce", "spruce_planks"),
    DARK_OAK("dark_oak", "dark_oak_planks"),
    IRON("iron", "iron_block"),
    GOLD("gold", "gold_block"),
    COPPER("copper", "copper_block");

    public static final Codec<PrintFrame> CODEC = StringRepresentable.fromEnum(PrintFrame::values);
    public static final StreamCodec<FriendlyByteBuf, PrintFrame> STREAM_CODEC = StreamCodec.of(
            (buffer, value) -> buffer.writeEnum(value), buffer -> buffer.readEnum(PrintFrame.class));

    private final String serializedName;
    private final ResourceLocation texture;

    PrintFrame(String serializedName, String blockTexture) {
        this.serializedName = serializedName;
        this.texture = blockTexture == null ? null : ResourceLocation.withDefaultNamespace(
                "textures/block/" + blockTexture + ".png");
    }

    @Override
    public String getSerializedName() {
        return serializedName;
    }

    public ResourceLocation texture() {
        return texture;
    }

    public boolean isPresent() {
        return this != NONE;
    }

    public PrintFrame next(int change) {
        PrintFrame[] values = values();
        return values[Math.floorMod(ordinal() + Integer.signum(change), values.length)];
    }

    public static PrintFrame byName(String name) {
        if (name == null) return NONE;
        return Arrays.stream(values()).filter(frame -> frame.serializedName.equals(name)).findFirst().orElse(NONE);
    }
}
