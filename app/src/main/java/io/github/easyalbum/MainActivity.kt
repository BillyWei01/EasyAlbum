package io.github.easyalbum

import android.Manifest
import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.Settings
import androidx.core.app.ActivityCompat
import androidx.recyclerview.widget.GridLayoutManager
import io.github.album.AlbumRequest
import io.github.album.EasyAlbum
import io.github.album.Folder
import io.github.album.MediaData
import io.github.album.interfaces.MediaFilter
import io.github.album.ui.GridItemDecoration
import io.github.easyalbum.application.GlobalConfig
import io.github.easyalbum.util.ActivityResultObserver
import io.github.easyalbum.util.PermissionUtil
import io.github.easyalbum.util.ToastUtil
import io.github.easyalbum.util.Utils
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {
    companion object {
        private const val RC_READ_WRITE_STORAGE = 1

        private val storagePermissions = arrayOf(
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        )
    }

    private val storageResultObserver by lazy {
        ActivityResultObserver("storage", activityResultRegistry) {
            if (PermissionUtil.hasPermissions(*storagePermissions)) {
                openAlbum()
            } else {
                ToastUtil.showTips("Permission deny")
            }
        }
    }

    private var mediaAdapter: MediaAdapter? = null

    private var selectLimit = 1
    private var option = Option.ALL

    enum class Option(val text: String) {
        ALL(getStr(R.string.all_medias)),
        VIDEO(getStr(R.string.all_videos)),
        IMAGE(getStr(R.string.all_images))
    }

    private class TestMediaFilter(private val opt: Option) : MediaFilter {
        override fun accept(media: MediaData): Boolean {
            return when (opt) {
                Option.VIDEO -> media.isVideo
                Option.IMAGE -> !media.isVideo
                else -> true
            }
        }

        override fun tag(): String {
            return "Filter${opt.text}"
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val context = this
        initLayoutStyle()

        multi_choice_btn.onClick {
            selectLimit = getMultiChoiceLimit()
            selectPhoto()
        }

        single_choice_btn.onClick {
            selectLimit = 1
            val count = mediaAdapter?.getData()?.size
            if (count != null && count > 1) {
                mediaAdapter?.clearData()
            }
            selectPhoto()
        }

        mediaAdapter = MediaAdapter(context)
        select_rv.apply {
            layoutManager = GridLayoutManager(context, 3)
            addItemDecoration(GridItemDecoration(3, Utils.dp2px(3f)));
            adapter = mediaAdapter
        }

        clear_selected_btn.onClick {
            mediaAdapter?.clearData()
        }
    }

    private fun getMultiChoiceLimit(): Int {
        var text = limit_title_et.text?.toString()
        if (text.isNullOrEmpty()) {
            text = limit_title_et.hint?.toString()
        }
        return if (text.isNullOrEmpty()) {
            1
        } else {
            kotlin.runCatching {
                Integer.parseInt(text).takeIf { it > 0 } ?: 1
            }.getOrDefault(1)
        }
    }

    private fun getOption(): Option {
        return if (radio_video.isChecked) {
            Option.VIDEO
        } else if (radio_image.isChecked) {
            Option.IMAGE
        } else {
            Option.ALL
        }
    }

    private fun initLayoutStyle(){
        if(radio_custom_white.isChecked) {
            EasyAlbum.config()
                .setCustomAlbumLayout(R.layout.activty_album_white_sample)
                .setCustomAlbumItemLayout(R.layout.adapter_media_item_white_sample)
                .setCustomFolderItemLayout(R.layout.adapter_folder_sample)
                .setCustomPreviewLayout(R.layout.activity_preview_white_sample)
                .setUseCustomLayout(true)
        }else{
            EasyAlbum.config().setUseCustomLayout(false)
        }
    }

    private fun selectPhoto() {
        option = getOption()
        if (PermissionUtil.hasPermissions(*storagePermissions)) {
            openAlbum()
        } else {
            ActivityCompat.requestPermissions(this, storagePermissions, RC_READ_WRITE_STORAGE)
        }
    }

    private val overLimitCallback = AlbumRequest.OverLimitCallback { limit ->
        ToastUtil.showTips(getStr(R.string.select_pictures_and_videos_limit, limit))
    }

    // Just make an example of
    // making one folder to the front of folder list.
    private val priorityFolderComparator = Comparator<Folder> { o1, o2 ->
        val priorityFolder = "Camera"
        if (o1.name == priorityFolder) -1
        else if (o2.name == priorityFolder) 1
        else o1.name.compareTo(o2.name)
    }

    private fun getFilter(opt: Option): MediaFilter? {
        return if (opt == Option.ALL) null else TestMediaFilter(option)
    }

    private fun openAlbum() {
        initLayoutStyle()
        EasyAlbum.from(this)
            .setFilter(getFilter(option))
            .setSelectedLimit(selectLimit)
            .setOverLimitCallback(overLimitCallback)
            .setSelectedList(mediaAdapter?.getData())
            .setAllString(option.text)
            .enableOriginal()
            //.setFolderComparator(priorityFolderComparator)
            .start { result ->
                mediaAdapter?.setData(result.selectedList)
            }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == RC_READ_WRITE_STORAGE) {
            if (PermissionUtil.allGran(grantResults)) {
                openAlbum()
            } else {
                showPermissionRequest()
            }
        }
    }

    private fun showPermissionRequest() {
        AlertDialog.Builder(this)
            .setTitle(R.string.storage_permission_request)
            .setPositiveButton(R.string.to_system_setting) { _, _ ->
                val packageUri = Uri.parse("package:${GlobalConfig.appContext.packageName}")
                storageResultObserver.launch(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, packageUri))
            }.show()
    }
}
