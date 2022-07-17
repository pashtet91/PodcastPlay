package com.pashtet.myapplication.ui

import android.os.Bundle
import android.text.method.ScrollingMovementMethod
import android.view.*
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.pashtet.myapplication.R
import com.pashtet.myapplication.adapter.EpisodeListAdapter
import com.pashtet.myapplication.databinding.FragmentPodDetailsBinding
import com.pashtet.myapplication.viewmodel.PodViewModel

class PodDetailsFragment : Fragment() {
    private lateinit var vB: FragmentPodDetailsBinding
    private val podViewModel: PodViewModel by activityViewModels()
    private lateinit var episodeListAdapter: EpisodeListAdapter

    override fun onCreate(savedInstanceState: Bundle?){
        super.onCreate(savedInstanceState)

        setHasOptionsMenu(true)

    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?): View{

        vB = FragmentPodDetailsBinding.inflate(inflater, container, false)
        return vB.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        //updateControls()
        podViewModel.podLiveData.observe(viewLifecycleOwner,
            {viewData ->
            if(viewData != null){
                vB.feedTitleTV.text = viewData.feedTitle
                vB.feedDescTV.text = viewData.feedDesc
                activity?.let{activity ->
                    Glide.with(activity).load(viewData.imageUrl).into(vB.feedIV)
                }

                vB.feedDescTV.movementMethod = ScrollingMovementMethod()
                vB.episodeRV.setHasFixedSize(true)

                val layoutManager = LinearLayoutManager(activity)
                vB.episodeRV.layoutManager = layoutManager

                val dividerItemDecoration = DividerItemDecoration(vB.episodeRV.context,
                                                                  layoutManager.orientation)

                vB.episodeRV.addItemDecoration(dividerItemDecoration)

                episodeListAdapter = EpisodeListAdapter(viewData.episodes)
                vB.episodeRV.adapter = episodeListAdapter
            }
            })
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.menu_details, menu)
    }

    private fun updateControls(){
        val viewData = podViewModel.activePodcastViewData ?: return
        vB.feedTitleTV.text = viewData.feedTitle
        vB.feedDescTV.text = viewData.feedDesc
        activity?.let{activity ->
            Glide.with(activity).load(viewData.imageUrl).into(vB.feedIV)
        }
    }

    companion object{
        fun newInstance(): PodDetailsFragment{
            return PodDetailsFragment()
        }
    }
}