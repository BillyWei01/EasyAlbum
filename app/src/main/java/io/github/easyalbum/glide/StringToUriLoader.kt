package io.github.easyalbum.glide

import android.net.Uri
import com.bumptech.glide.Priority
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.Options
import com.bumptech.glide.load.data.DataFetcher
import com.bumptech.glide.load.model.ModelLoader
import com.bumptech.glide.load.model.ModelLoaderFactory
import com.bumptech.glide.load.model.MultiModelLoaderFactory
import com.bumptech.glide.signature.ObjectKey

class StringToUriLoader: ModelLoader<String, Uri> {
    class Factory : ModelLoaderFactory<String, Uri> {
        override fun build(multiFactory: MultiModelLoaderFactory): ModelLoader<String, Uri> {
            return StringToUriLoader()
        }

        override fun teardown() {
        }
    }

    override fun buildLoadData(
        model: String,
        width: Int,
        height: Int,
        options: Options
    ): ModelLoader.LoadData<Uri> {
        return ModelLoader.LoadData(
            ObjectKey(model),
            object : DataFetcher<Uri> {
                override fun loadData(
                    priority: Priority,
                    callback: DataFetcher.DataCallback<in Uri?>
                ) {
                    callback.onDataReady(Uri.parse(model))
                }

                override fun cleanup() {}
                override fun cancel() {}
                override fun getDataClass(): Class<Uri> {
                    return Uri::class.java
                }

                override fun getDataSource(): DataSource {
                    return DataSource.LOCAL
                }
            }
        )
    }

    override fun handles(model: String): Boolean {
        return model.startsWith("content://media/")
    }
}

