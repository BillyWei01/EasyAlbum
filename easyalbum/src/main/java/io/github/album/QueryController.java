package io.github.album;

import androidx.annotation.NonNull;

import java.util.List;

import io.github.album.MediaLoader.DataListener;
import io.github.album.MediaLoader.DeletedListener;
import io.github.album.MediaLoader.UpdateListener;

final class QueryController {
    volatile DataListener dataListener;
    volatile UpdateListener updateListener;
    volatile DeletedListener deletedListener;
    volatile boolean isCancelled = false;
    volatile boolean isFinished = false;

    QueryController(DataListener dataListener, UpdateListener updateListener, DeletedListener deletedListener) {
        this.dataListener = dataListener;
        this.updateListener = updateListener;
        this.deletedListener = deletedListener;
    }

    void postData(@NonNull final List<Folder> result) {
        if (dataListener != null) {
            Utils.uiHandler.post(() -> {
                DataListener listener = dataListener;
                if (listener != null) {
                    listener.onReady(result);
                }
            });
        }
    }

    void postUpdate(@NonNull final List<Folder> result) {
        if (updateListener != null) {
            Utils.uiHandler.post(() -> {
                UpdateListener listener = updateListener;
                if (listener != null) {
                    listener.onUpdate(result);
                }
            });
        }
    }

    void postInvalid(@NonNull final List<MediaData> invalidList) {
        if (deletedListener != null && !invalidList.isEmpty()) {
            Utils.uiHandler.post(() -> {
                DeletedListener listener = deletedListener;
                if (listener != null) {
                    listener.onDelete(invalidList);
                }
            });
        }
    }

    void clear() {
        isCancelled = true;
        dataListener = null;
        updateListener = null;
        deletedListener = null;
    }
}
