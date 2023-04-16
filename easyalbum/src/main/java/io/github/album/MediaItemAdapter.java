package io.github.album;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import io.github.album.MediaItemAdapter.MediaItemViewHolder;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;


import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

class MediaItemAdapter extends RecyclerView.Adapter<MediaItemViewHolder> {
    private static final long ANIM_DURATION = 200L;

    private final ArrayList<MediaData> data = new ArrayList<>();
    private final LayoutInflater inflater;
    private final EventListener eventListener;
    private MediaData lastSelectedItem;

    interface EventListener {
        void onInsertedFront();
        void onItemClick(MediaData item);
        void onSelectedChange();
    }

    MediaItemAdapter(Context context, EventListener eventListener) {
        this.inflater = LayoutInflater.from(context);
        this.eventListener = eventListener;
    }

    public void update(List<MediaData> list) {
        data.clear();
        if (list != null && !list.isEmpty()) {
            data.addAll(list);
        }
        refreshUI();
    }

    public void rangeUpdate(List<MediaData> list) {
        if (list == null || list.isEmpty()) {
            update(list);
            return;
        }

        Set<MediaData> common = new HashSet<>(data);
        common.retainAll(new HashSet<>(list));
        int deletedCount = data.size() - common.size();
        int addedCount = list.size() - common.size();

        if (deletedCount > 0 && addedCount == 0) {
            int[] range = findChangedRange(data, common, deletedCount);
            data.clear();
            data.addAll(list);
            if (range != null) {
                notifyItemRangeRemoved(range[0], range[1]);
            } else {
                refreshUI();
            }
        } else {
            data.clear();
            data.addAll(list);
            if (deletedCount == 0 && addedCount > 0) {
                int[] range = findChangedRange(list, common, addedCount);
                if (range != null) {
                    notifyItemRangeInserted(range[0], range[1]);
                    if (range[0] == 0) {
                        eventListener.onInsertedFront();
                    }
                } else {
                    refreshUI();
                }
            } else if (deletedCount > 0 && addedCount > 0) {
                refreshUI();
            }
        }
    }

    private int[] findChangedRange(List<MediaData> list, Set<MediaData> common, int changeCount) {
        int n = list.size();
        int firstChanged = -1;
        for (int i = 0; i < n; i++) {
            if (!common.contains(list.get(i))) {
                if (changeCount == 1) {
                    return new int[]{i, 1};
                }
                if (firstChanged == -1) {
                    firstChanged = i;
                } else {
                    int count = i - firstChanged + 1;
                    if (count == changeCount) {
                        return new int[]{firstChanged, count};
                    }
                }
            } else {
                if (firstChanged != -1) {
                    // discontinuous
                    return null;
                }
            }
        }
        return null;
    }

    @android.annotation.SuppressLint("NotifyDataSetChanged")
    public void refreshUI() {
        notifyDataSetChanged();
    }

    @Override
    public long getItemId(int position) {
        if (position < data.size()) {
            return data.get(position).mediaId;
        } else {
            return super.getItemId(position);
        }
    }

    @NonNull
    @Override
    public MediaItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = inflater.inflate(AlbumConfig.style.albumItemLayout, parent, false);
        MediaItemViewHolder holder = new MediaItemViewHolder(itemView);
        holder.itemView.setOnClickListener(v -> eventListener.onItemClick(holder.item));
        holder.selectView.setOnClickListener(v -> {
            if (Session.selectItem(holder.item)) {
                if (AlbumConfig.doSelectedAnimation) {
                    lastSelectedItem = holder.item;
                }
                eventListener.onSelectedChange();
            } else {
                lastSelectedItem = null;
            }
        });
        return holder;
    }

    @android.annotation.SuppressLint("SetTextI18n")
    @Override
    public void onBindViewHolder(@NonNull MediaItemViewHolder holder, int position) {
        MediaData item = data.get(position);
        holder.item = item;
        AlbumRequest request = Session.request;
        AlbumConfig.imageLoader.loadThumbnail(item, holder.itemIv, request.thumbnailAsBitmap);
        if (item.isVideo) {
            holder.cameraIv.setVisibility(View.VISIBLE);
            holder.durationTv.setVisibility(View.VISIBLE);
            holder.durationTv.setText(Utils.getFormatTime(item.getDuration()));
            holder.gifIv.setVisibility(View.GONE);
        } else {
            holder.cameraIv.setVisibility(View.GONE);
            holder.durationTv.setVisibility(View.GONE);
            holder.gifIv.setVisibility(request.thumbnailAsBitmap && item.isGif() ? View.VISIBLE : View.GONE);
        }

        int selectedIndex = Session.result.selectedList.indexOf(item);
        if (selectedIndex >= 0) {
            if (request.limit > 1) {
                holder.selectTv.setBackgroundResource(R.drawable.album_bg_select_p);
                holder.selectTv.setText(Integer.toString(selectedIndex + 1));
            } else {
                holder.selectTv.setBackgroundResource(R.drawable.album_bg_select_radio);
            }
            holder.selectedMask.setVisibility(View.VISIBLE);
            if (item == lastSelectedItem) {
                lastSelectedItem = null;
                ObjectAnimator.ofFloat(holder.selectedMask, View.ALPHA, 0F, 1F)
                        .setDuration(ANIM_DURATION).start();
            }
        } else {
            holder.selectTv.setBackgroundResource(R.drawable.album_bg_select_n);
            if (request.limit > 1) {
                holder.selectTv.setText("");
            }
            if (item == lastSelectedItem) {
                lastSelectedItem = null;
                AnimatorSet animatorSet = new AnimatorSet();
                animatorSet.play(ObjectAnimator.ofFloat(holder.selectedMask, View.ALPHA, 1F, 0F));
                animatorSet.addListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        holder.selectedMask.setVisibility(View.GONE);
                    }
                });
                animatorSet.setDuration(ANIM_DURATION).start();
            } else {
                holder.selectedMask.setVisibility(View.GONE);
            }
        }
    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    public static class MediaItemViewHolder extends RecyclerView.ViewHolder {
        MediaData item;
        final ImageView itemIv;
        final TextView selectTv;
        final View selectView;
        final ImageView cameraIv;
        final View selectedMask;
        final TextView durationTv;
        final ImageView gifIv;

        MediaItemViewHolder(View itemView) {
            super(itemView);
            itemIv = itemView.findViewById(R.id.album_item_iv);
            selectTv = itemView.findViewById(R.id.album_select_tv);
            selectView = itemView.findViewById(R.id.album_select_v);
            cameraIv = itemView.findViewById(R.id.album_camera_iv);
            selectedMask = itemView.findViewById(R.id.album_selected_mask);
            durationTv = itemView.findViewById(R.id.album_duration_tv);
            gifIv = itemView.findViewById(R.id.album_gif_iv);
        }
    }
}
