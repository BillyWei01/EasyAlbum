package io.github.album;

import android.net.Uri;
import android.os.Build;

import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;

import androidx.annotation.NonNull;

import java.io.File;

public final class MediaData implements Comparable<MediaData> {
    private static final String BASE_VIDEO_URI = "content://media/external/video/media/";
    private static final String BASE_IMAGE_URI = "content://media/external/images/media/";

    static final byte ROTATE_UNKNOWN = -1;
    static final byte ROTATE_NO = 0;
    static final byte ROTATE_YES = 1;

    public final boolean isVideo;
    public final int mediaId;
    public final String parent;
    public final String name;
    public final long modifiedTime; // in seconds
    public String mime;

    long fileSize;
    int duration;
    int width;
    int height;
    byte rotate = ROTATE_UNKNOWN;

    volatile boolean hadFillData = false;

    public MediaData(boolean isVideo, int mediaId, @NonNull String parent, @NonNull String name, long modifiedTime) {
        this.isVideo = isVideo;
        this.mediaId = mediaId;
        this.parent = parent;
        this.name = name;
        this.modifiedTime = modifiedTime;
    }

    public String getPath() {
        return parent + name;
    }

    public Uri getUri() {
        String baseUri = isVideo ? BASE_VIDEO_URI : BASE_IMAGE_URI;
        return Uri.parse(baseUri + mediaId);
    }

    /**
     * Some device read data very slow with uri, but fast with file path. <br/>
     * Reference:
     * <a href='https://github.com/bumptech/glide/issues/4174'>https://github.com/bumptech/glide/issues/4174</a>
     * <p/>
     * When setting android:requestLegacyExternalStorage="true"<br/>
     * and Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q,
     * The app can read media with File API (like FileInputStream).
     * <p/>
     * Note: When get an uri from file, use 'uri.getPath()' rather than 'uri.toString()' to File API.
     */
    public Uri getProperUri() {
        /*
         * Uncertain guess:
         * Even Build.VERSION.SDK_INT > Build.VERSION_CODES.Q,
         * If a 'File' object refer to external storage call canRead() return true,
         * The app can also use File API to read.
         */
        // if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q || Utils.hasReadPermission())

        // For safety, using File API only if SDK version <= 29 (Android 10).
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q) {
            return Uri.fromFile(new File(getPath()));
        } else {
            return getUri();
        }
    }

    public boolean exists() {
        return new File(parent + name).exists();
    }

    /**
     * This method may return invalid width,
     * Because MediaStore may miss width/height data.
     *
     * If you need accurate data, use {@link #getRealWidth()} to get data.
     */
    public int getWidth() {
        return rotate != ROTATE_YES ? width : height;
    }

    public int getHeight() {
        return rotate != ROTATE_YES ? height : width;
    }

    /**
     * Value of width, height or orientation from MediaStore may be missing.
     * For ensuring to get the real info, we need to Read the file.
     * <p>
     * In this method, if the size info missing, call 'fillData()' to read the info.
     * That action may block for a while.
     * So it's suggest to call this in background thread.
     */
    public int getRealWidth() {
        if (rotate == ROTATE_UNKNOWN || width == 0 || height == 0) {
            fillData();
        }
        return rotate != ROTATE_YES ? width : height;
    }

    public int getRealHeight() {
        if (rotate == ROTATE_UNKNOWN || width == 0 || height == 0) {
            fillData();
        }
        return rotate != ROTATE_YES ? height : width;
    }

    public int getDuration() {
        if (isVideo && duration == 0) {
            checkData();
        }
        return duration;
    }

    public long getFileSize() {
        if (fileSize == 0L) {
            checkData();
        }
        return fileSize;
    }

    public boolean isGif() {
        // Mime is not always accurate,
        // In some case the media database shows the image is 'image/jpeg', but actually the file is gif.
        // In high accurate situation, it's suggest to reading head bytes to identify.
        return "image/gif".equals(mime);
    }

    void checkData() {
        if (!hadFillData) {
            FutureTask<Boolean> future = new FutureTask<>(this::fillData);
            try {
                // Limit the time for filling extra info, in case of ANR.
                AlbumConfig.getExecutor().execute(future);
                future.get(300, TimeUnit.MILLISECONDS);
            } catch (Throwable ignore) {
                LogProxy.d("Album", "Fill data time out:" + name);
            }
        }
    }

    synchronized Boolean fillData() {
        boolean missing = false;
        if (!hadFillData) {
            missing = MediaInfoReader.fetchInfo(this);
            hadFillData = true;
        }
        return missing;
    }

    @Override
    public int compareTo(MediaData o) {
        // Compare with descending order.
        if (modifiedTime == o.modifiedTime) {
            return Integer.compare(o.mediaId, mediaId);
        }
        return (o.modifiedTime < modifiedTime) ? -1 : 1;
    }

    @SuppressWarnings("EqualsWhichDoesntCheckParameterClass")
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        // || getClass() != o.getClass()
        if (o == null) return false;
        MediaData item = (MediaData) o;
        return mediaId == item.mediaId;
    }

    @Override
    public int hashCode() {
        return mediaId;
    }
}
