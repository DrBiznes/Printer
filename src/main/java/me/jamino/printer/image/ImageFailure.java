package me.jamino.printer.image;

import java.io.IOException;

/** Safe error categories shared by GUI, network responses, and worker jobs. */
public final class ImageFailure extends IOException {
    public enum Reason {
        INVALID_URL, HOST_DENIED, HTTP_ERROR, TIMEOUT, TOO_LARGE, UNSUPPORTED, DIMENSIONS,
        STORAGE_FULL, NO_IMAGE, NO_SUPPLIES, SUPPLIES_CHANGED, MISSING_DATA, BUSY,
        RATE_LIMIT, UPLOAD_DISABLED, INVALID_TRANSFER, CANCELLED, INVALID_MENU, READ_FAILED, PROCESSING;

        public String key() { return "message.printer.error." + name().toLowerCase(java.util.Locale.ROOT); }
    }

    private final Reason reason;

    public ImageFailure(Reason reason) {
        super(reason.name());
        this.reason = reason;
    }

    public Reason reason() { return reason; }

    public static Reason classify(Throwable error) {
        for (Throwable cause = error; cause != null; cause = cause.getCause()) {
            if (cause instanceof ImageFailure failure) return failure.reason;
            if (cause instanceof java.net.SocketTimeoutException || cause instanceof java.util.concurrent.TimeoutException)
                return Reason.TIMEOUT;
        }
        return Reason.PROCESSING;
    }
}
