package io.github.album;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import io.github.album.SelectedAdapter.SelectedViewHolder;

class SelectedAdapter extends RecyclerView.Adapter<SelectedViewHolder> {
    private final ArrayList<MediaData> data = new ArrayList<>();
    private final LayoutInflater inflater;
    private final Preview preview;

    SelectedAdapter(Preview preview, List<MediaData> list) {
        this.inflater = LayoutInflater.from(preview.getContext());
        this.preview = preview;
        data.addAll(list);
    }

    void updateMedia(MediaData mediaData, boolean selectedPreview) {
        if (selectedPreview) {
            if (!data.contains(mediaData)) {
                data.add(mediaData);
            }
        } else {
            List<MediaData> selectedList = Session.result.selectedList;
            if (selectedList.contains(mediaData)) {
                if (!data.contains(mediaData)) {
                    data.add(mediaData);
                }
            } else {
                data.remove(mediaData);
            }
        }
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
    public SelectedViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = inflater.inflate(R.layout.album_selected_media_item, parent, false);
        SelectedViewHolder holder = new SelectedViewHolder(itemView);
        holder.itemView.setOnClickListener(v -> preview.selectedItem(holder.item));
        return holder;
    }

    @Override
    public void onBindViewHolder(@NonNull SelectedViewHolder holder, int position) {
        MediaData item = data.get(position);
        holder.item = item;
        AlbumRequest request = Session.request;
        AlbumConfig.imageLoader.loadThumbnail(item, holder.itemIv, request.thumbnailAsBitmap);
        if (item.isVideo) {
            holder.cameraIv.setVisibility(View.VISIBLE);
        } else {
            holder.cameraIv.setVisibility(View.GONE);
        }
        List<MediaData> selectedList = Session.result.selectedList;
        if (selectedList.contains(item)) {
            holder.itemMask.setVisibility(View.GONE);
        } else {
            holder.itemMask.setVisibility(View.VISIBLE);
        }
        if (item == preview.getCurrentMedia()) {
            holder.itemFrame.setVisibility(View.VISIBLE);
        } else {
            holder.itemFrame.setVisibility(View.INVISIBLE);
        }
    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    static class SelectedViewHolder extends RecyclerView.ViewHolder {
        MediaData item;
        final ImageView itemIv;
        final View itemMask;
        final View itemFrame;
        final ImageView cameraIv;

        SelectedViewHolder(View itemView) {
            super(itemView);
            itemIv = itemView.findViewById(R.id.album_selected_item_iv);
            itemMask = itemView.findViewById(R.id.album_selected_item_mask);
            itemFrame = itemView.findViewById(R.id.album_selected_item_frame);
            cameraIv = itemView.findViewById(R.id.album_selected_item_camera);
        }
    }
}
