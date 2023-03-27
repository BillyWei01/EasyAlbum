package io.github.easyalbum.album

import android.graphics.Bitmap
import android.os.Build
import android.widget.ImageView
import io.github.album.MediaData
import io.github.album.interfaces.ImageLoader
import io.github.doodle.enums.DiskCacheStrategy
import io.github.doodle.Doodle
import io.github.doodle.enums.ClipType
import io.github.doodle.enums.MemoryCacheStrategy

object DoodleImageLoader : ImageLoader {
    private const val DISK_CACHE_LIMIT = 300

    private val thumbnailEncodeFormat = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
        Bitmap.CompressFormat.WEBP_LOSSY else Bitmap.CompressFormat.JPEG

    private var count = 0

    override fun loadPreview(data: MediaData, imageView: ImageView, asBitmap: Boolean) {
        Doodle.load(data.properUri)
            .diskCacheStrategy(DiskCacheStrategy.NONE)
            .memoryCacheStrategy(MemoryCacheStrategy.WEAK)
            .clipType(ClipType.NO_CLIP)
            .asBitmap(asBitmap)
            .into(imageView)
    }

    override fun loadThumbnail(data: MediaData, imageView: ImageView, asBitmap: Boolean) {
        count++
        Doodle.load(data.uri)
            .apply {
                if (asBitmap || !data.isGif) {
                    enableThumbnailDecoder()
                }
                if (count > DISK_CACHE_LIMIT) {
                    diskCacheStrategy(DiskCacheStrategy.NONE)
                }
            }
            .encodeFormat(thumbnailEncodeFormat)
            .asBitmap(asBitmap)
            .into(imageView)
    }
}
