package io.github.album;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.graphics.Rect;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager.OnPageChangeListener;

import java.util.List;

import io.github.album.PreviewItem.ImageItem;
import io.github.album.PreviewItem.VideoItem;
import io.github.album.ui.ClickHelper;
import io.github.album.ui.PreviewViewPager;

final class Preview {
    private static final String TAG = "Preview";

    private static final long ANIM_DURATION = 250L;

    private static final int SELECTED_HORIZON_MARGIN = Utils.dp2px(5f);
    private static final int SELECTED_VERTICAL_MARGIN = Utils.dp2px(13.5f);

    final ViewGroup previewContainer;
    final PreviewEventListener eventListener;
    final View.OnClickListener previewClickListener;

    private View rootView;
    private PreviewHolder holder;
    private int currentIndex = 0;
    private SelectedAdapter selectedAdapter;
    List<MediaData> mediaList;

    private boolean isTitleShowing = true;
    private AnimatorSet hideTitleSet;
    private AnimatorSet showTitleSet;

    /**
     * 是否是选中预览模式，该模式下底部选中item变为未选中态会有白色遮罩。
     * 非预览模式，则是会把未选中item移除
     */
    private boolean selectedPreview = false;

    interface PreviewEventListener {
        void onClose();

        void onConfirm();
    }

    Preview(ViewGroup container, PreviewEventListener listener) {
        this.previewContainer = container;
        this.eventListener = listener;
        this.previewClickListener = v -> toggleTitle();
    }

    Context getContext() {
        return previewContainer.getContext();
    }

    private class PreviewHolder {
        final View previewHeader;
        final View previewBottom;
        final View backIv;
        final TextView titleTv;
        final View originalLayout;
        final ImageView originalIv;
        final TextView totalTv;
        final TextView doneTv;
        final View selectLayout;
        final ImageView selectIv;
        final RecyclerView selectedRv;
        final PreviewViewPager viewPager;

        PreviewHolder(View root) {
            previewHeader = root.findViewById(R.id.album_preview_header);
            previewBottom = root.findViewById(R.id.album_preview_bottom);
            backIv = root.findViewById(R.id.album_preview_back_iv);
            titleTv = root.findViewById(R.id.album_preview_title_tv);
            originalLayout = root.findViewById(R.id.album_preview_original);
            originalIv = originalLayout.findViewById(R.id.album_preview_original_iv);
            totalTv = originalLayout.findViewById(R.id.album_preview_total_tv);
            doneTv = root.findViewById(R.id.album_preview_done_tv);
            selectLayout = root.findViewById(R.id.album_preview_select);
            selectIv = root.findViewById(R.id.album_preview_select_iv);
            selectedRv = root.findViewById(R.id.album_preview_selected_rv);
            viewPager = root.findViewById(R.id.album_preview_view_pager);
        }

        void updateOriginalView() {
            if (Session.request.enableOriginal) {
                if (originalLayout.getVisibility() != View.VISIBLE) {
                    originalLayout.setVisibility(View.VISIBLE);
                }
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

        void updateSelectIv() {
            if (currentIndex >= mediaList.size()) {
                return;
            }
            MediaData mediaData = mediaList.get(currentIndex);
            List<MediaData> selectedList = Session.result.selectedList;
            boolean isSelected = selectedList.contains(mediaData);
            if (isSelected) {
                selectIv.setImageResource(R.drawable.album_preview_select_p);
            } else {
                selectIv.setImageResource(R.drawable.album_preview_select_n);
            }
        }

        void updateDoneTv() {
            doneTv.setEnabled(!Session.result.selectedList.isEmpty());
            doneTv.setText(Session.getDoneText());
        }

        void updateTitleTv() {
            String text = "" + (currentIndex + 1) + '/' + mediaList.size();
            titleTv.setText(text);
        }

        void scrollSelectRv() {
            if (currentIndex >= mediaList.size()) {
                return;
            }
            MediaData mediaData = mediaList.get(currentIndex);
            List<MediaData> selectedList = Session.result.selectedList;
            int position = selectedList.indexOf(mediaData);
            if (position > -1) {
                selectedRv.scrollToPosition(position);
            }
        }
    }

    void show(List<MediaData> mediaList, MediaData item, boolean selectedPreview) {
        if (rootView == null) {
            this.selectedPreview = selectedPreview;
            initView(mediaList, item);
        }
    }

    void close() {
        if (rootView != null) {
            previewContainer.setVisibility(View.GONE);
            previewContainer.removeView(rootView);
            rootView = null;
            holder = null;
            mediaList = null;
            selectedAdapter = null;
            hideTitleSet = null;
            showTitleSet = null;
            eventListener.onClose();
        }
    }

    boolean isShowing() {
        return rootView != null;
    }

    private void initView(List<MediaData> list, MediaData item) {
        this.mediaList = list;
        currentIndex = Math.max(mediaList.indexOf(item), 0);
        View root = LayoutInflater.from(previewContainer.getContext())
                .inflate(AlbumConfig.style.previewLayout, previewContainer, false);

        rootView = root;
        previewContainer.setVisibility(View.VISIBLE);
        previewContainer.addView(root);
        holder = new PreviewHolder(root);

        // In case of touch events passing through the preview view tree.
        root.setOnClickListener(v -> LogProxy.d(TAG, "click preview"));

        initPager();
        initViews();
        initSelectedRv();

        ClickHelper.listen(holder.backIv, this::close);
        ClickHelper.listen(holder.doneTv, eventListener::onConfirm);
        ClickHelper.listen(holder.originalLayout, () -> {
            Session.result.toggleOriginalFlag();
            holder.updateOriginalView();
        });
        ClickHelper.listen(holder.selectLayout, () -> {
            if (currentIndex < mediaList.size()) {
                MediaData mediaData = mediaList.get(currentIndex);
                if (Session.selectItem(mediaData) && Session.request.limit > 1) {
                    if (Session.result.selectedList.isEmpty()) {
                        holder.selectedRv.setVisibility(View.GONE);
                    } else {
                        holder.selectedRv.setVisibility(View.VISIBLE);
                    }
                    selectedAdapter.updateMedia(mediaData, selectedPreview);
                }
            }
            holder.updateOriginalView();
            holder.updateSelectIv();
            holder.updateDoneTv();
            selectedAdapter.refreshUI();
        });
    }

    private void initViews() {
        holder.updateOriginalView();
        holder.updateSelectIv();
        holder.updateDoneTv();
        holder.updateTitleTv();
    }

    private void initSelectedRv() {
        selectedAdapter = new SelectedAdapter(this, Session.result.selectedList);
        selectedAdapter.setHasStableIds(true);
        RecyclerView selectedRv = holder.selectedRv;
        selectedRv.setLayoutManager(new LinearLayoutManager(getContext(), RecyclerView.HORIZONTAL, false));
        selectedRv.addItemDecoration(new RecyclerView.ItemDecoration() {
            @Override
            public void getItemOffsets(
                    @NonNull Rect outRect,
                    @NonNull View view,
                    @NonNull RecyclerView parent,
                    @NonNull RecyclerView.State state
            ) {
                outRect.left = SELECTED_HORIZON_MARGIN;
                outRect.right = SELECTED_HORIZON_MARGIN;
                outRect.top = SELECTED_VERTICAL_MARGIN;
            }
        });
        selectedRv.setAdapter(selectedAdapter);
        if (Session.result.selectedList.isEmpty() || Session.request.limit == 1) {
            selectedRv.setVisibility(View.GONE);
        } else {
            selectedRv.setVisibility(View.VISIBLE);
        }
        holder.scrollSelectRv();
    }

    private void toggleTitle() {
        if (hasAnimation()) {
            return;
        }
        if (isTitleShowing) {
            hideTitle();
        } else {
            showTitle();
        }
    }

    private void showTitle() {
        if (showTitleSet == null) {
            int headerHeight = holder.previewHeader.getHeight();
            int bottomHeight = holder.previewBottom.getHeight();
            showTitleSet = new AnimatorSet();
            showTitleSet.playTogether(
                    ObjectAnimator.ofFloat(holder.previewHeader, View.TRANSLATION_Y, -headerHeight, 0F),
                    ObjectAnimator.ofFloat(holder.previewBottom, View.TRANSLATION_Y, bottomHeight, 0F),
                    ObjectAnimator.ofFloat(holder.selectedRv, View.ALPHA, 0F, 1F)
            );
            showTitleSet.setDuration(ANIM_DURATION).start();
        } else {
            showTitleSet.start();
        }
        isTitleShowing = true;
    }

    private void hideTitle() {
        if (hideTitleSet == null) {
            int headerHeight = holder.previewHeader.getHeight();
            int bottomHeight = holder.previewBottom.getHeight();
            hideTitleSet = new AnimatorSet();
            hideTitleSet.playTogether(
                    ObjectAnimator.ofFloat(holder.previewHeader, View.TRANSLATION_Y, 0F, -headerHeight),
                    ObjectAnimator.ofFloat(holder.previewBottom, View.TRANSLATION_Y, 0F, bottomHeight),
                    ObjectAnimator.ofFloat(holder.selectedRv, View.ALPHA, 1F, 0F)
            );
            hideTitleSet.setDuration(ANIM_DURATION).start();
        } else {
            hideTitleSet.start();
        }
        isTitleShowing = false;
    }

    private boolean hasAnimation() {
        return (showTitleSet != null && (showTitleSet.isStarted())) ||
                (hideTitleSet != null && (hideTitleSet.isStarted()));
    }

    void selectedItem(MediaData item) {
        int index = mediaList.indexOf(item);
        if (index < 0) {
            return;
        }
        currentIndex = index;
        holder.viewPager.setCurrentItem(index);
    }

    MediaData getCurrentMedia() {
        return currentIndex < mediaList.size() ? mediaList.get(currentIndex) : null;
    }

    private void initPager() {
        PagerAdapter adapter = getPagerAdapter();
        PreviewViewPager viewPager = holder.viewPager;
        viewPager.setAdapter(adapter);
        viewPager.setCurrentItem(currentIndex);
        viewPager.setOffscreenPageLimit(2);

        viewPager.addOnPageChangeListener(new OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
            }

            @Override
            public void onPageSelected(int position) {
                if (currentIndex != position && currentIndex < mediaList.size()) {
                    MediaData preMedia = mediaList.get(currentIndex);
                    int childCount = holder.viewPager.getChildCount();
                    for (int i = 0; i < childCount; i++) {
                        View view = holder.viewPager.getChildAt(i);
                        Object tag = view.getTag();
                        if (tag instanceof VideoItem) {
                            VideoItem videoItem = (VideoItem) tag;
                            if (videoItem.media == preMedia) {
                                videoItem.pause();
                                break;
                            }
                        }
                    }
                }
                currentIndex = position;
                holder.updateSelectIv();
                holder.updateTitleTv();
                selectedAdapter.refreshUI();
                holder.scrollSelectRv();
            }

            @Override
            public void onPageScrollStateChanged(int state) {
            }
        });
    }

    private PagerAdapter getPagerAdapter() {
        return new PagerAdapter() {
            @NonNull
            @Override
            public Object instantiateItem(@NonNull ViewGroup container, int position) {
                MediaData media = mediaList.get(position);
                PreviewItem viewItem = getViewItem(container, media);
                container.addView(viewItem.root);
                viewItem.show();
                viewItem.onClickListener = previewClickListener;
                return viewItem;
            }

            @Override
            public void destroyItem(@NonNull ViewGroup container, int position, @NonNull Object object) {
                if (object instanceof PreviewItem) {
                    PreviewItem item = (PreviewItem) object;
                    container.removeView(item.root);
                    item.root.setTag(null);
                }
            }

            @Override
            public int getCount() {
                return mediaList.size();
            }

            @Override
            public boolean isViewFromObject(@NonNull View view, @NonNull Object object) {
                if (object instanceof PreviewItem) {
                    return view == ((PreviewItem) object).root;
                }
                return view == object;
            }
        };
    }

    private PreviewItem getViewItem(ViewGroup container, MediaData media) {
        if (media.isVideo) {
            return new VideoItem(container, media);
        } else {
            return new ImageItem(container, media);
        }
    }
}
