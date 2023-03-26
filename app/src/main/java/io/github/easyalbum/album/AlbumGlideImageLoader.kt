package io.github.easyalbum.album

import android.annotation.SuppressLint
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.bitmap.DownsampleStrategy
import io.github.album.MediaData
import io.github.album.interfaces.ImageLoader
import io.github.easyalbum.glide.UriToBitmapDecoder

object AlbumGlideImageLoader : ImageLoader {
    private const val DISK_CACHE_LIMIT = 300

    var count = 0

    @SuppressLint("CheckResult")
    override fun loadPreview(data: MediaData, imageView: ImageView, asBitmap: Boolean) {
        val requestBuilder = if (asBitmap) {
            Glide.with(imageView).asBitmap()
        } else if (data.isGif) {
            Glide.with(imageView).asGif()
        } else {
            Glide.with(imageView).asDrawable()
        }
        requestBuilder.load(data.properUri)
            .downsample(DownsampleStrategy.CENTER_INSIDE)
            .diskCacheStrategy(DiskCacheStrategy.NONE)
            .skipMemoryCache(true)
            .into(imageView)
    }

    @SuppressLint("CheckResult")
    override fun loadThumbnail(data: MediaData, imageView: ImageView, asBitmap: Boolean) {
        count++
        val requestBuilder = if(asBitmap){
            Glide.with(imageView).asBitmap()
        } else if (data.isGif) {
            Glide.with(imageView).asGif()
        } else {
            Glide.with(imageView).asDrawable()
        }
        requestBuilder.load(data.uri)
            .apply {
                if (asBitmap || !data.isGif) {
                    // There are two intentions to set options.
                    // 1. To make UriToBitmapDecoder's 'handle()' return true.
                    // 2. To make cache key different with normal request (like load image to preview).
                    set(UriToBitmapDecoder.THUMBNAIL_MEMORY, 0)
                    set(UriToBitmapDecoder.THUMBNAIL_DISK, 0)
                }
                if (count > DISK_CACHE_LIMIT) {
                    diskCacheStrategy(DiskCacheStrategy.NONE)
                }
            }
            .into(imageView)
    }
}
