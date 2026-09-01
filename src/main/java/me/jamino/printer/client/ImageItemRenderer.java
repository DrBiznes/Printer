package me.jamino.printer.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import me.jamino.printer.data.ImageReference;
import me.jamino.printer.registry.ModDataComponents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

public final class ImageItemRenderer extends BlockEntityWithoutLevelRenderer {
    private static final ResourceLocation PLACEHOLDER = me.jamino.printer.Printer.id("textures/item/image.png");
    private static final ResourceLocation CANVAS = ResourceLocation.withDefaultNamespace(
            "textures/block/white_concrete.png");

    public ImageItemRenderer() {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext context, PoseStack poseStack,
                             MultiBufferSource buffers, int packedLight, int packedOverlay) {
        ImageReference reference = stack.get(ModDataComponents.IMAGE_REFERENCE.get());
        ResourceLocation texture = reference == null ? PLACEHOLDER : ClientImageCache.getOrRequest(reference.contentId());
        if (texture == null) texture = PLACEHOLDER;

        float width = 1.0F;
        float height = 1.0F;
        if (reference != null) {
            float aspect = reference.pixelWidth() / (float) reference.pixelHeight();
            if (aspect > 1.0F) height /= aspect; else width *= aspect;
        }
        float x0 = (1.0F - width) * 0.5F;
        float y0 = (1.0F - height) * 0.5F;
        float x1 = x0 + width;
        float y1 = y0 + height;

        poseStack.pushPose();
        poseStack.translate(0.0F, 0.0F, 0.5F);
        // GUI buffers use the entity format; held/fixed auxiliary layers keep the
        // flat text format used by vanilla maps. The image itself uses our smooth type.
        boolean inventory = context == ItemDisplayContext.GUI;
        PoseStack.Pose pose = poseStack.last();
        boolean framed = reference != null && reference.frame().isPresent();
        float border = framed ? Math.min(1.0F / 16.0F, Math.min(width, height) * 0.2F) : 0.0F;
        if (framed) {
            drawQuad(buffers, CANVAS, pose, x0, y0, x1, y1, 0.0F,
                    packedLight, packedOverlay, inventory);
            drawQuad(buffers, reference.frame().texture(), pose, x0, y0, x1, y0 + border, 0.003F,
                    packedLight, packedOverlay, inventory);
            drawQuad(buffers, reference.frame().texture(), pose, x0, y1 - border, x1, y1, 0.003F,
                    packedLight, packedOverlay, inventory);
            drawQuad(buffers, reference.frame().texture(), pose, x0, y0, x0 + border, y1, 0.003F,
                    packedLight, packedOverlay, inventory);
            drawQuad(buffers, reference.frame().texture(), pose, x1 - border, y0, x1, y1, 0.003F,
                    packedLight, packedOverlay, inventory);
        }
        VertexConsumer image = buffers.getBuffer(PrinterRenderTypes.smoothImage(texture));
        quad(image, pose, x0 + border, y0 + border, x1 - border, y1 - border, 0.001F,
                packedLight, packedOverlay, true);
        poseStack.popPose();
    }

    private static void drawQuad(MultiBufferSource buffers, ResourceLocation texture, PoseStack.Pose pose,
                                 float x0, float y0, float x1, float y1, float z,
                                 int light, int overlay, boolean entityFormat) {
        VertexConsumer consumer = buffers.getBuffer(entityFormat
                ? RenderType.entityCutoutNoCull(texture)
                : RenderType.text(texture));
        quad(consumer, pose, x0, y0, x1, y1, z, light, overlay, entityFormat);
    }

    private static void quad(VertexConsumer consumer, PoseStack.Pose pose,
                             float x0, float y0, float x1, float y1, float z,
                             int light, int overlay, boolean entityFormat) {
        vertex(consumer, pose, x0, y0, z, 0, 1, light, overlay, entityFormat);
        vertex(consumer, pose, x1, y0, z, 1, 1, light, overlay, entityFormat);
        vertex(consumer, pose, x1, y1, z, 1, 0, light, overlay, entityFormat);
        vertex(consumer, pose, x0, y1, z, 0, 0, light, overlay, entityFormat);
    }

    private static void vertex(VertexConsumer consumer, PoseStack.Pose pose, float x, float y, float z,
                               float u, float v, int light, int overlay, boolean entityFormat) {
        var vertex = consumer.addVertex(pose.pose(), x, y, z).setColor(-1).setUv(u, v);
        if (entityFormat) {
            vertex.setOverlay(overlay).setLight(light).setNormal(pose, 0, 0, 1);
        } else {
            vertex.setLight(light);
        }
    }
}
