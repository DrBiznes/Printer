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
        public final ModConfigSpec.ConfigValue<List<? extends String>> allowedHosts;
        public final ModConfigSpec.ConfigValue<List<? extends String>> blockedHosts;

        private Server(ModConfigSpec.Builder builder) {
            builder.push("images");
            maxImageWidth = builder.defineInRange("maxWidth", 1024, 1, 4096);
            maxImageHeight = builder.defineInRange("maxHeight", 1024, 1, 4096);
            autoSizeMaxBlocks = builder.comment("Maximum long edge selected automatically after loading an image.")
                    .defineInRange("autoSizeMaxBlocks", 4, 1, 8);
            maxPlacementBlocks = builder.comment("Maximum long edge selectable with the printer size controls.")
                    .defineInRange("maxPlacementBlocks", 8, 1, 8);
            maxDownloadMiB = builder.defineInRange("maxDownloadMiB", 10, 1, 64);
            maxStoredMiB = builder.defineInRange("maxStoredMiB", 256, 16, 4096);
            fetchTimeoutSeconds = builder.defineInRange("fetchTimeoutSeconds", 30, 5, 120);
            allowedHosts = builder.comment("Empty allows every public HTTP(S) host.")
                    .defineListAllowEmpty("allowedHosts", List.of(), value -> value instanceof String);
            blockedHosts = builder.defineListAllowEmpty("blockedHosts", List.of(), value -> value instanceof String);
            builder.pop();
        }
    }

    public static final class Client {
        public final ModConfigSpec.IntValue textureCacheMiB;

        private Client(ModConfigSpec.Builder builder) {
            textureCacheMiB = builder.defineInRange("textureCacheMiB", 128, 16, 1024);
        }
    }
}
