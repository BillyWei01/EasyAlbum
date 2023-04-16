package io.github.album;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import io.github.album.FolderAdapter.FolderViewHolder;

final class FolderAdapter extends RecyclerView.Adapter<FolderViewHolder> {
    private final ArrayList<Folder> data = new ArrayList<>();
    private final LayoutInflater inflater;
    private final StringBuilder builder = new StringBuilder();
    private final SelectedCallback callback;
    private final SelectFolderListener listener;

    interface SelectedCallback {
        boolean isSelected(Folder folder);
    }

    interface SelectFolderListener {
        void onSelect(Folder folder);
    }

    FolderAdapter(Context context, SelectedCallback callback, SelectFolderListener listener) {
        this.inflater = LayoutInflater.from(context);
        this.callback = callback;
        this.listener = listener;
    }

    public void update(List<Folder> list) {
        data.clear();
        if (list != null && !list.isEmpty()) {
            data.addAll(list);
        }
        refreshUI();
    }

    @android.annotation.SuppressLint("NotifyDataSetChanged")
    public void refreshUI() {
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public FolderViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        int layout = AlbumConfig.style.folderItemLayout;
        FolderViewHolder holder =  new FolderViewHolder(inflater.inflate(layout, parent, false));
        holder.itemView.setOnClickListener(v -> {
            listener.onSelect(holder.folder);
            refreshUI();
        });
        return holder;
    }

    @Override
    public void onBindViewHolder(@NonNull FolderViewHolder holder, int position) {
        Folder folder = data.get(position);
        holder.folder = folder;
        List<MediaData> list = folder.mediaList;
        if (list.isEmpty()) {
            holder.folderCoverIv.setImageDrawable(null);
        } else {
            AlbumConfig.imageLoader.loadThumbnail(list.get(0), holder.folderCoverIv, true);
        }
        holder.folderTitleTv.setText(folder.name);
        holder.mediaCountTv.setText(getCountStr(list.size()));
        holder.checkFolderIv.setVisibility(callback.isSelected(folder) ? View.VISIBLE : View.GONE);
    }

    private String getCountStr(int size) {
        builder.setLength(0);
        builder.append('(').append(size).append(')');
        return builder.toString();
    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    public static class FolderViewHolder extends RecyclerView.ViewHolder {
        Folder folder;
        final ImageView folderCoverIv;
        final TextView folderTitleTv;
        final TextView mediaCountTv;
        final ImageView checkFolderIv;

        FolderViewHolder(View itemView) {
            super(itemView);
            folderCoverIv = itemView.findViewById(R.id.folder_cover_iv);
            folderTitleTv = itemView.findViewById(R.id.folder_title_tv);
            mediaCountTv = itemView.findViewById(R.id.media_count_tv);
            checkFolderIv = itemView.findViewById(R.id.check_folder_iv);
        }
    }
}
