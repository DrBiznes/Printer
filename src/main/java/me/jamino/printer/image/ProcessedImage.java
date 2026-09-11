package me.jamino.printer.image;

public record ProcessedImage(String contentId, byte[] png, int width, int height, int originalWidth, int originalHeight) {
    public ProcessedImage(String contentId, byte[] png, int width, int height) {
        this(contentId, png, width, height, width, height);
    }
}
