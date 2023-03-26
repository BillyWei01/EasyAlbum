package io.github.album;

import java.util.ArrayList;
import java.util.List;

public final class AlbumResult {
    public final List<MediaData> selectedList = new ArrayList<>();
    public boolean originalFlag = false;

    long getTotalSize() {
        long sum = 0L;
        for (MediaData mediaData : selectedList) {
            sum += mediaData.getFileSize();
        }
        return sum;
    }

    void toggleOriginalFlag() {
        originalFlag = !originalFlag;
    }
}
