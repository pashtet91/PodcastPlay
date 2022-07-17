package com.pashtet.myapplication.adapter

import android.app.Activity
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.pashtet.myapplication.databinding.MainrvItemBinding
import com.pashtet.myapplication.viewmodel.MainViewModel

class PodListAdapter (
    private var podSummaryViewList: List<MainViewModel.PodSummaryViewData>?,
    private val podListAdapterListener: PodListAdapterListener,
    private val parentActivity: Activity
        ): RecyclerView.Adapter<PodListAdapter.ViewHolder>() {

    interface PodListAdapterListener{
        fun onShowDetails(podSummaryViewData: MainViewModel.PodSummaryViewData)
    }

    inner class ViewHolder(
        vB: MainrvItemBinding,
        private val podListAdapterListener: PodListAdapterListener)
        : RecyclerView.ViewHolder(vB.root){
            var podSummaryViewData: MainViewModel.PodSummaryViewData? = null
            val nameTV: TextView = vB.podNameTV
            val lastUpdatedTV: TextView = vB.podLastUpdatedTV
            val podImV: ImageView = vB.podImage

        init{
            vB.podListItem.setOnClickListener{
                podSummaryViewData?.let{
                    podListAdapterListener.onShowDetails(it)
                }
            }
        }
    }

    fun setListData(podSummaryViewData: List<MainViewModel.PodSummaryViewData>){
        podSummaryViewList = podSummaryViewData
        this.notifyDataSetChanged()
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): PodListAdapter.ViewHolder{
        return ViewHolder(MainrvItemBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        ),
            podListAdapterListener
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int){
        val searchViewList = podSummaryViewList ?: return
        val searchView = searchViewList[position]
        holder.podSummaryViewData = searchView
        holder.nameTV.text = searchView.name
        holder.lastUpdatedTV.text = searchView.lastUpdated

        Glide.with(parentActivity)
            .load(searchView.imageUrl)
            .into(holder.podImV)
    }

    override fun getItemCount():Int{
        return podSummaryViewList?.size ?:0
    }
}