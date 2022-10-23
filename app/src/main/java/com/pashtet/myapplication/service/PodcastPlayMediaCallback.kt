package com.pashtet.myapplication.service

import android.content.Context
import android.media.*
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.ResultReceiver
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat

class PodcastPlayMediaCallback(val context: Context,
                               val mediaSession: MediaSessionCompat,
                               var mediaPlayer: MediaPlayer? = null
                               ) : MediaSessionCompat.Callback() {

    private var mediaUri: Uri? = null
    private var newMedia: Boolean = false
    private var mediaExtras: Bundle? = null
    private var focusRequest: AudioFocusRequest? = null
    private var mediaNeedsPrepare: Boolean = false

    override fun onPlayFromUri(uri: Uri?, extras: Bundle?) {
        super.onPlayFromUri(uri, extras)
        println("Playing ${uri.toString()}")
        if(mediaUri == uri) {
            newMedia = false
            mediaExtras = null
        } else {
            mediaExtras = extras
            setNewMedia(uri)
        }
        onPlay()
//        mediaSession.setMetadata(
//            MediaMetadataCompat.Builder()
//                .putString(MediaMetadataCompat.METADATA_KEY_MEDIA_URI, uri.toString())
//                .build()
//        )
    }

    override fun onPlay(){
        super.onPlay()
        println("onPlay called")
        if(ensureAudioFocus()) {
            mediaSession.isActive = true
            //setState(PlaybackStateCompat.STATE_PLAYING)
            initializeMediaPlayer()
            prepareMedia()
            startPlaying()
        }
    }

    override fun onStop(){
        super.onStop()
        println("onStop called")
        stopPlaying()
    }

    override fun onPause() {
        super.onPause()
        println("onPause called")
        //setState(PlaybackStateCompat.STATE_PAUSED)
        pausePlaying()
    }

    override fun onCommand(command: String?, extras: Bundle?, cb: ResultReceiver?) {
        super.onCommand(command, extras, cb)
        when(command) {
            CMD_CHANGESPEED -> extras?.let{changeSpeed(it)}
        }
    }

    override fun onSeekTo(pos: Long) {
        super.onSeekTo(pos)
        mediaPlayer?. seekTo(pos.toInt())

        val playbackState: PlaybackStateCompat? = mediaSession.controller.playbackState

        if(playbackState != null) {
            setState(playbackState.state)
        } else {
            setState(PlaybackStateCompat.STATE_PAUSED)
        }
    }

    private fun setState(state: Int, newSpeed: Float? = null){
        var position: Long = -1
        mediaPlayer?.let{
            position = it.currentPosition.toLong()
        }

        var speed = 1.0f;

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if(newSpeed == null) {
                speed = mediaPlayer?.getPlaybackParams()?.speed ?: 1.0f
            } else {
                speed = newSpeed
            }

            mediaPlayer?.let{mediaPlayer ->
                try{
                    mediaPlayer.playbackParams = mediaPlayer.playbackParams.setSpeed(speed)
                }
                catch (e:Exception) {
                    mediaPlayer.reset()
                    mediaUri?.let{ mediaUri ->
                        mediaPlayer.setDataSource(context,mediaUri)
                    }
                    mediaPlayer.prepare()
                    mediaPlayer.playbackParams = mediaPlayer.playbackParams.setSpeed(speed)
                    mediaPlayer.seekTo(position.toInt())

                    if(state == PlaybackStateCompat.STATE_PLAYING){
                        mediaPlayer.start()
                    }
                }
            }
        }
        val playbackState = PlaybackStateCompat.Builder()
            .setActions(
                PlaybackStateCompat.ACTION_PLAY or
                PlaybackStateCompat.ACTION_STOP or
                PlaybackStateCompat.ACTION_PLAY_PAUSE or
                PlaybackStateCompat.ACTION_PAUSE
            )
            .setState(state, position, speed)
            .build()

        mediaSession.setPlaybackState(playbackState)
    }

    private fun setNewMedia(uri:Uri?){
        newMedia = true
        mediaUri = uri
    }

    private fun ensureAudioFocus(): Boolean{
        val audioManager = this.context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN).run{
                setAudioAttributes(AudioAttributes.Builder().run{
                    setUsage(AudioAttributes.USAGE_MEDIA)
                    setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    build()
                })
                build()
            }
            this.focusRequest = focusRequest
            val result = audioManager.requestAudioFocus(focusRequest)
            return result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        }
        else {
            val result = audioManager.requestAudioFocus(
                null,
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN
                )
            return result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        }
    }

    private fun removeAudioFocus() {
        val audioManager = this.context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            focusRequest?.let{
                audioManager.abandonAudioFocusRequest(it)
            }
        } else {
            audioManager.abandonAudioFocus(null)
        }
    }

    private fun initializeMediaPlayer() {
        if (mediaPlayer == null) {
            mediaPlayer = MediaPlayer()
            mediaPlayer?.setOnCompletionListener({
                setState(PlaybackStateCompat.STATE_PAUSED)
            })
            mediaNeedsPrepare = true
        }
    }

    private fun prepareMedia() {
        if(newMedia) {
            newMedia = false
            mediaPlayer?.let{mediaPlayer ->
                mediaUri?.let{ mediaUri ->
                    if(mediaNeedsPrepare) {
                        mediaPlayer.reset()
                        mediaPlayer.setDataSource(context, mediaUri)
                        mediaPlayer.prepare()
                    }
                    mediaSession.setMetadata(
                        MediaMetadataCompat.Builder().putString(
                            MediaMetadataCompat.METADATA_KEY_MEDIA_URI,
                            mediaUri.toString()
                        )
                        .putLong(
                            MediaMetadataCompat.METADATA_KEY_DURATION,
                            mediaPlayer.duration.toLong()
                        )
                        .build()
                    )
                }
            }
        }
    }

    private fun startPlaying() {
        mediaPlayer?.let{mediaPlayer ->
            if(!mediaPlayer.isPlaying) {
                mediaPlayer.start()
                setState(PlaybackStateCompat.STATE_PLAYING)
            }
        }
    }

    private fun changeSpeed(extras: Bundle) {
        var playbackState = PlaybackStateCompat.STATE_PAUSED
        if(mediaSession.controller.playbackState != null) {
            playbackState = mediaSession.controller.playbackState.state
        }
        setState(playbackState, extras.getFloat(CMD_EXTRA_SPEED))
    }

    private fun pausePlaying() {
        removeAudioFocus()
        mediaPlayer?.let{ mediaPlayer ->
            if(mediaPlayer.isPlaying){
                mediaPlayer.pause()
                setState(PlaybackStateCompat.STATE_PAUSED)
            }
        }
    }

    private fun stopPlaying() {
        removeAudioFocus()
        mediaSession.isActive = false
        mediaPlayer?.let {mediaPlayer ->
            if(mediaPlayer.isPlaying){
                mediaPlayer.stop()
                setState(PlaybackStateCompat.STATE_STOPPED)
            }
        }
    }


    companion object {
        const val CMD_CHANGESPEED = "change_speed"
        const val CMD_EXTRA_SPEED = "speed"
    }
}