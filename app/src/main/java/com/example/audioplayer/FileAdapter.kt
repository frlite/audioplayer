package com.example.audioplayer

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class FileAdapter(
    private val items: List<FileItem>,
    private val onFolderClick: (FileItem.Folder) -> Unit,
    private val onAudioClick: (FileItem.Audio) -> Unit
) : RecyclerView.Adapter<FileAdapter.ViewHolder>() {

    private var currentPlayingPath: String? = null

    fun setCurrentPlaying(path: String?) {
        val oldPath = currentPlayingPath
        currentPlayingPath = path
        if (oldPath != null) {
            val oldIndex = items.indexOfFirst { it.path == oldPath }
            if (oldIndex >= 0) notifyItemChanged(oldIndex)
        }
        if (path != null) {
            val newIndex = items.indexOfFirst { it.path == path }
            if (newIndex >= 0) notifyItemChanged(newIndex)
        }
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivIcon: ImageView = view.findViewById(R.id.ivIcon)
        val tvName: TextView = view.findViewById(R.id.tvName)
        val tvPath: TextView = view.findViewById(R.id.tvPath)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_file, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.tvName.text = item.name
        holder.tvPath.text = item.path

        when (item) {
            is FileItem.Folder -> {
                holder.ivIcon.setImageResource(android.R.drawable.ic_menu_more)
                holder.itemView.setBackgroundColor(0x00000000)
                holder.itemView.setOnClickListener {
                    onFolderClick(item)
                }
            }
            is FileItem.Audio -> {
                holder.ivIcon.setImageResource(android.R.drawable.ic_media_play)
                if (item.path == currentPlayingPath) {
                    holder.itemView.setBackgroundColor(0xFFE3F2FD.toInt())
                } else {
                    holder.itemView.setBackgroundColor(0x00000000)
                }
                holder.itemView.setOnClickListener {
                    onAudioClick(item)
                }
            }
        }
    }

    override fun getItemCount(): Int = items.size
}
