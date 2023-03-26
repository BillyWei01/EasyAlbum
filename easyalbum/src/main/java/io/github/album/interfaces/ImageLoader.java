package io.github.album.interfaces;

import android.widget.ImageView;
import io.github.album.MediaData;

import androidx.annotation.NonNull;

public interface ImageLoader {
    void loadPreview(@NonNull MediaData data, @NonNull ImageView imageView, boolean asBitmap);

    void loadThumbnail(@NonNull MediaData data, @NonNull ImageView imageView, boolean asBitmap);
}
