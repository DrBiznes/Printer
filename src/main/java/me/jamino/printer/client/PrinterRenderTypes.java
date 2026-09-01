package me.jamino.printer.client;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.Util;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

import java.util.function.Function;

final class PrinterRenderTypes {
    private static final Function<ResourceLocation, RenderType> SMOOTH_IMAGE = Util.memoize(texture ->
            RenderType.create("printer_smooth_image", DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS,
                    1536, true, false, RenderType.CompositeState.builder()
                            .setShaderState(RenderStateShard.RENDERTYPE_ENTITY_CUTOUT_NO_CULL_SHADER)
                            .setTextureState(new RenderStateShard.TextureStateShard(texture, true, false))
                            .setTransparencyState(RenderStateShard.NO_TRANSPARENCY)
                            .setCullState(RenderStateShard.NO_CULL)
                            .setLightmapState(RenderStateShard.LIGHTMAP)
                            .setOverlayState(RenderStateShard.OVERLAY)
                            .createCompositeState(true)));

    private PrinterRenderTypes() {}

    static RenderType smoothImage(ResourceLocation texture) {
        return SMOOTH_IMAGE.apply(texture);
    }
}
