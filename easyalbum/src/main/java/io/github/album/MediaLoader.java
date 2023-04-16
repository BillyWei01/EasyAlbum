package io.github.album;

import android.database.Cursor;
import android.os.*;
import android.net.Uri;
import android.provider.MediaStore;

import androidx.annotation.NonNull;

import androidx.core.content.ContentResolverCompat;

import java.io.File;
import java.util.*;

import io.github.album.interfaces.MediaFilter;

final class MediaLoader {
    private static final String TAG = "MediaLoader";

    private static final Uri CONTENT_URI = MediaStore.Files.getContentUri("external");

    private static final String TYPE_SELECTION = "(" + MediaStore.Files.FileColumns.MEDIA_TYPE + "="
            + MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO
            + " OR " + MediaStore.Files.FileColumns.MEDIA_TYPE + "="
            + MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE
            + ")";

    private static final int IDX_ID = 0;
    private static final int IDX_DATA = 1;
    private static final int IDX_MEDIA_TYPE = 2;
    private static final int IDX_DATE_MODIFIED = 3;
    private static final int IDX_MIME_TYPE = 4;
    private static final int IDX_DURATION = 5;
    private static final int IDX_SIZE = 6;
    private static final int IDX_WIDTH = 7;
    private static final int IDX_HEIGHT = 8;
    private static final int IDX_ORIENTATION = 9;

    private static final String[] PROJECTIONS = new String[]{
            MediaStore.MediaColumns._ID,
            MediaStore.MediaColumns.DATA,
            MediaStore.Files.FileColumns.MEDIA_TYPE,
            MediaStore.MediaColumns.DATE_MODIFIED,
            MediaStore.MediaColumns.MIME_TYPE,
            MediaStore.Video.Media.DURATION,
            MediaStore.MediaColumns.SIZE,
            MediaStore.MediaColumns.WIDTH,
            MediaStore.MediaColumns.HEIGHT,
            MediaStore.Images.Media.ORIENTATION
    };

    private static final String[] ID_PROJECTION = new String[]{
            MediaStore.MediaColumns._ID
    };

    private static final long NEW_PICTURE_THRESHOLD = 10L;

    private static final SerialExecutor loadingExecutor = new SerialExecutor();
    private static final SerialExecutor checkingExecutor = new SerialExecutor();
    private static QueryController preloadController = null;

    private static boolean hasCache = false;
    private static boolean hadCheckedExists = false;
    private static final Set<Integer> notExistSet = new HashSet<>();
    private static final Map<Integer, MediaData> mediaCache = new LinkedHashMap<>();
    private static final Map<String, List<Folder>> resultCache = new HashMap<>();

    // Many media have same parent or mime type,
    // make them reference to same object, for saving memory.
    private static final StringPool parentPool = new StringPool();
    private static final StringPool mimePool = new StringPool();

    public interface DataListener {
        void onReady(@NonNull List<Folder> result);
    }

    public interface UpdateListener {
        void onUpdate(@NonNull List<Folder> result);
    }

    public interface DeletedListener {
        void onDelete(@NonNull List<MediaData> deleted);
    }

    public static void start(final AlbumRequest request, final QueryController controller) {
        cancelPreload();
        controller.isFinished = false;
        loadingExecutor.execute(() -> {
            if (controller.isCancelled) {
                return;
            }
            long startTime = SystemClock.uptimeMillis();
            try {
                String tag = request.getTag();
                List<Folder> result = resultCache.get(tag);
                if (result != null && controller.dataListener != null) {
                    logTime("Post data with cache in ", startTime);
                    controller.postData(result);
                }

                boolean hasDataChanged;
                if (!hasCache) {
                    load(controller);
                    hasCache = true;
                    hasDataChanged = true;
                    logTime("Load time:", startTime);
                } else {
                    hasDataChanged = refresh(controller);
                    logTime("Refresh time:", startTime);
                }

                if (controller.isCancelled) {
                    return;
                }

                if (result == null || controller.dataListener != null || controller.updateListener != null) {
                    if (result == null) {
                        List<Folder> newResult = makeResult(request);
                        logTime("Post new data in ", startTime);
                        resultCache.put(tag, newResult);
                        controller.postData(newResult);
                    } else if (hasDataChanged) {
                        List<Folder> newResult = makeResult(request);
                        if (!result.equals(newResult)) {
                            logTime("Post update data in ", startTime);
                            resultCache.put(tag, newResult);
                            controller.postUpdate(newResult);
                        }
                    } else {
                        LogProxy.d(TAG, "No data changed");
                    }
                }
                LogProxy.d(TAG, "Media count:" + mediaCache.size());

                if (AlbumConfig.doDeletedChecking && !hadCheckedExists && !controller.isCancelled) {
                    checkExists(controller, startTime);
                }
            } catch (Throwable t) {
                LogProxy.e(TAG, t);
            } finally {
                controller.isFinished = true;
            }
        });
    }

    private static void logTime(String action, long startTime) {
        long queryTime = SystemClock.uptimeMillis() - startTime;
        LogProxy.d(TAG, action + queryTime + "ms");
    }

    /**
     * Only preload the media caches.
     * But it will also speed up the real 'AlbumRequest' loading.
     */
    public static void preload() {
        if (hasCache) {
            return;
        }
        final QueryController controller = new QueryController(null, null, null);
        preloadController = controller;
        loadingExecutor.execute(() -> {
            if (hasCache || controller.isCancelled) {
                preloadController = null;
                return;
            }
            long startTime = SystemClock.uptimeMillis();
            try {
                load(controller);
                hasCache = true;
                if (AlbumConfig.doDeletedChecking && !hadCheckedExists && !controller.isCancelled) {
                    checkExists(controller, startTime);
                }
            } catch (Throwable t) {
                LogProxy.e(TAG, t);
            } finally {
                preloadController = null;
            }
        });
    }

    /**
     * 'checkDeleted' may take some time, when calling {@link #start},
     * it's better to stop checking to make query be executed as soon as possibly.
     */
    private static void cancelPreload() {
        QueryController t = preloadController;
        if (t != null) {
            t.isCancelled = true;
        }
    }

    public static void clearCache() {
        if (Session.result == null) {
            loadingExecutor.execute(() -> {
                hasCache = false;
                mediaCache.clear();
                resultCache.clear();
            });
        }
    }

    private static void load(final QueryController controller) {
        List<MediaData> list = query(null);
        if (list != null && !list.isEmpty()) {
            for (MediaData item : list) {
                mediaCache.put(item.mediaId, item);
            }
            checkInfo(list, controller);
        }
    }

    private static boolean refresh(final QueryController controller) {
        Cursor cursor = ContentResolverCompat.query(
                Utils.appContext.getContentResolver(),
                CONTENT_URI, ID_PROJECTION, TYPE_SELECTION, null, null, null
        );
        if (cursor == null) {
            return false;
        }
        List<Integer> ids;
        try {
            int count = cursor.getCount();
            ids = new ArrayList<>(count);
            while (cursor.moveToNext()) {
                int id = cursor.getInt(IDX_ID);
                if (exist(id)) {
                    ids.add(id);
                }
            }
        } finally {
            Utils.closeQuietly(cursor);
        }

        Set<Integer> cacheSet = mediaCache.keySet();
        Set<Integer> newIdSet = new HashSet<>(ids);
        if (cacheSet.equals(newIdSet)) {
            return false;
        }
        cacheSet.retainAll(newIdSet);
        ids.removeAll(cacheSet);
        if (!ids.isEmpty()) {
            // query the increment
            StringBuilder builder = new StringBuilder();
            if (ids.size() < 50) {
                queryByIds(ids, builder, controller);
            } else {
                List<Integer> idList = new ArrayList<>();
                for (Integer id : ids) {
                    idList.add(id);
                    if (idList.size() == 50) {
                        queryByIds(idList, builder, controller);
                        idList.clear();
                    }
                }
                if (!idList.isEmpty()) {
                    queryByIds(idList, builder, controller);
                }
            }
        }
        return true;
    }

    private static void queryByIds(
            List<Integer> idList,
            StringBuilder builder,
            final QueryController controller
    ) {
        builder.setLength(0);
        if (idList.size() == 1) {
            builder.append(MediaStore.MediaColumns._ID).append('=').append(idList.get(0));
        } else {
            builder.append(MediaStore.MediaColumns._ID).append(" in (");
            for (Integer id : idList) {
                builder.append(id).append(',');
            }
            builder.setCharAt(builder.length() - 1, ')');
        }

        List<MediaData> list = query(builder.toString());
        if (list != null && !list.isEmpty()) {
            List<MediaData> excludeList = new ArrayList<>();
            long currentTimeSeconds = System.currentTimeMillis() / 1000;
            for (MediaData item : list) {
                // If the picture is just taking by camera,
                // it might be processing while picture info had already been saved to media database.
                // So we need to check if the picture file is exists when the picture is new.
                if (currentTimeSeconds - item.modifiedTime < NEW_PICTURE_THRESHOLD) {
                    if (item.exists()) {
                        mediaCache.put(item.mediaId, item);
                    } else {
                        excludeList.add(item);
                    }
                } else {
                    mediaCache.put(item.mediaId, item);
                }
            }
            if (!excludeList.isEmpty()) {
                list.removeAll(excludeList);
            }
            checkInfo(list, controller);
        }
    }

    private static void checkInfo(final List<MediaData> list, final QueryController controller) {
        if (!AlbumConfig.doInfoChecking || list.isEmpty()) {
            return;
        }
        checkingExecutor.execute(() -> {
            int count = 0;
            long t0 = SystemClock.uptimeMillis();
            List<MediaData> emptyFileMedias = new ArrayList<>();
            for (MediaData item : list) {
                if (item.fillData()) {
                    if (item.fileSize <= 0L && Utils.hasReadPermission()) {
                        emptyFileMedias.add(item);
                    }
                    count++;
                }
            }
            if (!emptyFileMedias.isEmpty()) {
                markEmptyMedia(emptyFileMedias, controller);
                LogProxy.d(TAG, "There are " + emptyFileMedias.size() + " empty files");
            }
            long t1 = SystemClock.uptimeMillis();
            LogProxy.d(TAG, "Check info finish, missing count:" + count + ", used:" + (t1 - t0) + "ms");
        });
    }

    private static void markEmptyMedia(final List<MediaData> emptyFileList, final QueryController controller) {
        loadingExecutor.execute(() -> {
            for (MediaData item : emptyFileList) {
                notExistSet.add(item.mediaId);
            }
            if (!controller.isCancelled) {
                controller.postInvalid(emptyFileList);
            }
        });
    }

    private static List<MediaData> query(String idSelection) {
        String selection = idSelection != null ? idSelection : TYPE_SELECTION;
        Cursor cursor = ContentResolverCompat.query(
                Utils.appContext.getContentResolver(),
                CONTENT_URI, PROJECTIONS, selection, null, null, null
        );
        if (cursor == null) {
            return null;
        }
        List<MediaData> list;
        try {
            int count = cursor.getCount();
            list = new ArrayList<>(count);
            while (cursor.moveToNext()) {
                int id = cursor.getInt(IDX_ID);
                String path = cursor.getString(IDX_DATA);
                if (!exist(id) || path == null || path.isEmpty()) continue;

                int type = cursor.getInt(IDX_MEDIA_TYPE);
                long time = cursor.getLong(IDX_DATE_MODIFIED);
                String parent = parentPool.getOrAdd(Utils.getParentPath(path));
                String name = Utils.getFileName(path);
                boolean isVideo = type == MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO;
                MediaData item = new MediaData(isVideo, id, parent, name, time);

                item.mime = mimePool.getOrAdd(cursor.getString(IDX_MIME_TYPE));
                item.duration = isVideo ? cursor.getInt(IDX_DURATION) : 0;
                item.fileSize = cursor.getLong(IDX_SIZE);
                item.width = cursor.getInt(IDX_WIDTH);
                item.height = cursor.getInt(IDX_HEIGHT);

                if (!cursor.isNull(IDX_ORIENTATION)) {
                    int orientation = cursor.getInt(IDX_ORIENTATION);
                    item.rotate = (orientation == 90 || orientation == 270) ? MediaData.ROTATE_YES : MediaData.ROTATE_NO;
                }
                list.add(item);
            }
        } finally {
            Utils.closeQuietly(cursor);
        }
        return list;
    }

    private static List<Folder> makeResult(AlbumRequest request) {
        MediaFilter filter = request.filter;
        ArrayList<MediaData> totalList = new ArrayList<>(mediaCache.size());

        if (filter == null) {
            totalList.addAll(mediaCache.values());
        } else {
            for (MediaData item : mediaCache.values()) {
                if (filter.accept(item)) {
                    totalList.add(item);
                }
            }
        }

        Collections.sort(totalList);

        Map<String, ArrayList<MediaData>> groupMap = new HashMap<>();
        for (MediaData item : totalList) {
            String parent = item.parent;
            ArrayList<MediaData> subList = groupMap.get(parent);
            if (subList == null) {
                subList = new ArrayList<>();
                groupMap.put(parent, subList);
            }
            subList.add(item);
        }

        final List<Folder> result = new ArrayList<>(groupMap.size() + 1);
        for (Map.Entry<String, ArrayList<MediaData>> entry : groupMap.entrySet()) {
            String folderName = Utils.getFileName(entry.getKey());
            result.add(new Folder(folderName, entry.getValue()));
        }
        Collections.sort(result, request.folderComparator);
        result.add(0, new Folder(request.getAllString(), totalList));
        return result;
    }

    /*
     * If the app has permission to read the external files (call File.list()),
     * the checking will be fast.
     * For Android applies 'ScopedStorage' since Android 10,
     * it's recommended to add ' android:requestLegacyExternalStorage="true" ' in AndroidManifest.xml.
     */
    private static void checkExists(final QueryController controller, long startTime) {
        if (mediaCache.isEmpty()) {
            return;
        }
        final List<MediaData> notExistsList = new ArrayList<>();
        if (Utils.hasReadPermission()) {
            Map<String, ArrayList<MediaData>> groupMap = new HashMap<>();
            for (MediaData item : mediaCache.values()) {
                String parent = item.parent;
                ArrayList<MediaData> subList = groupMap.get(parent);
                if (subList == null) {
                    subList = new ArrayList<>();
                    groupMap.put(parent, subList);
                }
                subList.add(item);
            }
            for (Map.Entry<String, ArrayList<MediaData>> entry : groupMap.entrySet()) {
                checkDirectory(controller, entry.getKey(), entry.getValue(), notExistsList);
                if (controller.isCancelled) {
                    return;
                }
            }
        } else {
            ArrayList<MediaData> totalList = new ArrayList<>(mediaCache.size());
            totalList.addAll(mediaCache.values());
            checkList(controller, totalList, notExistsList);
        }

        if (controller.isCancelled) {
            return;
        }

        hadCheckedExists = true;
        long time = SystemClock.uptimeMillis() - startTime;
        LogProxy.d(TAG, "Check exists finish, used:" + time + "ms, deleted count:" + notExistsList.size());
        controller.postInvalid(notExistsList);
    }

    private static void checkDirectory(QueryController controller, String parentPath, List<MediaData> subList,
                                       List<MediaData> notExistsList) {
        if (subList.isEmpty()) {
            return;
        }
        // 'list()' can check many files (exist) in one time,
        // it's much faster than calling 'File.exist()' for each file.
        File parentFile = new File(parentPath);
        String[] fileNameArray = parentFile.list();
        if (fileNameArray == null || fileNameArray.length == 0) {
            checkList(controller, subList, notExistsList);
        } else {
            Set<String> nameSet = new HashSet<>(Arrays.asList(fileNameArray));
            long currentTimeSeconds = System.currentTimeMillis() / 1000L;
            for (MediaData item : subList) {
                if (currentTimeSeconds - item.modifiedTime < NEW_PICTURE_THRESHOLD || nameSet.contains(item.name)) {
                    continue;
                }
                if (controller.isCancelled) {
                    return;
                }
                // In case of "false negative" (The file exist, but 'list()' not return it),
                // still calling the 'exists()' to check the item.
                if (!item.exists()) {
                    notExistsList.add(item);
                    markDeleted(item);
                }
            }
        }
    }

    private static void checkList(final QueryController controller, List<MediaData> list, List<MediaData> deletedSet) {
        long currentTimeSeconds = System.currentTimeMillis() / 1000L;
        for (MediaData item : list) {
            if (controller.isCancelled) {
                return;
            }
            if (currentTimeSeconds - item.modifiedTime > NEW_PICTURE_THRESHOLD && !item.exists()) {
                deletedSet.add(item);
                markDeleted(item);
            }
        }
    }

    private static boolean exist(int mediaId) {
        return notExistSet.isEmpty() || !notExistSet.contains(mediaId);
    }

    private static void markDeleted(MediaData item) {
        Integer mediaId = item.mediaId;
        notExistSet.add(mediaId);
        mediaCache.remove(mediaId);
    }
}
