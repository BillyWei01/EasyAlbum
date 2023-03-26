package io.github.album;

import io.github.album.interfaces.AlbumLogger;

final class LogProxy {
    private static AlbumLogger logger;

    static void register(AlbumLogger realLogger) {
        logger = realLogger;
    }

    public static void d(String tag, String message){
        if (logger != null) {
            logger.d(tag, message);
        }
    }

    public static void e(String tag, Throwable t) {
        if (logger != null) {
            logger.e(tag, t);
        }
    }
}
