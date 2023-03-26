package io.github.album;

import android.media.MediaPlayer;
import android.net.Uri;
import android.os.*;
import android.view.Surface;

import androidx.annotation.NonNull;

final class VideoPlayer {
    private static final String TAG = "VideoPlayer";

    private static final int MSG_INIT = 1;
    private static final int MSG_SET_SURFACE = 2;
    private static final int MSG_START = 3;
    private static final int MSG_PAUSE = 4;
    private static final int MSG_RELEASE = 5;
    private static final int MSG_SEEK = 6;
    private static final int MSG_PREPARE = 7;

    // Using a handler thread to play the video
    private final HandlerThread worker = new HandlerThread("video-player");
    private final Handler workHandler;
    private MediaPlayer mediaPlayer;

    private final static Handler uiHandler = new Handler(Looper.getMainLooper());
    private final VideoPlayerListener playerListener;

    private boolean isPrepared = false;

    VideoPlayer(Object source, VideoPlayerListener listener) {
        playerListener = listener;
        worker.start();
        workHandler = new Handler(worker.getLooper()) {
            @Override
            public void handleMessage(@NonNull Message msg) {
                try {
                    handleMsg(msg);
                } catch (Exception e) {
                    LogProxy.e(TAG, e);
                }
            }
        };

        Message msg = Message.obtain(workHandler);
        msg.what = MSG_INIT;
        msg.obj = source;
        msg.sendToTarget();
    }

    public void setSurface(Surface surface) {
        Message msg = Message.obtain(workHandler);
        msg.what = MSG_SET_SURFACE;
        msg.obj = surface;
        msg.sendToTarget();
    }

    public void prepare() {
        Message msg = Message.obtain(workHandler);
        msg.what = MSG_PREPARE;
        msg.sendToTarget();
    }

    public void play() {
        Message msg = Message.obtain(workHandler);
        msg.what = MSG_START;
        msg.sendToTarget();
    }

    public void pause() {
        Message msg = Message.obtain(workHandler);
        msg.what = MSG_PAUSE;
        msg.sendToTarget();
    }

    public void release() {
        Message msg = Message.obtain(workHandler);
        msg.what = MSG_RELEASE;
        msg.sendToTarget();
    }

    public void seekTo(int mills) {
        Message msg = Message.obtain(workHandler);
        msg.what = MSG_SEEK;
        msg.obj = mills;
        msg.sendToTarget();
    }

    public boolean isPlaying() {
        return mediaPlayer != null && mediaPlayer.isPlaying();
    }

    private void handleMsg(@NonNull Message msg) {
        switch (msg.what) {
            case MSG_INIT:
                initPlayer(msg.obj);
                break;
            case MSG_SET_SURFACE:
                try {
                    Surface surface = (Surface) msg.obj;
                    if (surface == null || !surface.isValid()) {
                        LogProxy.d(TAG, "Surface invalid");
                        return;
                    }
                    mediaPlayer.setSurface(surface);
                } catch (Exception e) {
                    LogProxy.e(TAG, e);
                }
                break;
            case MSG_PREPARE:
                preparePlayer();
                mediaPlayer.seekTo(0);
                break;
            case MSG_START:
                preparePlayer();
                if (isPrepared) {
                    mediaPlayer.start();
                    uiHandler.post(playerListener::onStart);
                }
                break;
            case MSG_PAUSE:
                mediaPlayer.pause();
                uiHandler.post(playerListener::onStop);
                break;
            case MSG_RELEASE:
                isPrepared = false;
                mediaPlayer.stop();
                worker.quit();
                mediaPlayer.release();
                break;
            case MSG_SEEK:
                int mills = (int) msg.obj;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    mediaPlayer.seekTo(mills, MediaPlayer.SEEK_CLOSEST);
                } else {
                    mediaPlayer.seekTo(mills);
                }
                break;
            default:
                break;
        }
    }

    private void preparePlayer() {
        if (!isPrepared) {
            try {
                mediaPlayer.prepare();
                isPrepared = true;
            } catch (Exception e) {
                LogProxy.e(TAG, e);
            }
        }
    }

    private void initPlayer(Object source) {
        MediaPlayer player = new MediaPlayer();
        player.setOnPreparedListener(mp -> uiHandler.post(() ->
                playerListener.onPrepared(mp.getVideoWidth(), mp.getVideoHeight())));
        player.setOnCompletionListener(mp -> uiHandler.post(playerListener::onCompletion));
        player.setOnErrorListener((mp, what, extra) -> {
            uiHandler.post(playerListener::onError);
            return true;
        });
        player.setOnVideoSizeChangedListener((mp, width, height) ->
                uiHandler.post(() -> playerListener.onVideoSizeChanged(width, height)));
        player.setScreenOnWhilePlaying(true);
        try {
            if (source instanceof Uri) {
                player.setDataSource(Utils.appContext, (Uri) source);
            } else if (source instanceof String) {
                player.setDataSource((String) source);
            } else {
                LogProxy.e(TAG, new Exception("Invalid source:" + source));
            }
        } catch (Exception e) {
            LogProxy.e(TAG, e);
        }
        mediaPlayer = player;
    }

    interface VideoPlayerListener {
        void onStart();

        void onStop();

        void onPrepared(int width, int height);

        void onVideoSizeChanged(int width, int height);

        void onCompletion();

        void onError();
    }
}
