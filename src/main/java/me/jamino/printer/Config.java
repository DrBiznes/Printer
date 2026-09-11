package me.jamino.printer;

import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.List;

public final class Config {
    private Config() {}

    private static final ModConfigSpec.Builder SERVER_BUILDER = new ModConfigSpec.Builder();
    public static final Server SERVER = new Server(SERVER_BUILDER);
    public static final ModConfigSpec SERVER_SPEC = SERVER_BUILDER.build();

    private static final ModConfigSpec.Builder CLIENT_BUILDER = new ModConfigSpec.Builder();
    public static final Client CLIENT = new Client(CLIENT_BUILDER);
    public static final ModConfigSpec CLIENT_SPEC = CLIENT_BUILDER.build();

    public static final class Server {
        public final ModConfigSpec.IntValue maxImageWidth;
        public final ModConfigSpec.IntValue maxImageHeight;
        public final ModConfigSpec.IntValue autoSizeMaxBlocks;
        public final ModConfigSpec.IntValue maxPlacementBlocks;
        public final ModConfigSpec.IntValue maxDownloadMiB;
        public final ModConfigSpec.IntValue maxStoredMiB;
        public final ModConfigSpec.IntValue fetchTimeoutSeconds;
        public final ModConfigSpec.BooleanValue allowLocalUploads;
        public final ModConfigSpec.ConfigValue<List<? extends String>> allowedHosts;
        public final ModConfigSpec.ConfigValue<List<? extends String>> blockedHosts;

        private Server(ModConfigSpec.Builder builder) {
            builder.push("images");
            maxImageWidth = builder.translation("config.printer.maxWidth").defineInRange("maxWidth", 1024, 1, 4096);
            maxImageHeight = builder.translation("config.printer.maxHeight").defineInRange("maxHeight", 1024, 1, 4096);
            autoSizeMaxBlocks = builder.comment("Maximum long edge selected automatically after loading an image.")
                    .translation("config.printer.autoSizeMaxBlocks").defineInRange("autoSizeMaxBlocks", 4, 1, 8);
            maxPlacementBlocks = builder.comment("Maximum long edge selectable with the printer size controls.")
                    .translation("config.printer.maxPlacementBlocks").defineInRange("maxPlacementBlocks", 8, 1, 8);
            maxDownloadMiB = builder.translation("config.printer.maxDownloadMiB").defineInRange("maxDownloadMiB", 10, 1, 64);
            maxStoredMiB = builder.translation("config.printer.maxStoredMiB").defineInRange("maxStoredMiB", 256, 16, 4096);
            fetchTimeoutSeconds = builder.translation("config.printer.fetchTimeoutSeconds").defineInRange("fetchTimeoutSeconds", 30, 5, 120);
            allowLocalUploads = builder.comment("Allow players to upload images through an open printer menu.")
                    .translation("config.printer.allowLocalUploads").define("allowLocalUploads", true);
            allowedHosts = builder.comment("Empty allows every public HTTP(S) host.")
                    .translation("config.printer.allowedHosts").defineListAllowEmpty("allowedHosts", List.of(), value -> value instanceof String);
            blockedHosts = builder.translation("config.printer.blockedHosts").defineListAllowEmpty("blockedHosts", List.of(), value -> value instanceof String);
            builder.pop();
        }
    }

    public static final class Client {
        public final ModConfigSpec.IntValue textureCacheMiB;

        private Client(ModConfigSpec.Builder builder) {
            textureCacheMiB = builder.translation("config.printer.textureCacheMiB").defineInRange("textureCacheMiB", 128, 16, 1024);
        }
    }
}
