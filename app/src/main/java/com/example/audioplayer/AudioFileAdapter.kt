package com.example.audioplayer

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class AudioFileAdapter(
    private val audioFiles: List<AudioFile>,
    private val onItemClick: (Int) -> Unit
) : RecyclerView.Adapter<AudioFileAdapter.ViewHolder>() {

    private var currentPlayingIndex: Int = -1

    fun setCurrentPlaying(index: Int) {
        val oldIndex = currentPlayingIndex
        currentPlayingIndex = index
        if (oldIndex >= 0) notifyItemChanged(oldIndex)
        if (index >= 0) notifyItemChanged(index)
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvFileName: TextView = view.findViewById(R.id.tvFileName)
        val tvFilePath: TextView = view.findViewById(R.id.tvFilePath)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_audio, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val audioFile = audioFiles[position]
        holder.tvFileName.text = audioFile.name
        holder.tvFilePath.text = audioFile.path

        if (position == currentPlayingIndex) {
            holder.itemView.setBackgroundColor(0xFFE3F2FD.toInt())
        } else {
            holder.itemView.setBackgroundColor(0x00000000)
        }

        holder.itemView.setOnClickListener {
            onItemClick(position)
        }
    }

    override fun getItemCount(): Int = audioFiles.size
}
