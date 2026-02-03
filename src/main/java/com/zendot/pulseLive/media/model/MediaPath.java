package com.zendot.pulseLive.media.model;


public enum MediaPath {
    PROFILE_IMAGE("media/image/profile-image"),
    VIDEO("media/video"),
    IMAGE("media/image");

    private final String path;

    MediaPath(String path) {
        this.path = path;
    }

    public String getPath() {
        return path;
    }
}

