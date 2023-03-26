package io.github.easyalbum.util

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.Executor

/**
 * Provide an executor for third party libraries, to reuse threads.
 */
object IOExecutor : Executor {
    override fun execute(r: Runnable) {
        CoroutineScope(Dispatchers.IO).launch {
            r.run()
        }
    }
}