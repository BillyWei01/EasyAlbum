package io.github.album.ui;

import android.view.*;
import android.graphics.*;

public class RoundedBottomProvider extends ViewOutlineProvider {
    private final int radius;

    public RoundedBottomProvider(int radius) {
        this.radius = radius;
    }

    @Override
    public void getOutline(View view, Outline outline) {
        int left = 0;
        int top = 0;
        int right = view.getWidth();
        int bottom = view.getHeight();
        outline.setRoundRect(left, top - radius, right, bottom, radius);
    }
}
