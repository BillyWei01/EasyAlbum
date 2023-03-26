package io.github.album;

import androidx.annotation.NonNull;
import android.content.Context;

/**
 * Entrance of setting an request.
 */
public class EasyAlbum {
    public static AlbumConfig config() {
        return AlbumConfig.INSTANCE;
    }

    public static AlbumRequest from(@NonNull Context context) {
        return new AlbumRequest(context);
    }

    public static void preload() {
        MediaLoader.preload();
    }

    public static void clearCache() {
        MediaLoader.clearCache();
    }
}
