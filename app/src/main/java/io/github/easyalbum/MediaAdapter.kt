package io.github.easyalbum

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import io.github.album.MediaData

class MediaAdapter(val context: Context) : RecyclerView.Adapter<MediaAdapter.MediaHolder>() {
    private val mediaList = ArrayList<MediaData>()

    inner class MediaHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        internal val itemIv: ImageView = itemView.findViewById(R.id.result_item_iv)
    }

    @SuppressLint("NotifyDataSetChanged")
    fun setData(data: List<MediaData>) {
        mediaList.clear()
        mediaList.addAll(data)
        notifyDataSetChanged()
    }

    @SuppressLint("NotifyDataSetChanged")
    fun clearData() {
        mediaList.clear()
        notifyDataSetChanged()
    }

    fun getData(): List<MediaData>{
        return mediaList
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MediaHolder {
        val itemView = LayoutInflater.from(context).inflate(R.layout.layout_result_item, parent, false)
        return MediaHolder(itemView)
    }

    override fun onBindViewHolder(holder: MediaHolder, position: Int) {
        if (position >= mediaList.size) return
        val item = mediaList[position]
        Glide.with(holder.itemIv)
            .load(item.properUri)
            .into(holder.itemIv)
    }

    override fun getItemCount(): Int {
        return mediaList.size
    }
}