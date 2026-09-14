package me.jamino.printer.client;

import me.jamino.printer.registry.ModMenus;
import me.jamino.printer.item.PrinterTooltips;
import net.minecraft.client.gui.screens.Screen;
import me.jamino.printer.registry.ModItems;
import me.jamino.printer.network.ModNetworking;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import me.jamino.printer.registry.ModEntities;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

public final class PrinterClient {
    private PrinterClient() {}

    public static void registerScreens(RegisterMenuScreensEvent event) {
        ModNetworking.CLIENT_IMAGE_CHUNK_HANDLER = ClientImageCache::accept;
        ModNetworking.CLIENT_UPLOAD_REPLY_HANDLER = payload -> {
            if (net.minecraft.client.Minecraft.getInstance().screen instanceof PrinterScreen screen) screen.acceptUpload(payload);
        };
        event.register(ModMenus.PRINTER.get(), PrinterScreen::new);
        event.register(ModMenus.PHOTOBOOK.get(), PhotobookScreen::new);
    }

    public static void registerExtensions(RegisterClientExtensionsEvent event) {
        PrinterTooltips.setShiftDownSupplier(Screen::hasShiftDown);
        event.registerItem(new IClientItemExtensions() {
            private final ImageItemRenderer renderer = new ImageItemRenderer();
            @Override public BlockEntityWithoutLevelRenderer getCustomRenderer() { return renderer; }
        }, ModItems.IMAGE.get());
    }

    public static void registerEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.PRINTED_IMAGE.get(), PrintedImageRenderer::new);
    }
}
