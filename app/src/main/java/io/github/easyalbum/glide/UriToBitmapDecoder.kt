package io.github.easyalbum.glide

import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Size
import com.bumptech.glide.load.Option
import com.bumptech.glide.load.Options
import com.bumptech.glide.load.ResourceDecoder
import com.bumptech.glide.load.data.mediastore.MediaStoreUtil
import com.bumptech.glide.load.engine.Resource
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool
import com.bumptech.glide.load.resource.bitmap.BitmapResource
import io.github.easyalbum.application.GlobalConfig
import io.github.easyalbum.util.LogUtil

class UriToBitmapDecoder(private val bitmapPool: BitmapPool) : ResourceDecoder<Uri, Bitmap> {
    companion object {
        val THUMBNAIL_MEMORY: Option<Byte> = Option.memory("thumbnail_memory", -1)
        val THUMBNAIL_DISK: Option<Byte> = Option.disk("thumbnail_disk", -1) { keyBytes, _, digest ->
            digest.update(keyBytes)
        }
    }

    override fun handles(source: Uri, options: Options): Boolean {
        return MediaStoreUtil.isMediaStoreUri(source)
            && options[THUMBNAIL_MEMORY] != null
            && options[THUMBNAIL_DISK] != null
    }

    override fun decode(source: Uri, width: Int, height: Int, options: Options): Resource<Bitmap>? {
        val contentResolver = GlobalConfig.appContext.contentResolver ?: return null
        var bitmap: Bitmap? = null
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                try {
                    bitmap = contentResolver.loadThumbnail(source, Size(width, height), null)
                } catch (ignore: Exception) {
                }
            }
            if (bitmap == null) {
                val path = source.toString()
                val index = path.lastIndexOf('/')
                if (index > 0) {
                    val mediaId = path.substring(index + 1).toLong()
                    bitmap = MediaStore.Video.Thumbnails.getThumbnail(
                        contentResolver,
                        mediaId,
                        MediaStore.Video.Thumbnails.MINI_KIND,
                        null
                    )
                }
            }
            if (bitmap != null) {
                return BitmapResource.obtain(bitmap, bitmapPool)
            }
        } catch (e: Throwable) {
            LogUtil.e("UriToBitmapDecoder", e)
        }
        return null
    }
}
