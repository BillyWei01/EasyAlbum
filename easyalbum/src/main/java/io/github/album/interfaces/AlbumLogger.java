package io.github.album.interfaces;

public interface AlbumLogger {
    void d(String tag, String message);

    void e(String tag, Throwable t);
}
