package io.github.easyalbum.util

import android.content.pm.PackageManager
import android.os.Process
import io.github.easyalbum.application.GlobalConfig

object PermissionUtil {
    fun hasPermissions(vararg permissions: String): Boolean {
        val context = GlobalConfig.appContext
        val pid = Process.myPid();
        val uid = Process.myUid()
        for (permission in permissions) {
            if (context.checkPermission(permission, pid, uid) != PackageManager.PERMISSION_GRANTED) {
                return false
            }
        }
        return true
    }

    fun allGran(grantResults: IntArray): Boolean {
        for (result in grantResults) {
            if (result != PackageManager.PERMISSION_GRANTED) {
                return false
            }
        }
        return true
    }

}

