package fr.shinobiwan.tvhome.data

import android.content.ComponentName
import android.content.Context
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.service.notification.NotificationListenerService
import android.graphics.Bitmap
import fr.shinobiwan.tvhome.core.Media
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class TvMediaListener:NotificationListenerService() {
    override fun onListenerConnected(){ connected.value=true }
    override fun onListenerDisconnected(){ connected.value=false }
    companion object { val connected=MutableStateFlow(false) }
}
class TvMedia(private val context:Context) {
    private val state=MutableStateFlow(Media(source="TV"))
    val flow=state.asStateFlow()
    private var controller:MediaController?=null
    fun refresh() {
        try {
            val manager=context.getSystemService(MediaSessionManager::class.java)
            val sessions=manager.getActiveSessions(ComponentName(context,TvMediaListener::class.java))
            val selected=sessions.firstOrNull { it.playbackState?.state==PlaybackState.STATE_PLAYING }
                ?:sessions.firstOrNull { it.metadata!=null }
            controller=selected
            if(selected==null){state.value=Media(status="EMPTY",source="TV");return}
            val metadata=selected.metadata; val playback=selected.playbackState
            val actions=playback?.actions?:0
            val playing=playback?.state==PlaybackState.STATE_PLAYING
            val canToggle=if(playing) actions and (PlaybackState.ACTION_PAUSE or PlaybackState.ACTION_PLAY_PAUSE)!=0L
                else actions and (PlaybackState.ACTION_PLAY or PlaybackState.ACTION_PLAY_PAUSE)!=0L
            val original=metadata?.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)?:metadata?.getBitmap(MediaMetadata.METADATA_KEY_ART)
            val art=original?.let { if(it.width>512||it.height>512){ val ratio=512f/maxOf(it.width,it.height); Bitmap.createScaledBitmap(it,(it.width*ratio).toInt().coerceAtLeast(1),(it.height*ratio).toInt().coerceAtLeast(1),true) }else it }
            // Position is the actual platform sample; do not extrapolate unobserved progress.
            state.value=Media("AVAILABLE","TV",metadata?.getString(MediaMetadata.METADATA_KEY_TITLE).orEmpty(),
                metadata?.getString(MediaMetadata.METADATA_KEY_ARTIST).orEmpty(),selected.packageName,playing,
                (playback?.position?:0).coerceAtLeast(0),(metadata?.getLong(MediaMetadata.METADATA_KEY_DURATION)?:0).coerceAtLeast(0),art,canToggle,
                actions and PlaybackState.ACTION_SKIP_TO_NEXT!=0L)
        } catch (_:SecurityException){controller=null;state.value=Media(source="TV")}
        catch (_:RuntimeException){controller=null;state.value=Media(source="TV")}
    }
    fun toggle(){
        refresh(); val current=state.value; if(!current.canPlayPause)return
        val active=controller?:return
        try { if(current.playing) active.transportControls.pause() else active.transportControls.play() }catch (_:RuntimeException){}
    }
    fun next(){refresh(); if(state.value.canNext)try{controller?.transportControls?.skipToNext()}catch (_:RuntimeException){} }
}
