package io.github.easyalbum.glide

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import com.bumptech.glide.module.AppGlideModule
import com.bumptech.glide.GlideBuilder
import com.bumptech.glide.Glide
import com.bumptech.glide.Registry
import com.bumptech.glide.annotation.GlideModule

@GlideModule
class MyAppGlideModule : AppGlideModule() {
    override fun applyOptions(context: Context, builder: GlideBuilder) {
    }

    override fun registerComponents(context: Context, glide: Glide, registry: Registry) {
        registry.prepend(
            String::class.java,
            Uri::class.java,
            StringToUriLoader.Factory()
        )
        registry.prepend(
            Uri::class.java,
            Uri::class.java,
            UriToUriLoader.Factory()
        )
        registry.prepend(
            Registry.BUCKET_BITMAP,
            Uri::class.java,
            Bitmap::class.java,
            UriToBitmapDecoder(glide.bitmapPool)
        )
    }
}