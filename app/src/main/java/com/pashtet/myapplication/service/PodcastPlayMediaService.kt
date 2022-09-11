package com.pashtet.myapplication.service

import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.session.MediaSessionCompat
import androidx.media.MediaBrowserServiceCompat

class PodcastPlayMediaService : MediaBrowserServiceCompat() {

    private lateinit var mediaSession: MediaSessionCompat

    override fun onCreate(){
        super.onCreate()

        createMediaSession()
    }

    override fun onLoadChildren(
        parentId: String,
        result: Result<MutableList<MediaBrowserCompat.MediaItem>>
    ) {
        if(parentId.equals(PODCASTPLAY_EMPTY_ROOT_MEDIA_ID)){
            result.sendResult(null)
        }
    }

    override fun onGetRoot(
        clientPackageName: String,
        clientUid: Int,
        rootHints: Bundle?
    ): BrowserRoot? {
        return BrowserRoot(PODCASTPLAY_EMPTY_ROOT_MEDIA_ID, null)

    }

    private fun createMediaSession(){
        mediaSession = MediaSessionCompat(this, "PodcastPlayMediaService")
        setSessionToken(mediaSession.sessionToken)

        val callback = PodcastPlayMediaCallback(this, mediaSession)
        mediaSession.setCallback(callback)
    }

    companion object {
        private const val PODCASTPLAY_EMPTY_ROOT_MEDIA_ID = "podcastplay_empty_root_media_id"
    }
}