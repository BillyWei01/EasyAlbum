package io.github.album.ui;

import android.view.View;

import androidx.annotation.NonNull;

public class ClickHelper {
    private static final long DELAY_LIMIT = 400;
    private static final int LAST_CLICK_TAG = 1123460103;

    public interface ClickListener {
        void onClick();
    }

    public static void listen(@NonNull View view, @NonNull final ClickListener listener) {
        view.setOnClickListener(v -> {
            long now = System.currentTimeMillis();
            Object lastClickTime = v.getTag(LAST_CLICK_TAG);
            if (lastClickTime == null) {
                listener.onClick();
            } else {
                if (lastClickTime instanceof Long) {
                    long passedTime = now - ((Long) lastClickTime);
                    if (passedTime > DELAY_LIMIT) {
                        listener.onClick();
                    }
                }
            }
            v.setTag(LAST_CLICK_TAG, now);
        });
    }
}
