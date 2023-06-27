package io.github.album;

import java.lang.ref.WeakReference;
import java.util.List;
import java.util.Comparator;

import android.content.Context;
import android.content.Intent;

import androidx.annotation.NonNull;

import io.github.album.interfaces.*;

import android.text.TextUtils;

public final class AlbumRequest {
    private static final int MAX_SELECTED_LIMIT = 99;
    private static final int DEFAULT_SELECTED_LIMIT = 9;

    public interface OverLimitCallback {
        void onOverLimit(int limit);
    }

    MediaFilter filter;

    Comparator<Folder> folderComparator = AlbumConfig.defaultFolderComparator;

    AlbumListener albumListener;
    OverLimitCallback overLimitCallback;
    private String allString;
    private String doneString;
    int limit = DEFAULT_SELECTED_LIMIT;
    List<MediaData> selectedList;
    boolean enableOriginal = false;

    boolean thumbnailAsBitmap = true;
    boolean previewAsBitmap = false;

    private WeakReference<Context> contextRef;

    AlbumRequest(Context context) {
        this.contextRef = new WeakReference<>(context);
    }

    public AlbumRequest setFilter(MediaFilter filter) {
        this.filter = filter;
        return this;
    }

    String getTag() {
        String tag = filter != null ? filter.tag() : "";
        return tag == null ? "" : tag;
    }

    public AlbumRequest setFolderComparator(Comparator<Folder> comparator) {
        if (comparator != null) {
            folderComparator = comparator;
        }
        return this;
    }

    public AlbumRequest setAllString(String str) {
        this.allString = str;
        return this;
    }

    String getAllString() {
        if (TextUtils.isEmpty(allString)) {
            return Utils.getString(R.string.album_all);
        } else {
            return allString;
        }
    }

    public AlbumRequest setDoneString(String str) {
        this.doneString = str;
        return this;
    }

    String getDoneString() {
        if (TextUtils.isEmpty(doneString)) {
            return Utils.getString(R.string.album_done);
        } else {
            return doneString;
        }
    }

    /**
     * Set selected limit.
     *
     * @param limit Media amount that can be selected, must be positive.
     *              Especially, when limit = 1, it uses like radio button.
     * @return AlbumRequest
     */
    public AlbumRequest setSelectedLimit(int limit) {
        if (limit <= 0) {
            throw new IllegalArgumentException("Limit must be positive");
        }
        if (limit > MAX_SELECTED_LIMIT) {
            throw new IllegalArgumentException("Limit must less or equal than " + MAX_SELECTED_LIMIT);
        }
        this.limit = limit;
        return this;
    }

    /**
     * If you want to reselect, set last selecting medias,
     *
     * @param mediaList The selected list
     * @return AlbumRequest
     */
    public AlbumRequest setSelectedList(List<MediaData> mediaList) {
        selectedList = mediaList;
        return this;
    }

    public AlbumRequest setOverLimitCallback(OverLimitCallback callback) {
        this.overLimitCallback = callback;
        return this;
    }

    public AlbumRequest setAlbumListener(AlbumListener listener) {
        this.albumListener = listener;
        return this;
    }

    /**
     * @param asBitmap <br>
     *                 True: Show pictures as static image;  <br>
     *                 False: Show picture as animated drawable if the media file is animated gif, otherwise show as static image.
     * @return AlbumRequest
     */
    public AlbumRequest setThumbnailAsBitmap(boolean asBitmap) {
        this.thumbnailAsBitmap = asBitmap;
        return this;
    }

    public AlbumRequest setPreviewAsBitmap(boolean asBitmap) {
        this.previewAsBitmap = true;
        return this;
    }

    public AlbumRequest enableOriginal() {
        this.enableOriginal = true;
        return this;
    }

    public void start(@NonNull ResultCallback callback) {
        if (AlbumConfig.imageLoader == null) {
            throw new IllegalArgumentException("ImageLoader is null, forget to call AlbumConfig.setImageLoader()?");
        }

        Session.init(this, callback, selectedList);

        if (contextRef != null) {
            Context context = contextRef.get();
            if (context != null) {
                context.startActivity(new Intent(context, AlbumActivity.class));
            }
            contextRef = null;
        }
    }

    void clear() {
        filter = null;
        overLimitCallback = null;
        folderComparator = null;
        albumListener = null;
    }
}
