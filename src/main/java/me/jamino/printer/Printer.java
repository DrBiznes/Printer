package me.jamino.printer;

import com.mojang.logging.LogUtils;
import me.jamino.printer.block.entity.PrinterBlockEntity;
import me.jamino.printer.client.ClientImageCache;
import me.jamino.printer.client.PrinterClient;
import me.jamino.printer.registry.ModBlockEntities;
import me.jamino.printer.registry.ModBlocks;
import me.jamino.printer.registry.ModCreativeTabs;
import me.jamino.printer.registry.ModDataComponents;
import me.jamino.printer.registry.ModEntities;
import me.jamino.printer.registry.ModItems;
import me.jamino.printer.registry.ModMenus;
import me.jamino.printer.network.ModNetworking;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import org.slf4j.Logger;

@Mod(Printer.MODID)
public final class Printer {
    public static final String MODID = "printer";
    public static final Logger LOGGER = LogUtils.getLogger();

    public Printer(IEventBus modBus, ModContainer container) {
        ModBlocks.register(modBus);
        ModItems.register(modBus);
        ModCreativeTabs.register(modBus);
        ModBlockEntities.register(modBus);
        ModMenus.register(modBus);
        ModDataComponents.register(modBus);
        ModEntities.register(modBus);

        modBus.addListener(this::registerCapabilities);
        modBus.addListener(ModNetworking::register);
        container.registerConfig(ModConfig.Type.SERVER, Config.SERVER_SPEC);
        container.registerConfig(ModConfig.Type.CLIENT, Config.CLIENT_SPEC);
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MODID, path);
    }

    private void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                ModBlockEntities.PRINTER.get(),
                (PrinterBlockEntity blockEntity, net.minecraft.core.Direction side) -> blockEntity.getAutomationHandler(side));
    }

    @EventBusSubscriber(modid = MODID, value = Dist.CLIENT)
    public static final class ClientEvents {
        @SubscribeEvent
        public static void registerScreens(net.neoforged.neoforge.client.event.RegisterMenuScreensEvent event) {
            PrinterClient.registerScreens(event);
        }

        @SubscribeEvent
        public static void registerClientExtensions(
                net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent event) {
            PrinterClient.registerExtensions(event);
        }

        @SubscribeEvent
        public static void registerEntityRenderers(
                net.neoforged.neoforge.client.event.EntityRenderersEvent.RegisterRenderers event) {
            PrinterClient.registerEntityRenderers(event);
        }

        @SubscribeEvent
        public static void clearImages(net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent.LoggingOut event) {
            ClientImageCache.clear();
        }
    }
}
