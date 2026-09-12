package me.jamino.printer.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import me.jamino.printer.Printer;
import me.jamino.printer.data.ImageReference;
import me.jamino.printer.entity.PrintedImageEntity;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

import java.util.function.IntBinaryOperator;

/**
 * Renders a print as a shallow, borderless canvas. Large
 * surfaces are split into block-sized quads so each part receives local world
 * lighting and the image keeps stable UVs at every supported size.
 */
public final class PrintedImageRenderer extends EntityRenderer<PrintedImageEntity> {
    private static final ResourceLocation PLACEHOLDER = Printer.id("textures/item/image.png");
    private static final ResourceLocation CANVAS = ResourceLocation.withDefaultNamespace(
            "textures/misc/white.png");

    private static final float CANVAS_FRONT = -0.020F;
    private static final float CANVAS_BACK = 0.0425F;
    private static final float IMAGE_FRONT = -0.022F;

    public PrintedImageRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(PrintedImageEntity entity, float yaw, float partialTick, PoseStack poseStack,
                       MultiBufferSource buffers, int packedLight) {
        ImageReference reference = entity.getReference();
        ResourceLocation imageTexture = ClientImageCache.getOrRequest(reference.contentId());
        if (imageTexture == null) imageTexture = PLACEHOLDER;

        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - yaw));
        renderCanvas(entity, poseStack.last(), buffers);
        renderImage(entity, reference, imageTexture, poseStack.last(), buffers);
        poseStack.popPose();
        super.render(entity, yaw, partialTick, poseStack, buffers, packedLight);
    }

    private static void renderCanvas(PrintedImageEntity entity, PoseStack.Pose pose, MultiBufferSource buffers) {
        VertexConsumer consumer = buffers.getBuffer(RenderType.entityCutoutNoCull(CANVAS));
        renderCanvas(pose, consumer, entity.blocksWide(), entity.blocksHigh(),
                entity.getReference().backgroundColor(), (x, y) -> lightAt(entity, x, y));
    }

    static void renderCanvas(PoseStack.Pose pose, VertexConsumer consumer, int width, int height,
                             int backgroundColor, IntBinaryOperator lightAt) {
        int color = 0xFF000000 | (backgroundColor & 0xFFFFFF);
        float left = -width / 2.0F;
        float bottom = -height / 2.0F;

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                float x0 = left + x;
                float y0 = bottom + y;
                int light = lightAt.applyAsInt(x, y);
                frontQuad(consumer, pose, x0, y0, x0 + 1.0F, y0 + 1.0F, CANVAS_FRONT,
                        0.0F, 1.0F, 1.0F, 0.0F, light, 0.0F, 0.0F, -1.0F, color);
                frontQuad(consumer, pose, x0, y0, x0 + 1.0F, y0 + 1.0F, CANVAS_BACK,
                        0.0F, 1.0F, 1.0F, 0.0F, light, 0.0F, 0.0F, 1.0F, color);
            }
        }

        for (int x = 0; x < width; x++) {
            float x0 = left + x;
            int bottomLight = lightAt.applyAsInt(x, 0);
            int topLight = lightAt.applyAsInt(x, height - 1);
            horizontalDepthQuad(consumer, pose, x0, x0 + 1.0F, bottom, CANVAS_FRONT, CANVAS_BACK,
                    bottomLight, 0.0F, -1.0F, color);
            horizontalDepthQuad(consumer, pose, x0, x0 + 1.0F, bottom + height, CANVAS_BACK, CANVAS_FRONT,
                    topLight, 0.0F, 1.0F, color);
        }
        for (int y = 0; y < height; y++) {
            float y0 = bottom + y;
            int leftLight = lightAt.applyAsInt(0, y);
            int rightLight = lightAt.applyAsInt(width - 1, y);
            verticalDepthQuad(consumer, pose, left, y0, y0 + 1.0F, CANVAS_BACK, CANVAS_FRONT,
                    leftLight, -1.0F, 0.0F, color);
            verticalDepthQuad(consumer, pose, left + width, y0, y0 + 1.0F, CANVAS_FRONT, CANVAS_BACK,
                    rightLight, 1.0F, 0.0F, color);
        }
    }

    private static void renderImage(PrintedImageEntity entity, ImageReference reference, ResourceLocation texture,
                                    PoseStack.Pose pose, MultiBufferSource buffers) {
        int width = entity.blocksWide();
        int height = entity.blocksHigh();
        float left = -width / 2.0F;
        float bottom = -height / 2.0F;
        float availableWidth = width;
        float availableHeight = height;
        float imageWidth = availableWidth;
        float imageHeight = availableHeight;
        float imageAspect = reference.pixelWidth() / (float) reference.pixelHeight();
        float availableAspect = availableWidth / availableHeight;
        if (imageAspect > availableAspect) imageHeight = imageWidth / imageAspect;
        else imageWidth = imageHeight * imageAspect;

        float imageLeft = -imageWidth / 2.0F;
        float imageBottom = -imageHeight / 2.0F;
        float imageTop = imageHeight / 2.0F;
        VertexConsumer consumer = buffers.getBuffer(PrinterRenderTypes.smoothImage(texture));

        for (int y = 0; y < height; y++) {
            float cellBottom = bottom + y;
            float y0 = Math.max(cellBottom, imageBottom);
            float y1 = Math.min(cellBottom + 1.0F, imageTop);
            if (y0 >= y1) continue;
            for (int x = 0; x < width; x++) {
                float cellLeft = left + x;
                float x0 = Math.max(cellLeft, imageLeft);
                float x1 = Math.min(cellLeft + 1.0F, imageLeft + imageWidth);
                if (x0 >= x1) continue;
                // The visible face points along local -Z, so screen-right is local -X.
                float u0 = 1.0F - (x0 - imageLeft) / imageWidth;
                float u1 = 1.0F - (x1 - imageLeft) / imageWidth;
                float v0 = 1.0F - (y0 - imageBottom) / imageHeight;
                float v1 = 1.0F - (y1 - imageBottom) / imageHeight;
                frontQuad(consumer, pose, x0, y0, x1, y1, IMAGE_FRONT,
                        u0, v0, u1, v1, lightAt(entity, x, y), 0.0F, 0.0F, -1.0F, -1);
            }
        }
    }

    private static int lightAt(PrintedImageEntity entity, int tileX, int tileY) {
        int width = entity.blocksWide();
        int height = entity.blocksHigh();
        double localX = tileX + 0.5D - width / 2.0D;
        double localY = tileY + 0.5D - height / 2.0D;
        Direction facing = entity.getDirection();
        // The renderer's local +X follows the clockwise direction after its Y rotation.
        Direction right = facing.getClockWise();
        Vec3 sample = entity.getBoundingBox().getCenter()
                .add(right.getStepX() * localX, localY, right.getStepZ() * localX)
                .relative(facing, 0.5D);
        return LevelRenderer.getLightColor(entity.level(), BlockPos.containing(sample));
    }

    private static void frontQuad(VertexConsumer consumer, PoseStack.Pose pose,
                                  float x0, float y0, float x1, float y1, float z,
                                  float u0, float v0, float u1, float v1, int light,
                                  float normalX, float normalY, float normalZ, int color) {
        vertex(consumer, pose, x0, y0, z, u0, v0, light, normalX, normalY, normalZ, color);
        vertex(consumer, pose, x1, y0, z, u1, v0, light, normalX, normalY, normalZ, color);
        vertex(consumer, pose, x1, y1, z, u1, v1, light, normalX, normalY, normalZ, color);
        vertex(consumer, pose, x0, y1, z, u0, v1, light, normalX, normalY, normalZ, color);
    }

    private static void verticalDepthQuad(VertexConsumer consumer, PoseStack.Pose pose, float x,
                                          float y0, float y1, float z0, float z1, int light,
                                          float normalX, float normalY, int color) {
        vertex(consumer, pose, x, y0, z0, 0.0F, 1.0F, light, normalX, normalY, 0.0F, color);
        vertex(consumer, pose, x, y0, z1, 1.0F, 1.0F, light, normalX, normalY, 0.0F, color);
        vertex(consumer, pose, x, y1, z1, 1.0F, 0.0F, light, normalX, normalY, 0.0F, color);
        vertex(consumer, pose, x, y1, z0, 0.0F, 0.0F, light, normalX, normalY, 0.0F, color);
    }

    private static void horizontalDepthQuad(VertexConsumer consumer, PoseStack.Pose pose,
                                            float x0, float x1, float y, float z0, float z1, int light,
                                            float normalX, float normalY, int color) {
        vertex(consumer, pose, x0, y, z0, 0.0F, 1.0F, light, normalX, normalY, 0.0F, color);
        vertex(consumer, pose, x1, y, z0, 1.0F, 1.0F, light, normalX, normalY, 0.0F, color);
        vertex(consumer, pose, x1, y, z1, 1.0F, 0.0F, light, normalX, normalY, 0.0F, color);
        vertex(consumer, pose, x0, y, z1, 0.0F, 0.0F, light, normalX, normalY, 0.0F, color);
    }

    private static void vertex(VertexConsumer consumer, PoseStack.Pose pose, float x, float y, float z,
                               float u, float v, int light, float normalX, float normalY, float normalZ, int color) {
        consumer.addVertex(pose, x, y, z).setColor(color).setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY).setLight(light)
                .setNormal(pose, normalX, normalY, normalZ);
    }

    @Override
    public ResourceLocation getTextureLocation(PrintedImageEntity entity) {
        ResourceLocation texture = ClientImageCache.getOrRequest(entity.getReference().contentId());
        return texture == null ? PLACEHOLDER : texture;
    }
}
