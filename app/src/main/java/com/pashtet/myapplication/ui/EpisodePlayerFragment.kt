package com.pashtet.myapplication.ui

import android.animation.ValueAnimator
import android.content.ComponentName
import android.content.Context
import android.graphics.Color
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.text.method.ScrollingMovementMethod
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.activityViewModels
import com.bumptech.glide.Glide
import android.text.format.DateUtils;
import android.view.SurfaceHolder
import android.view.animation.LinearInterpolator
import androidx.appcompat.app.AppCompatActivity
import com.pashtet.myapplication.databinding.FragmentEpisodePlayerBinding
import com.pashtet.myapplication.service.PodcastPlayMediaCallback
import com.pashtet.myapplication.service.PodcastPlayMediaCallback.Companion.CMD_CHANGESPEED
import com.pashtet.myapplication.service.PodcastPlayMediaCallback.Companion.CMD_EXTRA_SPEED
import com.pashtet.myapplication.service.PodcastPlayMediaService
import com.pashtet.myapplication.util.HtmlUtils
import com.pashtet.myapplication.viewmodel.PodViewModel


class EpisodePlayerFragment : Fragment() {

    private lateinit var vB: FragmentEpisodePlayerBinding
    private lateinit var mediaBrowser: MediaBrowserCompat
    private val podViewModel: PodViewModel by activityViewModels()
    private var mediaControllerCallback: MediaControllerCallback? = null
    private var mediaSession: MediaSessionCompat? = null
    private var mediaPlayer: MediaPlayer? = null
    private var progressAnimator: ValueAnimator? = null
    private var playerSpeed: Float = 1.0f
    private var episodeDuration: Long = 0
    private var draggingScrubber: Boolean = false
    private var playOnPrepare: Boolean = false
    private var isVideo: Boolean = false

    companion object {
        fun newInstance():EpisodePlayerFragment {
            return EpisodePlayerFragment()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        isVideo = if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            podViewModel.activeEpisodeViewData?.isVideo ?: false
        } else {
            false
        }
        if(!isVideo)
            initMediaBrowser()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        vB = FragmentEpisodePlayerBinding.inflate(inflater, container, false)
        return vB.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?){
        super.onViewCreated(view, savedInstanceState)
        setupControls()

        if(isVideo) {
            initMediaSession()
            initVideoPlayer()
        }
        updateControls()
    }

    override fun onStart() {
        super.onStart()
        if(!isVideo)
            if(mediaBrowser.isConnected) {
                val fragmentActivity = activity as FragmentActivity
                if(MediaControllerCompat.getMediaController(fragmentActivity) == null){
                    registerMediaController(mediaBrowser.sessionToken)
                }
                updateControlsFromController()
            } else{
                mediaBrowser.connect()
            }
    }

    override fun onStop() {
        super.onStop()
        progressAnimator?.cancel()

        val fragmentActivity = activity as FragmentActivity
        if(MediaControllerCompat.getMediaController(fragmentActivity) != null) {
            mediaControllerCallback?.let{
                MediaControllerCompat.getMediaController(fragmentActivity).unregisterCallback(it)
            }
        }

        if(isVideo)
            mediaPlayer?.setDisplay(null)

        if(!fragmentActivity.isChangingConfigurations) {
            mediaPlayer?.release()
            mediaPlayer = null
        }
    }

    private fun setupControls(){
        vB.playToggleButton.setOnClickListener{
            togglePlayPause()
        }

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            vB.speedButton.setOnClickListener {
                changeSpeed()
            }
        } else {
            vB.speedButton.visibility = View.INVISIBLE
        }

        vB.forwardButton.setOnClickListener{
            seekBy(30)
        }
        vB.replayButton.setOnClickListener{
            seekBy(-10)
        }

        vB.seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(p0: SeekBar?, p1: Int, p2: Boolean) {
                vB.currentTimeTV.text = DateUtils.formatElapsedTime((p1 / 1000).toLong())
            }

            override fun onStartTrackingTouch(p0: SeekBar?) {
                draggingScrubber = true
            }

            override fun onStopTrackingTouch(p0: SeekBar?) {
                draggingScrubber = false

                val fragmentActivity = activity as FragmentActivity
                val controller = MediaControllerCompat.getMediaController(fragmentActivity)

                if(controller.playbackState != null) {
                    controller.transportControls.seekTo(vB.seekBar.progress.toLong())
                } else {
                    vB.seekBar.progress = 0
                }
            }
        })
    }

    private fun setSurfaceSize() {
        val mediaPlayer = mediaPlayer ?: return
        val videoWidth = mediaPlayer.videoWidth
        val videoHeight = mediaPlayer.videoHeight

        val parent = vB.videoSV.parent as View
        val containerWidth = parent.width
        val containerHeigth = parent.height

        val layoutAspectRatio = containerWidth.toFloat() / containerHeigth
        val videoAspectRatio = videoWidth.toFloat() / videoHeight

        val layoutParams = vB.videoSV.layoutParams

        if(videoAspectRatio > layoutAspectRatio) {
            layoutParams.height = (containerWidth / videoAspectRatio).toInt()
        } else {
            layoutParams.width = (containerHeigth * videoAspectRatio).toInt()
        }

        vB.videoSV.layoutParams = layoutParams
    }

    private fun setupVideoUI() {
        vB.episodeDescTV.visibility = View.INVISIBLE
        vB.headerView.visibility = View.INVISIBLE
        val activity = activity as AppCompatActivity
        activity.supportActionBar?.hide()

        vB.playerControls.setBackgroundColor(Color.argb(255/2, 0, 0, 0))
    }

    private fun handleStateChange(state: Int, position: Long, speed: Float) {
        progressAnimator?.let{
            it.cancel()
            progressAnimator = null
        }

        val isPlaying = state == PlaybackStateCompat.STATE_PLAYING
        vB.playToggleButton.isActivated = isPlaying

        val progress = position.toInt()
        vB.seekBar.progress = progress
        val speedButtonText = "${playerSpeed}x"
        vB.speedButton.text = speedButtonText

        if(isPlaying){
            if(isVideo)
                setupVideoUI()

            animateScrubber(progress, speed)
        }
    }

    private fun updateControls() {
        vB.episodeTitleTV.text = podViewModel.activeEpisodeViewData?.title
        val htmlDesc = podViewModel.activeEpisodeViewData?.description ?: ""
        val descSpan = HtmlUtils.htmlToSpannable(htmlDesc)
        vB.episodeDescTV.text = descSpan
        vB.episodeDescTV.movementMethod = ScrollingMovementMethod()

        val fragmentActivity = activity as FragmentActivity
        Glide.with(fragmentActivity)
            .load(podViewModel.podLiveData.value?.imageUrl)
            .into(vB.episodeImageView)

        val speedButtonText = "${playerSpeed}x"
        vB.speedButton.text = speedButtonText

        mediaPlayer?.let{
            updateControlsFromController()
        }
    }

    private fun updateControlsFromMetadata(metadata: MediaMetadataCompat){
        episodeDuration = metadata.getLong(MediaMetadataCompat.METADATA_KEY_DURATION)
        vB.endTimeTV.text = DateUtils.formatElapsedTime((episodeDuration / 1000))
        vB.seekBar.max = episodeDuration.toInt()
    }

    private fun updateControlsFromController() {
        val fragmentActivity = activity as FragmentActivity
        val controller = MediaControllerCompat.getMediaController(fragmentActivity)

        if(controller != null) {
            val metadata = controller.metadata
            if(metadata != null){
                handleStateChange(
                    controller.playbackState.state,
                    controller.playbackState.position,
                    playerSpeed
                )
                updateControlsFromMetadata(controller.metadata)
            }
        }
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

    private fun initMediaPlayer(){
        if(mediaPlayer == null) {
            mediaPlayer = MediaPlayer()
            mediaPlayer?.let{medPlayer ->
                medPlayer.setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build()
                )
            medPlayer.setDataSource(podViewModel.activeEpisodeViewData?.mediaUrl)
            medPlayer.setOnPreparedListener{
                val fragmentActivity = activity as FragmentActivity
                mediaSession?.let{medSession ->
                    val episodeMediaCallback =
                        PodcastPlayMediaCallback(fragmentActivity, medSession, it)

                    medSession.setCallback(episodeMediaCallback)
                }

                setSurfaceSize()

                if(playOnPrepare) {
                    togglePlayPause()
                }
            }
                medPlayer.prepareAsync()
            }
        } else {
            setSurfaceSize()
        }

    }

    private fun initMediaSession() {
        if(mediaSession == null) {
            mediaSession = MediaSessionCompat(activity as Context, "EpisodePlayerFragment")
            mediaSession?.setMediaButtonReceiver(null)
        }
        mediaSession?.let {
            registerMediaController(it.sessionToken)
        }
    }

    private fun initVideoPlayer() {
        vB.videoSV.visibility = View.VISIBLE
        val surfaceHolder = vB.videoSV.holder
        surfaceHolder.addCallback(object : SurfaceHolder.Callback{
            override fun surfaceCreated(p0: SurfaceHolder) {
                initMediaPlayer()
                mediaPlayer?.setDisplay(p0)
            }
            override fun surfaceChanged(p0: SurfaceHolder, p1: Int, p2: Int, p3: Int) {}
            override fun surfaceDestroyed(p0: SurfaceHolder) {}
        })
    }

    private fun registerMediaController(token: MediaSessionCompat.Token) {
        val fragmentActivity = activity as FragmentActivity
        val mediaController = MediaControllerCompat(fragmentActivity, token)
        MediaControllerCompat.setMediaController(fragmentActivity, mediaController)
        mediaControllerCallback = MediaControllerCallback()
        mediaController.registerCallback(mediaControllerCallback!!)
    }

    private fun togglePlayPause() {
        playOnPrepare = true

        val fragmentActivity = activity as FragmentActivity
        val controller = MediaControllerCompat.getMediaController(fragmentActivity)
        if(controller.playbackState != null) {
            if(controller.playbackState.state == PlaybackStateCompat.STATE_PLAYING){
                controller.transportControls.pause()
            } else {
                podViewModel.activeEpisodeViewData?.let {
                    startPlaying(it)
                }
            }
        }
        else{
            podViewModel.activeEpisodeViewData?.let{
                startPlaying(it)
            }
        }
    }

    private fun startPlaying(episodeViewData: PodViewModel.EpisodeViewData) {
        val fragmentActivity = activity as FragmentActivity
        val controller = MediaControllerCompat.getMediaController(fragmentActivity)
        controller.transportControls.playFromUri(Uri.parse(episodeViewData.mediaUrl), null)
    }

    private fun changeSpeed() {
        playerSpeed += 0.25f

        if(playerSpeed > 2.0f) {
            playerSpeed = 0.75f
        }

        val bundle = Bundle()
        bundle.putFloat(CMD_EXTRA_SPEED, playerSpeed)
        val fragmentActivity = activity as FragmentActivity
        val controller = MediaControllerCompat.getMediaController(fragmentActivity)
        controller.sendCommand(CMD_CHANGESPEED, bundle, null)

        val speedButtonText = "${playerSpeed}x"
        vB.speedButton.text = speedButtonText
    }

    private fun seekBy(seconds: Int) {
        val fragmentActivity = activity as FragmentActivity
        val controller = MediaControllerCompat.getMediaController(fragmentActivity)
        val newPosition = controller.playbackState.position + seconds * 1000
        controller.transportControls.seekTo(newPosition)
    }

    private fun animateScrubber(progress: Int, speed: Float) {
        val timeRemaining = ((episodeDuration - progress) / speed).toInt()

        if(timeRemaining < 0) {
            return
        }

        progressAnimator = ValueAnimator.ofInt(progress, episodeDuration.toInt())
        progressAnimator?.let { animator ->
            animator.duration = timeRemaining.toLong()
            animator.interpolator = LinearInterpolator()
            animator.addUpdateListener {
                if (draggingScrubber) {
                    animator.cancel()
                } else {
                    vB.seekBar.progress = animator.animatedValue as Int
                }
            }

            animator.start()
        }
    }

    inner class MediaControllerCallback: MediaControllerCompat.Callback(){
        override fun onMetadataChanged(metadata: MediaMetadataCompat?) {
            super.onMetadataChanged(metadata)
            println(
                "metadata changed to ${metadata?.getString(
                MediaMetadataCompat.METADATA_KEY_MEDIA_URI
                )}"
            )
            metadata?.let{ updateControlsFromMetadata(it) }
        }

        override fun onPlaybackStateChanged(state: PlaybackStateCompat?) {
            super.onPlaybackStateChanged(state)
            println("state changed to $state")
            val state = state ?: return
            handleStateChange(state.getState(), state.position, state.playbackSpeed)
        }
    }

    inner class MediaBrowserCallbackS: MediaBrowserCompat.ConnectionCallback(){
        override fun onConnected() {
            super.onConnected()
            registerMediaController(mediaBrowser.sessionToken)
            println("onConnected")
            updateControlsFromController()
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