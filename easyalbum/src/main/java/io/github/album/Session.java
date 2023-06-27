package io.github.album;

import java.util.*;

import io.github.album.interfaces.ResultCallback;

final class Session {
    static AlbumRequest request;
    static AlbumResult result;
    static boolean hadConfirm;
    private static ResultCallback resultCallback;

    static void init(AlbumRequest req, ResultCallback callback, List<MediaData> selectedList) {
        request = req;
        resultCallback = callback;
        result = new AlbumResult();
        if (selectedList != null) {
            result.selectedList.addAll(selectedList);
        }
        hadConfirm = false;
    }

    static boolean ready() {
        return request != null && resultCallback != null;
    }

    static void clear() {
        if (request != null) {
            if (request.albumListener != null) {
                request.albumListener.onAlbumClose(hadConfirm);
            }
            // AlbumRequest may hold reference by MediaLoader,
            // so clear reference of AlbumRequest's member could help gc.
            request.clear();
            request = null;
            resultCallback = null;
            result = null;
        }
    }

    static void confirm() {
        hadConfirm = true;
        if (resultCallback != null) {
            resultCallback.onResult(result);
        }
    }

    static boolean selectItem(MediaData item) {
        List<MediaData> selectedList = result.selectedList;
        if (request.limit == 1) {
            if (!selectedList.isEmpty()) {
                boolean isSameItem = selectedList.get(0) == item;
                selectedList.clear();
                if (!isSameItem) {
                    selectedList.add(item);
                }
            } else {
                selectedList.add(item);
            }
        } else {
            int index = selectedList.indexOf(item);
            if (index >= 0) {
                selectedList.remove(item);
            } else {
                if (selectedList.size() >= request.limit) {
                    handleOverLimit(request.limit);
                    return false;
                } else {
                    selectedList.add(item);
                }
            }
        }
        return true;
    }

    static String getDoneText() {
        int limit = request.limit;
        String doneString = request.getDoneString();
        if (limit == 1) {
            return doneString;
        } else {
            int selectedCount = result.selectedList.size();
            if (selectedCount == 0) {
                return doneString;
            } else {
                return doneString + '(' + selectedCount + '/' + limit + ')';
            }
        }
    }

    private static void handleOverLimit(int limit) {
        AlbumRequest.OverLimitCallback callback = request.overLimitCallback;
        if (callback != null) {
            callback.onOverLimit(limit);
        } else {
            Utils.showTips(Utils.getString(R.string.album_selected_over_limit));
        }
    }
}
