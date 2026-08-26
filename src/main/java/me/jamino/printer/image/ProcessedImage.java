package me.jamino.printer.image;

public record ProcessedImage(String contentId, byte[] png, int width, int height) {
}
