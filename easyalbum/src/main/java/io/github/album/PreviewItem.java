package io.github.album;

import android.graphics.SurfaceTexture;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatImageView;

import com.davemorrissey.labs.subscaleview.ImageSource;
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView;
import com.github.chrisbanes.photoview.PhotoView;

import io.github.album.VideoPlayer.VideoPlayerListener;

abstract class PreviewItem {
    final MediaData media;
    final View root;

    View.OnClickListener onClickListener;

    PreviewItem(ViewGroup container, MediaData media) {
        this.media = media;
        int layoutId = media.isVideo ? R.layout.album_video_item : R.layout.album_image_item;
        root = LayoutInflater.from(container.getContext()).inflate(layoutId, container, false);
    }

    void performClick() {
        if (onClickListener != null) {
            onClickListener.onClick(null);
        }
    }

    abstract void show();

    static class ImageItem extends PreviewItem {
        ImageItem(ViewGroup container, MediaData media) {
            super(container, media);
        }

        @Override
        void show() {
            PhotoView photoView = root.findViewById(R.id.album_item_photo_view);
            SubsamplingScaleImageView scaleImageView = root.findViewById(R.id.album_item_scale_iv);
            int width = media.getWidth();
            int height = media.getHeight();
            if (width < 2048 && height < 2048) {
                photoView.setVisibility(View.VISIBLE);
                scaleImageView.setVisibility(View.GONE);
                AlbumRequest request = Session.request;
                if (request != null) {
                    AlbumConfig.imageLoader.loadPreview(media, photoView, request.previewAsBitmap);
                }
            } else {
                scaleImageView.setExecutor(AlbumConfig.getExecutor());
                photoView.setVisibility(View.GONE);
                scaleImageView.setOrientation(SubsamplingScaleImageView.ORIENTATION_USE_EXIF);
                scaleImageView.setVisibility(View.VISIBLE);
                scaleImageView.setImage(ImageSource.uri(media.getProperUri()));
            }
            photoView.setOnClickListener(v -> performClick());
            scaleImageView.setOnClickListener(v -> performClick());
            root.setOnClickListener(v -> performClick());
        }
    }

    enum VideoState {
        INIT,
        READY,
        RELEASE
    }

    static class VideoItem extends PreviewItem {
        private final TextureView textureView;
        private final AppCompatImageView playView;
        private VideoPlayer player;
        private Surface videoSurface;
        private VideoState state = VideoState.INIT;

        VideoItem(ViewGroup container, MediaData media) {
            super(container, media);
            textureView = root.findViewById(R.id.texture_view);
            playView = root.findViewById(R.id.play_view);
            playView.setOnClickListener(v -> togglePlay());
            root.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener() {
                @Override
                public void onViewAttachedToWindow(View v) {
                }

                @Override
                public void onViewDetachedFromWindow(View v) {
                    release();
                }
            });
            textureView.setOnClickListener(v -> onClick());
            root.setOnClickListener(v -> onClick());
        }

        private void onClick() {
            performClick();
            showPlayView();
        }

        private void showPlayView() {
            if (playView.getAlpha() != 1f) {
                playView.setAlpha(1f);
            }
        }

        private void togglePlay() {
            if (state == VideoState.READY && player != null) {
                if (player.isPlaying()) {
                    player.pause();
                } else {
                    player.play();
                }
            }
        }

        void pause() {
            if (state == VideoState.READY && player != null && player.isPlaying()) {
                player.pause();
                showPlayView();
            }
        }

        private void release() {
            if (state != VideoState.RELEASE) {
                state = VideoState.RELEASE;
                if (player != null) {
                    player.release();
                }
                if (videoSurface != null) {
                    videoSurface.release();
                }
                videoSurface = null;
                player = null;
            }
        }

        void show() {
            if (state == VideoState.INIT) {
                player = new VideoPlayer(media.getProperUri(), getListener());
                textureView.setVisibility(View.VISIBLE);
                textureView.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
                    @Override
                    public void onSurfaceTextureAvailable(@NonNull SurfaceTexture surface, int width, int height) {
                        videoSurface = new Surface(surface);
                        player.setSurface(videoSurface);
                        player.prepare();
                    }

                    @Override
                    public void onSurfaceTextureSizeChanged(@NonNull SurfaceTexture surface, int width, int height) {
                    }

                    @Override
                    public boolean onSurfaceTextureDestroyed(@NonNull SurfaceTexture surface) {
                        return false;
                    }

                    @Override
                    public void onSurfaceTextureUpdated(@NonNull SurfaceTexture surface) {
                    }
                });
                root.setTag(this);
            }
        }

        private void setSurfaceSize(TextureView textureView, int width, int height) {
            int rootWidth = root.getWidth();
            int rootHeight = root.getHeight();
            if (width <= 0 || height <= 0 || rootWidth <= 0 || rootHeight <= 0) {
                return;
            }
            float ratio = (float) width / (float) height;
            int targetHeight = (int) (rootWidth / ratio);
            if (targetHeight > rootHeight) {
                targetHeight = rootHeight;
            }
            int targetWidth = (int) (targetHeight * ratio);
            ViewGroup.LayoutParams params = textureView.getLayoutParams();
            if (params.width != targetWidth || params.height != targetHeight) {
                params.width = targetWidth;
                params.height = targetHeight;
                textureView.setLayoutParams(params);
            }
        }

        private VideoPlayerListener getListener() {
            return new VideoPlayerListener() {
                @Override
                public void onStart() {
                    playView.setImageResource(R.drawable.album_pause);
                    playView.setAlpha(0f);
                }

                @Override
                public void onStop() {
                    playView.setImageResource(R.drawable.album_play);
                    playView.setAlpha(1f);
                }

                @Override
                public void onPrepared(int width, int height) {
                    setSurfaceSize(textureView, width, height);
                    playView.setAlpha(1f);
                    state = VideoState.READY;
                }

                @Override
                public void onVideoSizeChanged(int width, int height) {
                    setSurfaceSize(textureView, width, height);
                }

                @Override
                public void onCompletion() {
                    player.seekTo(0);
                    player.pause();
                    showPlayView();
                }

                @Override
                public void onError() {
                    playView.setVisibility(View.GONE);
                }
            };
        }
    }
}
