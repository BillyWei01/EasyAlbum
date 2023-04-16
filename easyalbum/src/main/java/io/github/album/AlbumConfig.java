package io.github.album;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.Comparator;

import io.github.album.interfaces.*;

import androidx.recyclerview.widget.RecyclerView.ItemAnimator;

public final class AlbumConfig {
    private static volatile Executor realExecutor;
    static ImageLoader imageLoader;
    static ItemAnimator itemAnimator;

    static boolean doSelectedAnimation = true;
    static boolean doResumeChecking = true;

    static boolean doDeletedChecking = true;
    static boolean doInfoChecking = true;

    static AlbumStyle style = new AlbumStyle();

    // Default order is sorting by update time, desc.
    static Comparator<Folder> defaultFolderComparator =
            (o1, o2) -> Long.compare(o2.updatedTime, o1.updatedTime);

    private AlbumConfig() {
    }

    static final AlbumConfig INSTANCE = new AlbumConfig();

    public AlbumConfig setStyle(AlbumStyle style) {
        if (style != null) {
            AlbumConfig.style = style;
        }
        return this;
    }

    public AlbumConfig setLogger(AlbumLogger logger) {
        LogProxy.register(logger);
        return this;
    }

    public AlbumConfig setExecutor(Executor executor) {
        if (executor != null) {
            realExecutor = executor;
        }
        return this;
    }

    static Executor getExecutor() {
        if (realExecutor == null) {
            synchronized (AlbumConfig.class) {
                if (realExecutor == null) {
                    realExecutor = Executors.newCachedThreadPool();
                }
            }
        }
        return realExecutor;
    }

    public AlbumConfig setImageLoader(ImageLoader loader) {
        if (loader != null) {
            imageLoader = loader;
        }
        return this;
    }

    public AlbumConfig setDefaultFolderComparator(Comparator<Folder> comparator) {
        if (comparator != null) {
            defaultFolderComparator = comparator;
        }
        return this;
    }

    /**
     * The itemAnimator is null by default.
     * You could set {@link androidx.recyclerview.widget.DefaultItemAnimator} or your custom ItemAnimator.
     *
     * @param animator ItemAnimator
     * @return AlbumConfig
     */
    public AlbumConfig setItemAnimator(ItemAnimator animator) {
        itemAnimator = animator;
        return this;
    }

    public AlbumConfig disableSelectedAnimation() {
        doSelectedAnimation = false;
        return this;
    }

    public AlbumConfig disableResumeChecking() {
        doResumeChecking = false;
        return this;
    }

    /**
     * Some files had been deleted, but still recording in media database.
     * We'd better check if the files that relative to media records really exists.
     * The default value of doDeletedChecking is 'true'.
     * Call this if you want to disable the checking.
     *
     * @return AlbumConfig
     */
    public AlbumConfig disableDeletedChecking() {
        doDeletedChecking = false;
        return this;
    }

    /**
     * Value of width, height, orientation, duration or file size from MediaStore may be missing.
     * For ensuring to get the real info, we need to read the file.
     * <p>
     * In some method of MediaData, it will check if value valid before return,
     * if the value is invalid, read the file to get info.
     * <p>
     * But reading file takes some time, it might block for a while.
     * <p>
     * So we provider an option:
     * To check/read the MediaData info in background after creating MediaData object.
     * In this way, when read info of MediaData, the value may be checked and ready.
     * <p>
     * The option is opening by default.
     *
     * @return AlbumConfig
     */
    public AlbumConfig disableInfoChecking() {
        doInfoChecking = false;
        return this;
    }
}
