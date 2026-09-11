package me.jamino.printer.client;

import me.jamino.printer.Config;
import me.jamino.printer.image.ImageFailure;
import me.jamino.printer.network.ModNetworking;
import me.jamino.printer.network.UploadTransfers;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.util.tinyfd.TinyFileDialogs;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.UUID;
import java.util.concurrent.*;

/** Client-only paths and native picker. Only image bytes and the user-entered title cross the network. */
public final class ClientFileUpload {
    private static final ThreadPoolExecutor FILES = new ThreadPoolExecutor(1, 1, 0, TimeUnit.SECONDS,
            new ArrayBlockingQueue<>(1), r -> { Thread thread = new Thread(r, "Printer local file reader"); thread.setDaemon(true); return thread; });
    private final PrinterScreen screen;
    private UUID id;
    private byte[] bytes;
    private int lastSent = -1;
    private long deadline;
    private Component status;
    private Future<?> reader;
    private boolean sentBegin;

    public ClientFileUpload(PrinterScreen screen) { this.screen = screen; }
    public boolean busy() { return id != null; }
    public Component status() { return status; }
    public void clearStatus() { if (!busy()) status = null; }

    public void select(String title) {
        if (busy()) return;
        if (!Config.SERVER.allowLocalUploads.get()) { error(ImageFailure.Reason.UPLOAD_DISABLED); return; }
        String selected;
        try (MemoryStack stack = MemoryStack.stackPush()) {
            String[] extensions = {"*.png", "*.jpg", "*.jpeg", "*.webp", "*.gif", "*.bmp", "*.tif", "*.tiff", "*.ico", "*.tga"};
            var filters = stack.mallocPointer(extensions.length);
            for (String extension : extensions) filters.put(stack.UTF8(extension));
            filters.flip();
            selected = TinyFileDialogs.tinyfd_openFileDialog(Component.translatable("gui.printer.browse_title").getString(),
                    "", filters, Component.translatable("gui.printer.file_formats").getString(), false);
        } catch (RuntimeException | LinkageError exception) {
            error(ImageFailure.Reason.READ_FAILED); return;
        }
        if (selected == null) { error(ImageFailure.Reason.CANCELLED); return; }
        if (Minecraft.getInstance().screen != screen) return;
        UUID request = UUID.randomUUID();
        id = request;
        lastSent = -1;
        sentBegin = false;
        deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(150);
        status = Component.translatable("gui.printer.reading_file");
        int limit = Config.SERVER.maxDownloadMiB.get() * 1024 * 1024;
        try {
            reader = FILES.submit(() -> {
                try {
                    byte[] content = readFile(Path.of(selected), limit);
                    Minecraft.getInstance().execute(() -> {
                        if (!request.equals(id) || Minecraft.getInstance().screen != screen) return;
                        bytes = content;
                        sentBegin = true;
                        status = Component.translatable("gui.printer.status.uploading");
                        PacketDistributor.sendToServer(new ModNetworking.BeginUploadPayload(
                                screen.getMenu().getPos(), request, content.length, title));
                    });
                } catch (Exception exception) {
                    var reason = exception instanceof ImageFailure failure ? failure.reason() : ImageFailure.Reason.READ_FAILED;
                    Minecraft.getInstance().execute(() -> { if (request.equals(id)) error(reason); });
                }
            });
        } catch (RejectedExecutionException exception) { error(ImageFailure.Reason.BUSY); }
    }

    static byte[] readFile(Path path, int limit) throws java.io.IOException {
        if (!Files.isRegularFile(path)) throw new ImageFailure(ImageFailure.Reason.READ_FAILED);
        if (Files.size(path) > limit) throw new ImageFailure(ImageFailure.Reason.TOO_LARGE);
        try (var input = Files.newInputStream(path)) {
            byte[] content = input.readNBytes(limit + 1);
            if (content.length == 0 || content.length > limit) throw new ImageFailure(ImageFailure.Reason.TOO_LARGE);
            return content;
        }
    }

    public void accept(ModNetworking.UploadReplyPayload reply) {
        if (!reply.id().equals(id)) return;
        status = Component.translatable(reply.status());
        if (reply.next() == -1 || reply.next() == -3) { reset(); return; }
        if (reply.next() == -2) { bytes = null; return; }
        if (bytes == null || reply.next() != lastSent + 1
                || (long) reply.next() * UploadTransfers.CHUNK_BYTES >= bytes.length) {
            cancel(); error(ImageFailure.Reason.INVALID_TRANSFER); return;
        }
        int start = reply.next() * UploadTransfers.CHUNK_BYTES;
        byte[] chunk = Arrays.copyOfRange(bytes, start, Math.min(bytes.length, start + UploadTransfers.CHUNK_BYTES));
        lastSent = reply.next();
        status = Component.translatable("gui.printer.upload_progress", (start + chunk.length) * 100L / bytes.length);
        PacketDistributor.sendToServer(new ModNetworking.UploadChunkPayload(id, lastSent, chunk));
    }

    public void tick() {
        if (busy() && System.nanoTime() >= deadline) { cancel(); error(ImageFailure.Reason.TIMEOUT); }
    }

    public void cancel() {
        if (id != null && sentBegin && Minecraft.getInstance().getConnection() != null)
            PacketDistributor.sendToServer(new ModNetworking.CancelUploadPayload(id));
        reset();
        status = Component.translatable(ImageFailure.Reason.CANCELLED.key());
    }

    private void error(ImageFailure.Reason reason) { reset(); status = Component.translatable(reason.key()); }
    private void reset() {
        if (reader != null) reader.cancel(true);
        reader = null;
        id = null;
        bytes = null;
        sentBegin = false;
    }
}
