package io.github.album;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import io.github.album.ui.ClickHelper;
import io.github.album.ui.GridItemDecoration;
import io.github.album.ui.RoundedBottomProvider;

public final class AlbumActivity extends AppCompatActivity {
    private static final String TAG = "AlbumActivity";

    private static final long ANIM_DURATION = 300L;

    private TextView doneTv;
    private TextView previewTv;
    private View originalLayout;
    private ImageView originalIv;
    private TextView totalTv;

    private MediaItemAdapter itemAdapter;
    private TextView folderNameTv;
    private ImageView dropdownIv;
    private FrameLayout folderContainer;
    private RecyclerView folderRv;
    private FolderAdapter folderAdapter;

    private List<Folder> data;
    private Folder currentFolder;

    private QueryController queryController;

    private boolean isFolderShowing = false;
    private AnimatorSet hideFolderSet;
    private AnimatorSet showFolderSet;

    private long onStopTime;
    private long onResumeTime;

    private Preview preview;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(AlbumConfig.useCustomLayout ? AlbumConfig.customAlbumLayout : R.layout.album_activty);
        setWindowStatusBarColor();
        if (!Session.ready()) {
            // Should not be here
            LogProxy.e(TAG, new Exception("Session is not ready"));
            finishActivity();
            return;
        }
        initView();
    }

    private void setWindowStatusBarColor() {
        try {
            Window window = this.getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            int primaryColor = Utils.getColor(AlbumConfig.useCustomLayout ? AlbumConfig.customFolderListBgColor : R.color.album_primary);
            int vis = AlbumConfig.useCustomLayout && AlbumConfig.useDarkStatusIcon ? window.getDecorView().getSystemUiVisibility() | View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR : 0;
            window.getDecorView().setSystemUiVisibility(vis);
            window.setStatusBarColor(primaryColor);
            window.setNavigationBarColor(primaryColor);
        } catch (Exception ignore) {
        }
    }

    private void confirm() {
        Session.confirm();
        finishActivity();
    }

    private void initView() {
        ClickHelper.listen(findViewById(R.id.album_close_iv), this::finishActivity);

        ClickHelper.listen(findViewById(R.id.album_done_tv), this::confirm);

        folderContainer = findViewById(R.id.folder_list_container);
        folderNameTv = findViewById(R.id.folder_name_tv);
        dropdownIv = findViewById(R.id.album_dropdown_iv);

        doneTv = findViewById(R.id.album_done_tv);
        originalLayout = findViewById(R.id.album_original);
        originalIv = findViewById(R.id.album_original_iv);
        totalTv = findViewById(R.id.album_total_tv);
        previewTv = findViewById(R.id.album_preview_tv);

        folderContainer.setOnClickListener(v -> {
            if (isFolderShowing) {
                hideFolder();
            }
        });

        ClickHelper.listen(findViewById(R.id.select_folder_layout), () -> {
            if (isFolderShowing) {
                hideFolder();
            } else {
                showFolder();
            }
        });

        ClickHelper.listen(previewTv, () -> {
            List<MediaData> selectedList = Session.result.selectedList;
            if (!selectedList.isEmpty()) {
                getPreview().show(new ArrayList<>(selectedList), selectedList.get(0));
            }
        });

        ClickHelper.listen(originalLayout, () -> {
            Session.result.toggleOriginalFlag();
            updateOriginalView();
        });

        RecyclerView mediaRv = findViewById(R.id.album_media_rv);
        mediaRv.setLayoutManager(new GridLayoutManager(this, 4));
        mediaRv.addItemDecoration(new GridItemDecoration(4, Utils.dp2px(1.5f)));
        itemAdapter = new MediaItemAdapter(this, new MediaItemAdapter.EventListener() {
            @Override
            public void onInsertedFront() {
                mediaRv.scrollToPosition(0);
            }

            @Override
            public void onItemClick(MediaData item) {
                getPreview().show(currentFolder.mediaList, item);
            }

            @Override
            public void onSelectedChange() {
                updateViews();
            }
        });
        itemAdapter.setHasStableIds(true);
        mediaRv.setAdapter(itemAdapter);
        mediaRv.setItemAnimator(AlbumConfig.itemAnimator);

        updateViews();

        queryController = new QueryController(this::setData, this::refreshData, this::removeDeleted);

        MediaLoader.start(Session.request, queryController);
    }

    private void setData(List<Folder> result) {
        // Return an "All" folder with empty mediaList as least,
        // Not impossible to be empty.
        if (!result.isEmpty()) {
            data = result;
            updateFolder(result.get(0));
        }
    }

    private void updateFolder(Folder folder) {
        currentFolder = folder;
        folderNameTv.setText(folder.name);
        itemAdapter.update(folder.mediaList);
        if (isFolderShowing) {
            hideFolder();
        }
    }

    private void refreshData(List<Folder> result) {
        LogProxy.d(TAG, "Refresh data");
        update(result, true);
    }

    private void updateData(List<Folder> result) {
        LogProxy.d(TAG, "Update data");
        update(result, false);
    }

    private void update(List<Folder> result, boolean isRefresh) {
        // Update selected
        List<MediaData> selectedList = Session.result.selectedList;
        boolean selectedChanged = false;
        if (!selectedList.isEmpty()) {
            List<MediaData> totalList = result.get(0).mediaList;
            if (selectedList.size() < 5) {
                selectedChanged = selectedList.retainAll(totalList);
            } else {
                selectedChanged = selectedList.retainAll(new HashSet<>(totalList));
            }
        }

        // Update folder
        data.clear();
        data.addAll(result);
        if (folderAdapter != null) {
            folderAdapter.update(data);
        }

        // Update gallery
        currentFolder = findMatchedFolder(result);
        folderNameTv.setText(currentFolder.name);
        if (isRefresh || selectedChanged || AlbumConfig.itemAnimator == null) {
            itemAdapter.update(currentFolder.mediaList);
        } else {
            itemAdapter.rangeUpdate(currentFolder.mediaList);
        }
    }

    private Folder findMatchedFolder(List<Folder> result) {
        if (currentFolder != null) {
            for (Folder folder : result) {
                if (folder.name.equals(currentFolder.name)) {
                    return folder;
                }
            }
        }
        return result.get(0);
    }

    private void removeDeleted(Set<MediaData> deleted) {
        if (deleted.isEmpty()) {
            return;
        }
        boolean selectedChanged = Session.result.selectedList.retainAll(deleted);
        Iterator<Folder> it = data.listIterator();
        while (it.hasNext()) {
            Folder folder = it.next();
            boolean changed = folder.mediaList.removeAll(deleted);
            if (folder == currentFolder && (selectedChanged || changed)) {
                itemAdapter.refreshUI();
            }
            if (folder.mediaList.isEmpty() && data.size() > 1) {
                it.remove();
            }
        }
        if (currentFolder.mediaList.isEmpty()) {
            updateFolder(data.get(0));
        }
        if (folderAdapter != null) {
            folderAdapter.refreshUI();
        }
    }

    private boolean isSelected(Folder folder) {
        return currentFolder == folder;
    }

    private void initFolder() {
        folderRv = new RecyclerView(this);
        int folderBgColor = AlbumConfig.useCustomLayout ? AlbumConfig.customFolderListBgColor : R.color.album_primary;
        folderRv.setBackgroundColor(this.getResources().getColor(folderBgColor));
        folderRv.setLayoutManager(new LinearLayoutManager(this));
        folderRv.setOutlineProvider(new RoundedBottomProvider(Utils.dp2px(10)));
        folderRv.setClipToOutline(true);
        folderAdapter = new FolderAdapter(this, this::isSelected, this::updateFolder);
        folderRv.setAdapter(folderAdapter);
        ViewGroup.MarginLayoutParams mlp = new ViewGroup.MarginLayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        mlp.bottomMargin = Utils.dp2px(120);
        folderContainer.addView(folderRv, mlp);
        folderAdapter.update(data);
    }

    private void showFolder() {
        if (folderRv == null) {
            folderContainer.setVisibility(View.INVISIBLE);
            initFolder();
            // Start animation after folderRv inflating,
            // In that time we can get height of folderRv to do the translation animation.
            folderContainer.post(this::startShowAnimate);
        } else {
            startShowAnimate();
        }
    }

    private void startShowAnimate() {
        if (hasAnimation()) {
            return;
        }
        folderContainer.setVisibility(View.VISIBLE);
        if (showFolderSet == null) {
            showFolderSet = new AnimatorSet();
            showFolderSet.playTogether(
                    ObjectAnimator.ofFloat(folderRv, View.TRANSLATION_Y, -folderRv.getHeight(), 0F),
                    ObjectAnimator.ofFloat(dropdownIv, View.ROTATION, 0F, 180F),
                    ObjectAnimator.ofFloat(folderContainer, View.ALPHA, 0F, 1F)
            );
            showFolderSet.setDuration(ANIM_DURATION).start();
        } else {
            showFolderSet.start();
        }
        isFolderShowing = true;
    }

    private void hideFolder() {
        if (hasAnimation()) {
            return;
        }
        if (hideFolderSet == null) {
            hideFolderSet = new AnimatorSet();
            hideFolderSet.playTogether(
                    ObjectAnimator.ofFloat(folderRv, View.TRANSLATION_Y, 0F, -folderRv.getHeight()),
                    ObjectAnimator.ofFloat(dropdownIv, View.ROTATION, 180F, 360F),
                    ObjectAnimator.ofFloat(folderContainer, View.ALPHA, 1F, 0F)
            );
            hideFolderSet.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    folderContainer.setVisibility(View.GONE);
                }
            });
            hideFolderSet.setDuration(ANIM_DURATION).start();
        } else {
            hideFolderSet.start();
        }
        isFolderShowing = false;
    }

    private boolean hasAnimation() {
        return (showFolderSet != null && (showFolderSet.isStarted())) ||
                (hideFolderSet != null && (hideFolderSet.isStarted()));
    }


    void updateOriginalView() {
        if (Session.request.enableOriginal) {
            originalLayout.setVisibility(View.VISIBLE);

            if (Session.result.originalFlag) {
                originalIv.setImageResource(R.drawable.album_bg_original_p);
                long totalSize = Session.result.getTotalSize();
                if (totalSize == 0L) {
                    totalTv.setVisibility(View.GONE);
                } else {
                    totalTv.setVisibility(View.VISIBLE);
                    totalTv.setText(Utils.getFormatSize(totalSize));
                }
            } else {
                originalIv.setImageResource(R.drawable.album_bg_original_n);
                totalTv.setVisibility(View.GONE);
            }
        } else {
            if (originalLayout.getVisibility() != View.GONE) {
                originalLayout.setVisibility(View.GONE);
            }
        }
    }

    private void updateViews() {
        boolean enable = !Session.result.selectedList.isEmpty();
        doneTv.setEnabled(enable);
        doneTv.setText(Session.getDoneText());
        previewTv.setEnabled(enable);
        itemAdapter.refreshUI();
        updateOriginalView();
    }

    private Preview getPreview() {
        if (preview == null) {
            ViewGroup container = findViewById(R.id.album_preview_container);
            preview = new Preview(container, new Preview.PreviewEventListener() {
                @Override
                public void onClose() {
                    updateViews();
                    checkUpdate();
                }

                @Override
                public void onConfirm() {
                    confirm();
                }
            });
        }
        return preview;
    }

    @Override
    protected void onStop() {
        super.onStop();
        onStopTime = System.currentTimeMillis();
    }

    @Override
    protected void onResume() {
        super.onResume();
        onResumeTime = System.currentTimeMillis();
        if (preview == null || !preview.isShowing()) {
            checkUpdate();
        }
    }

    private void checkUpdate() {
        if (AlbumConfig.doResumeChecking
                && onStopTime != 0L
                && onResumeTime != 0L
                && data != null
                && queryController != null
                && queryController.isFinished) {
            if ((onResumeTime - onStopTime) > 3000L) {
                LogProxy.d(TAG, "Start checking update");
                queryController = new QueryController(null, this::updateData, null);
                MediaLoader.start(Session.request, queryController);
            }
        }
        onStopTime = 0L;
        onResumeTime = 0L;
    }

    @Override
    public void onBackPressed() {
        if (preview != null && preview.isShowing()) {
            preview.close();
        } else if (isFolderShowing) {
            hideFolder();
        } else {
            finishActivity();
        }
    }

    /**
     * AlbumActivity is not support switch portrait/landscape now.
     * If some day you need to support switch portrait/landscape,
     * you may need to move 'clearSession()' from 'onDestroy' to 'finishActivity()',
     * and override 'onSaveInstanceState()' to save key data before activity destroy,
     * and change the UI params by orientation.
     */
    @Override
    protected void onDestroy() {
        clearSession();
        super.onDestroy();
    }

    private void finishActivity() {
        // clearSession()
        finish();
    }

    private void clearSession() {
        // Set references to be null, to avoid memory leaks.
        if (queryController != null) {
            queryController.clear();
            queryController = null;
        }
        Session.clear();
    }
}
