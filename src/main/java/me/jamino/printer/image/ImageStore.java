package me.jamino.printer.image;

import me.jamino.printer.Config;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.DimensionDataStorage;

import java.util.HashSet;
import java.util.Set;

public final class ImageStore {
    private static final String INDEX_NAME = "printer_image_index";

    private ImageStore() {}

    public static boolean putSource(MinecraftServer server, ProcessedImage image) { return put(server, image, true); }
    public static boolean putVariant(MinecraftServer server, ProcessedImage image) { return put(server, image, false); }
    public static byte[] getSource(MinecraftServer server, String id) { return get(server, id, true); }
    public static byte[] getVariant(MinecraftServer server, String id) { return get(server, id, false); }

    private static boolean put(MinecraftServer server, ProcessedImage image, boolean source) {
        DimensionDataStorage storage = server.overworld().getDataStorage();
        IndexData index = storage.computeIfAbsent(IndexData.FACTORY, INDEX_NAME);
        Set<String> ids = source ? index.sources : index.variants;
        if (ids.contains(image.contentId())) return true;
        long maxBytes = Config.SERVER.maxStoredMiB.get() * 1024L * 1024L;
        if (index.totalBytes + image.png().length > maxBytes) return false;
        BlobData blob = new BlobData(image.png(), image.width(), image.height());
        blob.setDirty();
        storage.set(fileName(image.contentId(), source), blob);
        ids.add(image.contentId());
        index.totalBytes += image.png().length;
        index.setDirty();
        return true;
    }

    private static byte[] get(MinecraftServer server, String id, boolean source) {
        if (id == null || !id.matches("[0-9a-f]{64}")) return null;
        DimensionDataStorage storage = server.overworld().getDataStorage();
        IndexData index = storage.computeIfAbsent(IndexData.FACTORY, INDEX_NAME);
        if (!(source ? index.sources : index.variants).contains(id)) return null;
        BlobData blob = storage.get(BlobData.FACTORY, fileName(id, source));
        return blob == null ? null : blob.png.clone();
    }

    private static String fileName(String id, boolean source) {
        return "printer_" + (source ? "source_" : "variant_") + id;
    }

    private static final class IndexData extends SavedData {
        static final Factory<IndexData> FACTORY = new Factory<>(IndexData::new, IndexData::load);
        final Set<String> sources = new HashSet<>();
        final Set<String> variants = new HashSet<>();
        long totalBytes;

        static IndexData load(CompoundTag tag, HolderLookup.Provider registries) {
            IndexData data = new IndexData();
            ListTag sources = tag.getList("Sources", StringTag.TAG_STRING);
            for (int i = 0; i < sources.size(); i++) data.sources.add(sources.getString(i));
            ListTag variants = tag.getList("Variants", StringTag.TAG_STRING);
            for (int i = 0; i < variants.size(); i++) data.variants.add(variants.getString(i));
            data.totalBytes = tag.getLong("TotalBytes");
            return data;
        }

        @Override
        public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
            ListTag sourceList = new ListTag();
            sources.forEach(value -> sourceList.add(StringTag.valueOf(value)));
            ListTag variantList = new ListTag();
            variants.forEach(value -> variantList.add(StringTag.valueOf(value)));
            tag.put("Sources", sourceList);
            tag.put("Variants", variantList);
            tag.putLong("TotalBytes", totalBytes);
            return tag;
        }
    }

    private static final class BlobData extends SavedData {
        static final Factory<BlobData> FACTORY = new Factory<>(() -> new BlobData(new byte[0], 0, 0), BlobData::load);
        final byte[] png;
        final int width;
        final int height;

        BlobData(byte[] png, int width, int height) {
            this.png = png.clone();
            this.width = width;
            this.height = height;
        }

        static BlobData load(CompoundTag tag, HolderLookup.Provider registries) {
            return new BlobData(tag.getByteArray("Png"), tag.getInt("Width"), tag.getInt("Height"));
        }

        @Override
        public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
            tag.putByteArray("Png", png);
            tag.putInt("Width", width);
            tag.putInt("Height", height);
            return tag;
        }
    }
}
