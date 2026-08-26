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
            float aspect = reference.width() / (float) reference.height();
            if (aspect > 1.0F) height /= aspect; else width *= aspect;
        }
        float x0 = (1.0F - width) * 0.5F;
        float y0 = (1.0F - height) * 0.5F;
        float x1 = x0 + width;
        float y1 = y0 + height;

        poseStack.pushPose();
        poseStack.translate(0.0F, 0.0F, 0.5F);
        // Dynamic map textures use the text vertex format in vanilla. It is a flat,
        // two-sided lightmapped surface and does not expect overlay or normal elements.
        VertexConsumer consumer = buffers.getBuffer(RenderType.text(texture));
        PoseStack.Pose pose = poseStack.last();
        vertex(consumer, pose, x0, y0, 0.001F, 0, 1, packedLight);
        vertex(consumer, pose, x1, y0, 0.001F, 1, 1, packedLight);
        vertex(consumer, pose, x1, y1, 0.001F, 1, 0, packedLight);
        vertex(consumer, pose, x0, y1, 0.001F, 0, 0, packedLight);
        poseStack.popPose();
    }

    private static void vertex(VertexConsumer consumer, PoseStack.Pose pose, float x, float y, float z,
                               float u, float v, int light) {
        consumer.addVertex(pose.pose(), x, y, z).setColor(-1).setUv(u, v).setLight(light);
    }
}
