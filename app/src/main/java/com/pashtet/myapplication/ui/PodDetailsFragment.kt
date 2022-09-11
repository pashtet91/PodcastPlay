package com.pashtet.myapplication.ui

import android.content.ComponentName
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.text.method.ScrollingMovementMethod
import android.view.*
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.pashtet.myapplication.R
import com.pashtet.myapplication.adapter.EpisodeListAdapter
import com.pashtet.myapplication.databinding.FragmentPodDetailsBinding
import com.pashtet.myapplication.service.PodcastPlayMediaService
import com.pashtet.myapplication.viewmodel.PodViewModel
import java.lang.RuntimeException

class PodDetailsFragment : Fragment(), EpisodeListAdapter.EpisodeListAdapterListener {
    private lateinit var vB: FragmentPodDetailsBinding
    private lateinit var episodeListAdapter: EpisodeListAdapter
    private lateinit var mediaBrowser: MediaBrowserCompat
    private var listener: OnPodcastDetailsListener? = null
    private var mediaControllerCallback: MediaControllerCallback? = null
    private val podViewModel: PodViewModel by activityViewModels()

    override fun onCreate(savedInstanceState: Bundle?){
        super.onCreate(savedInstanceState)

        setHasOptionsMenu(true)
        initMediaBrowser()

    }

    override fun onAttach(context: Context) {
        super.onAttach(context)

        if(context is OnPodcastDetailsListener) {
            listener = context
        } else {
            throw RuntimeException(context.toString() +
                " must implement OnPodcastDetailsListener")
        }
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

                episodeListAdapter = EpisodeListAdapter(viewData.episodes, this)
                vB.episodeRV.adapter = episodeListAdapter

                activity?.invalidateOptionsMenu()
            }
            })
    }

    override fun onStart() {
        super.onStart()
        if(mediaBrowser.isConnected) {
            val fragmentActivity = activity as FragmentActivity
            if(MediaControllerCompat.getMediaController(fragmentActivity) == null){
                registerMediaController(mediaBrowser.sessionToken)
            }
        } else{
            mediaBrowser.connect()
        }
    }

    override fun onStop() {
        super.onStop()

        val fragmentActivity = activity as FragmentActivity
        if(MediaControllerCompat.getMediaController(fragmentActivity) != null) {
            mediaControllerCallback?.let{
                MediaControllerCompat.getMediaController(fragmentActivity).unregisterCallback(it)
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.menu_details, menu)
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        podViewModel.podLiveData.observe(viewLifecycleOwner, {
            podcast ->
            if(podcast != null){
                menu.findItem(R.id.menu_feed_action).title =
                    if(podcast.subscribed)
                        getString(R.string.unsubscribe)
                    else
                        getString(R.string.subscribe)
            }
        })
        super.onPrepareOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_feed_action -> {
                if(item.title == getString(R.string.unsubscribe)) {
//                podViewModel.podLiveData.value?.feedUrl?.let{
//                    listener?.onSubscribe()
//                }
//                true
                    listener?.onUnsubscribe()
                }else{
                    listener?.onSubscribe()
                }
                true
            }else-> super.onOptionsItemSelected(item)
        }
    }

    override fun onSelectedEpisode(episodeViewData: PodViewModel.EpisodeViewData) {
        val fragmentActivity = activity as FragmentActivity
        val controller = MediaControllerCompat.getMediaController(fragmentActivity)
        if(controller.playbackState != null){
            if(controller.playbackState.state == PlaybackStateCompat.STATE_PLAYING) {
                controller.transportControls.pause()
            } else {
                startPlaying(episodeViewData)
            }
        } else {
            startPlaying(episodeViewData)
        }
    }

    private fun updateControls(){
        val viewData = podViewModel.activePodcastViewData ?: return
        vB.feedTitleTV.text = viewData.feedTitle
        vB.feedDescTV.text = viewData.feedDesc
        activity?.let{activity ->
            Glide.with(activity).load(viewData.imageUrl).into(vB.feedIV)
        }
    }

    private fun registerMediaController(token: MediaSessionCompat.Token) {
        val fragmentActivity = activity as FragmentActivity
        val mediaController = MediaControllerCompat(fragmentActivity, token)
        MediaControllerCompat.setMediaController(fragmentActivity, mediaController)
        mediaControllerCallback = MediaControllerCallback()
        mediaController.registerCallback(mediaControllerCallback!!)
    }

    private fun initMediaBrowser() {
        val fragmentActivity = activity as FragmentActivity
        mediaBrowser = MediaBrowserCompat (
                fragmentActivity,
                ComponentName(fragmentActivity, PodcastPlayMediaService::class.java),
                MediaBrowserCallbackS(),
                null
            )
    }

    private fun startPlaying(episodeViewData: PodViewModel.EpisodeViewData) {
        val fragmentActivity = activity as FragmentActivity
        val controller = MediaControllerCompat.getMediaController(fragmentActivity)
        controller.transportControls.playFromUri(Uri.parse(episodeViewData.mediaUrl), null)
    }


    companion object{
        fun newInstance(): PodDetailsFragment{
            return PodDetailsFragment()
        }
    }

    interface OnPodcastDetailsListener{
        fun onSubscribe()
        fun onUnsubscribe()
    }

    inner class MediaControllerCallback: MediaControllerCompat.Callback(){
        override fun onMetadataChanged(metadata: MediaMetadataCompat?) {
            super.onMetadataChanged(metadata)
            println("metadata changed to ${metadata?.getString(
                MediaMetadataCompat.METADATA_KEY_MEDIA_URI
            )}")
        }

        override fun onPlaybackStateChanged(state: PlaybackStateCompat?) {
            super.onPlaybackStateChanged(state)
            println("state changed to $state")
        }
    }

    inner class MediaBrowserCallbackS: MediaBrowserCompat.ConnectionCallback(){
        override fun onConnected() {
            super.onConnected()
            registerMediaController(mediaBrowser.sessionToken)
            println("onConnected")
        }

        override fun onConnectionSuspended() {
            super.onConnectionSuspended()
            println("onConnectionSuspended")
            // Disable transport controls
        }

        override fun onConnectionFailed() {
            super.onConnectionFailed()
            println("onConnectionFailed")
            // Fatal error handling
        }
    }
}