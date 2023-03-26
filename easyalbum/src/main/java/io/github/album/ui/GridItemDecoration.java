package io.github.album.ui;

import android.graphics.Rect;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public class GridItemDecoration extends RecyclerView.ItemDecoration {
    private final int n; // column count
    private final int space;
    private final int part;

    public GridItemDecoration(int n, int space) {
        this.n = n;
        this.space = space;
        part = space * (n - 1) / n;
    }

    @Override
    public void getItemOffsets(
            @NonNull Rect outRect,
            @NonNull View view,
            @NonNull RecyclerView parent,
            @NonNull RecyclerView.State state) {
        int position = parent.getChildLayoutPosition(view);
        int column = position % n;
        outRect.left = Math.round(part * column / (float) (n - 1));
        outRect.right = part - outRect.left;
        outRect.top = 0;
        outRect.bottom = space;
    }
}
