package io.github.album;

import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.text.TextUtils;

import androidx.exifinterface.media.ExifInterface;

import java.io.File;
import java.io.InputStream;
import java.io.FileNotFoundException;

final class MediaInfoReader {
    static boolean fetchInfo(MediaData data) {
        boolean missing = false;
        if (data.fileSize == 0L) {
            try {
                data.fileSize = new File(data.getPath()).length();
                missing = true;
            } catch (Throwable ignore) {
            }
        }
        if (data.fileSize == 0L) {
            return false;
        }

        if (data.width == 0L
                || data.height == 0L
                || data.rotate == MediaData.ROTATE_UNKNOWN
                || (data.isVideo && data.duration == 0)
        ) {
            missing = true;
            try {
                if (data.isVideo) {
                    fillVideoSize(data);
                } else {
                    fillImageSize(data);
                }
            } catch (Throwable e) {
                LogProxy.e("Album", e);
            }

            // Fill fake data if get info failed
            if (data.width == 0) {
                data.width = 1080;
            }
            if (data.height == 0) {
                data.height = 1920;
            }
            if (data.rotate == MediaData.ROTATE_UNKNOWN) {
                data.rotate = MediaData.ROTATE_NO;
            }
        }
        return missing;
    }

    private static void fillVideoSize(MediaData data) {
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        try {
            retriever.setDataSource(Utils.appContext, data.getProperUri());
            if (data.duration == 0) {
                try {
                    String duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
                    if (!TextUtils.isEmpty(duration)) {
                        data.duration = Integer.parseInt(duration);
                    }
                } catch (Exception e) {
                    LogProxy.e("Album", e);
                }
            }

            int lastRotate = data.rotate;
            if (lastRotate == MediaData.ROTATE_UNKNOWN) {
                String rotation = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION);
                if (!TextUtils.isEmpty(rotation)) {
                    int o = Integer.parseInt(rotation);
                    data.rotate = (o == 90 || o == 270) ? MediaData.ROTATE_YES : MediaData.ROTATE_NO;
                }
            }

            if (data.width == 0 || data.height == 0 ||
                    (lastRotate == MediaData.ROTATE_UNKNOWN && data.rotate == MediaData.ROTATE_YES)
            ) {
                String width = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH);
                String height = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT);
                if (!TextUtils.isEmpty(width) && !TextUtils.isEmpty(height)) {
                    data.width = Integer.parseInt(width);
                    data.height = Integer.parseInt(height);
                }
            }
        } finally {
            retriever.release();
        }
    }

    private static void fillImageSize(MediaData data) throws FileNotFoundException {
        // When calling this method, some info of the 'data' must be miss.
        // So we don't need to check if the 'data' had value.
        Uri uri = data.getProperUri();
        data.rotate = isImageRotate(uri) ? MediaData.ROTATE_YES : MediaData.ROTATE_NO;
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        InputStream inputStream = Utils.appContext.getContentResolver().openInputStream(uri);
        try {
            BitmapFactory.decodeStream(inputStream, null, options);
            data.width = options.outWidth;
            data.height = options.outHeight;
        } finally {
            Utils.closeQuietly(inputStream);
        }
    }

    private static boolean isImageRotate(Uri uri) {
        InputStream inputStream = null;
        try {
            inputStream = Utils.appContext.getContentResolver().openInputStream(uri);
            if (inputStream != null) {
                ExifInterface exif = new ExifInterface(inputStream);
                int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, -1);
                if (orientation >= ExifInterface.ORIENTATION_TRANSPOSE) {
                    return true;
                }
            }
        } catch (Throwable e) {
            LogProxy.e("Album", e);
        } finally {
            Utils.closeQuietly(inputStream);
        }
        return false;
    }
}
