package io.github.easyalbum.util

import android.content.Intent
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.ActivityResultRegistry
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner

class ActivityResultObserver(
    private val key: String,
    private val registry: ActivityResultRegistry,
    private val callback: (ActivityResult?) -> Unit)
    : DefaultLifecycleObserver {
    private var launcher: ActivityResultLauncher<Intent>? = null

    override fun onCreate(owner: LifecycleOwner) {
        launcher = registry.register(key, ActivityResultContracts.StartActivityForResult(), callback)
    }

    override fun onDestroy(owner: LifecycleOwner) {
        launcher?.unregister()
    }

    fun launch(intent: Intent) {
        launcher?.launch(intent)
    }
}
