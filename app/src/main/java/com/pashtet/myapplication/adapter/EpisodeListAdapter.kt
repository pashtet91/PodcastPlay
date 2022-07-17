package com.pashtet.myapplication.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.pashtet.myapplication.databinding.EpisodeItemBinding
import com.pashtet.myapplication.util.DateUtils
import com.pashtet.myapplication.util.HtmlUtils
import com.pashtet.myapplication.viewmodel.PodViewModel

class EpisodeListAdapter (
    private var episodeViewList:List<PodViewModel.EpisodeViewData>?)
    : RecyclerView.Adapter<EpisodeListAdapter.ViewHolder>(){
        inner class ViewHolder(vb: EpisodeItemBinding): RecyclerView.ViewHolder(vb.root){
            var episodeViewData: PodViewModel.EpisodeViewData? = null
            val titleTV: TextView = vb.titleView
            val descTV: TextView = vb.descView
            val durationTV: TextView = vb.durationView
            val releaseDateTV: TextView = vb.releaseDateView
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int)
            : EpisodeListAdapter.ViewHolder {
        return ViewHolder(EpisodeItemBinding.inflate(LayoutInflater.from(parent.context),
            parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val episodeViewList = episodeViewList ?: return
        val episodeView = episodeViewList[position]

        holder.episodeViewData = episodeView
        holder.titleTV.text = episodeView.title
        holder.descTV.text = HtmlUtils.htmlToSpannable(episodeView.description ?: "")//episodeView.description
        holder.durationTV.text = episodeView.duration
        holder.releaseDateTV.text = episodeView.releaseDate?.let { DateUtils.dateToShortDate(it) }
    }

    override fun getItemCount(): Int {
        return episodeViewList?.size ?: 0
    }
}