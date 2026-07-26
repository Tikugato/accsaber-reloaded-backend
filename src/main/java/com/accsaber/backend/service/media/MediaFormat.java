package com.accsaber.backend.service.media;

public enum MediaFormat {
    WEBP(".webp"),
    AVIF(".avif"),
    PNG(".png"),
    GIF(".gif");

    public final String extension;

    MediaFormat(String extension) {
        this.extension = extension;
    }
}
