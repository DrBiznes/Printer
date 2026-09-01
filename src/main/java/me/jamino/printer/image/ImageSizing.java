package me.jamino.printer.image;

public final class ImageSizing {
    public static final int PIXELS_PER_BLOCK = 128;

    private ImageSizing() {}

    public static Size automatic(int pixelWidth, int pixelHeight, int maximumLongEdge) {
        int sourceLongEdge = Math.max(positive(pixelWidth), positive(pixelHeight));
        int desiredLongEdge = Math.max(1, (sourceLongEdge + PIXELS_PER_BLOCK - 1) / PIXELS_PER_BLOCK);
        return forLongEdge(pixelWidth, pixelHeight, Math.min(desiredLongEdge, positive(maximumLongEdge)));
    }

    public static Size forLongEdge(int pixelWidth, int pixelHeight, int longEdge) {
        int width = positive(pixelWidth);
        int height = positive(pixelHeight);
        int edge = positive(longEdge);
        if (width >= height) {
            return new Size(edge, Math.max(1, Math.round(edge * height / (float) width)));
        }
        return new Size(Math.max(1, Math.round(edge * width / (float) height)), edge);
    }

    public static PixelSize textureSize(int sourceWidth, int sourceHeight, int blocksWide, int blocksHigh) {
        int width = positive(sourceWidth);
        int height = positive(sourceHeight);
        int pixelBudget = Math.max(positive(blocksWide), positive(blocksHigh)) * PIXELS_PER_BLOCK;
        double scale = Math.min(1.0D, pixelBudget / (double) Math.max(width, height));
        return new PixelSize(Math.max(1, (int) Math.round(width * scale)),
                Math.max(1, (int) Math.round(height * scale)));
    }

    private static int positive(int value) {
        return Math.max(1, value);
    }

    public record Size(int width, int height) {
        public int area() {
            return Math.multiplyExact(width, height);
        }
    }

    public record PixelSize(int width, int height) {}
}
