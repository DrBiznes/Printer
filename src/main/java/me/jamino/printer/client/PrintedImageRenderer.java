package me.jamino.printer.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import me.jamino.printer.data.ImageReference;
import me.jamino.printer.entity.PrintedImageEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

public final class PrintedImageRenderer extends EntityRenderer<PrintedImageEntity> {
    private static final ResourceLocation PLACEHOLDER = me.jamino.printer.Printer.id("textures/item/image.png");

    public PrintedImageRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(PrintedImageEntity entity, float yaw, float partialTick, PoseStack poseStack,
                       MultiBufferSource buffers, int packedLight) {
        ImageReference reference = entity.getReference();
        ResourceLocation texture = ClientImageCache.getOrRequest(reference.contentId());
        if (texture == null) texture = PLACEHOLDER;
        float canvasWidth = entity.blocksWide();
        float canvasHeight = entity.blocksHigh();
        float imageWidth = canvasWidth;
        float imageHeight = canvasHeight;
        float imageAspect = reference.width() / (float) reference.height();
        float canvasAspect = canvasWidth / canvasHeight;
        if (imageAspect > canvasAspect) imageHeight = imageWidth / imageAspect;
        else imageWidth = imageHeight * imageAspect;

        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - yaw));
        // The painting transform presents the negative-Z side. Keep the surface just in
        // front of the wall and disable culling because this custom quad has no back mesh.
        VertexConsumer backing = buffers.getBuffer(RenderType.entityCutoutNoCull(PLACEHOLDER));
        quad(backing, poseStack.last(), -canvasWidth / 2, -canvasHeight / 2, canvasWidth / 2, canvasHeight / 2,
                -0.032F, packedLight, 0, 0, 1, 1);
        VertexConsumer image = buffers.getBuffer(RenderType.entityCutoutNoCull(texture));
        quad(image, poseStack.last(), -imageWidth / 2, -imageHeight / 2, imageWidth / 2, imageHeight / 2,
                -0.035F, packedLight, 0, 1, 1, 0);
        poseStack.popPose();
        super.render(entity, yaw, partialTick, poseStack, buffers, packedLight);
    }

    private static void quad(VertexConsumer consumer, PoseStack.Pose pose, float x0, float y0, float x1, float y1,
                             float z, int light, float u0, float v0, float u1, float v1) {
        vertex(consumer, pose, x0, y0, z, u0, v0, light);
        vertex(consumer, pose, x1, y0, z, u1, v0, light);
        vertex(consumer, pose, x1, y1, z, u1, v1, light);
        vertex(consumer, pose, x0, y1, z, u0, v1, light);
    }

    private static void vertex(VertexConsumer consumer, PoseStack.Pose pose, float x, float y, float z,
                               float u, float v, int light) {
        consumer.addVertex(pose, x, y, z).setColor(-1).setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, 0, 0, -1);
    }

    @Override
    public ResourceLocation getTextureLocation(PrintedImageEntity entity) {
        ResourceLocation texture = ClientImageCache.getOrRequest(entity.getReference().contentId());
        return texture == null ? PLACEHOLDER : texture;
    }
}
