package io.github.album.interfaces;

import io.github.album.MediaData;

public interface MediaFilter {
    boolean accept(MediaData media);

    // To identify the filter
    String tag();
}
