package io.github.album;

import android.content.Context;
import android.content.res.Resources;
import android.widget.Toast;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.ColorRes;
import androidx.annotation.StringRes;

import java.io.File;
import android.os.Environment;

import java.io.Closeable;

final class Utils {
    private static final long K = 1L << 10;
    private static final long M = 1L << 20;
    private static final long G = 1L << 30;

    static final Handler uiHandler = new Handler(Looper.getMainLooper());

    private static final int UNKNOWN = -1;
    private static final int NO = 0;
    private static final int YES = 1;
    private static int hasPermission = UNKNOWN;

    // Assign value by AlbumContentProvider
    static Context appContext;

    private static float density = 1f;

    private static float getDensity() {
        if (density == 1f) {
            density = Resources.getSystem().getDisplayMetrics().density;
        }
        return density;
    }

    static boolean hasReadPermission() {
        if(hasPermission == UNKNOWN){
            boolean flag = false;
            try {
                File externalRoot = Environment.getExternalStorageDirectory();
                flag = externalRoot != null && externalRoot.canRead();
            } catch (Throwable ignore) {
            }
            hasPermission = flag ? YES : NO;
        }
        return hasPermission == YES;
    }

    static int dp2px(float dp) {
        return Math.round(dp * getDensity());
    }

    static String getFormatTime(long millis) {
        long s = millis / 1000;
        long min = s / 60;
        long sec = s % 60;
        StringBuilder builder = new StringBuilder();
        builder.append(min).append(':');
        if (s < 10) {
            builder.append('0');
        }
        builder.append(sec);
        return builder.toString();
    }

    static String getFormatSize(long size) {
        if (size >= G) {
            return (size >> 30) + "GB";
        } else if (size >= M) {
            return (size >> 20) + "MB";
        } else if (size >= K) {
            return (size >> 10) + "KB";
        } else {
            return size + "B";
        }
    }

    static void closeQuietly(Closeable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (Exception ignore) {
            }
        }
    }

    static String getString(@StringRes int resId) {
        return appContext.getString(resId);
    }

    static int getColor(@ColorRes int resId) {
        return appContext.getResources().getColor(resId);
    }

    static void showTips(String tips) {
        Toast.makeText(appContext, tips, Toast.LENGTH_SHORT).show();
    }

    static String getParentPath(String path) {
        int end = path.lastIndexOf('/');
        return end <= 0 ? "" : path.substring(0, end + 1);
    }

    static String getFileName(String path) {
        int length = path.length();
        if (length == 0) return "";
        if (path.charAt(length - 1) == '/') {
            int index = path.lastIndexOf('/', length - 2);
            return index <= 0 ? path.substring(0, length - 1) : path.substring(index + 1, length - 1);
        } else {
            int index = path.lastIndexOf('/');
            return index <= 0 ? path : path.substring(index + 1);
        }
    }
}
