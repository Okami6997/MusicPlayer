package com.musicplayer.service

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.Uri
import android.net.wifi.WifiManager
import androidx.media3.cast.CastPlayer
import androidx.media3.cast.SessionAvailabilityListener
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import com.google.android.gms.cast.framework.CastContext
import com.musicplayer.data.repository.QueueRepository
import com.musicplayer.domain.model.Track
import com.musicplayer.ui.settings.SettingsKeys
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences

/**
 * Manages the active [Player] instance, switching between [ExoPlayer] (local/network)
 * and [CastPlayer] (Chromecast) transparently.
 */
@UnstableApi
@Singleton
class PlayerHolder @Inject constructor(
    @ApplicationContext private val context: Context,
    private val queueRepository: QueueRepository,
    private val okHttpClient: OkHttpClient,
    private val dataStore: DataStore<Preferences>,
) : SessionAvailabilityListener {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val wifiLock: WifiManager.WifiLock by lazy {
        (context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager)
            .createWifiLock(WifiManager.WIFI_MODE_FULL_HIGH_PERF, "MusicPlayer:WifiLock")
    }

    private var _exoPlayer: ExoPlayer? = null
    
    private val exoPlayerListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            if (isPlaying) {
                if (!wifiLock.isHeld) wifiLock.acquire()
            } else {
                if (wifiLock.isHeld) wifiLock.release()
            }
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            _exoPlayer?.let { player ->
                val idx = player.currentMediaItemIndex
                _currentIndex.value = idx
                if (_queue.value.isNotEmpty()) {
                    scope.launch(Dispatchers.IO) { 
                        queueRepository.saveQueueState(_queue.value, idx) 
                    }
                }
            }
        }
    }

    private fun createExoPlayer(): ExoPlayer {
        Timber.d("Creating new ExoPlayer instance")
        val httpDataSourceFactory = OkHttpDataSource.Factory(okHttpClient)
        val dataSourceFactory = DefaultDataSource.Factory(context, httpDataSourceFactory)
        val player = ExoPlayer.Builder(context)
            .setMediaSourceFactory(
                DefaultMediaSourceFactory(context)
                    .setDataSourceFactory(dataSourceFactory)
            )
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .setUsage(C.USAGE_MEDIA)
                    .build(),
                /* handleAudioFocus= */ true
            )
            .setHandleAudioBecomingNoisy(true)
            .setWakeMode(C.WAKE_MODE_NETWORK)
            .build()
        
        player.addListener(exoPlayerListener)
        
        // Initial gapless/skip silence setting
        scope.launch {
            val enabled = dataStore.data.map { it[SettingsKeys.GAPLESS_PLAYBACK] ?: true }.first()
            withContext(Dispatchers.Main) {
                player.skipSilenceEnabled = enabled
            }
        }

        _exoPlayer = player
        return player
    }

    // Network monitoring
    private val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    private val _isNetworkAvailable = MutableStateFlow(true)
    val isNetworkAvailable: StateFlow<Boolean> = _isNetworkAvailable.asStateFlow()

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            Timber.d("Network available")
            _isNetworkAvailable.value = true
            scope.launch { retryPlayback() }
        }
        override fun onLost(network: Network) {
            Timber.d("Network lost")
            _isNetworkAvailable.value = false
        }
        override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) {
            _isNetworkAvailable.value = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        }
    }

    private val castPlayer: CastPlayer? by lazy {
        try {
            val castContext = CastContext.getSharedInstance(context)
            CastPlayer(castContext).also {
                it.setSessionAvailabilityListener(this)
            }
        } catch (e: Exception) {
            Timber.w(e, "Cast not available — disabling Chromecast support")
            null
        }
    }

    private val _currentPlayer = MutableStateFlow<Player>(createExoPlayer())
    val currentPlayerFlow: StateFlow<Player> = _currentPlayer.asStateFlow()

    private val _queue = MutableStateFlow<List<Track>>(emptyList())
    val queue: StateFlow<List<Track>> = _queue.asStateFlow()

    private val _currentIndex = MutableStateFlow(0)
    val currentIndex: StateFlow<Int> = _currentIndex.asStateFlow()

    var currentPlayer: Player
        get() {
            synchronized(this) {
                val current = _currentPlayer.value
                if (current is ExoPlayer && _exoPlayer == null) {
                    val newPlayer = createExoPlayer()
                    _currentPlayer.value = newPlayer
                    return newPlayer
                }
                return current
            }
        }
        private set(value) {
            _currentPlayer.value = value
        }

    fun withPlayer(action: Player.() -> Unit) {
        currentPlayer.runOnPlayerThread(action)
    }

    fun restoreQueue(tracks: List<Track>, index: Int) {
        _queue.value = tracks
        _currentIndex.value = index
    }

    fun playTracks(tracks: List<Track>, startIndex: Int = 0) {
        _queue.value = tracks
        _currentIndex.value = startIndex
        scope.launch(Dispatchers.IO) { queueRepository.saveQueueState(tracks, startIndex) }

        context.startService(Intent(context, MusicPlaybackService::class.java))

        val mediaItems = tracks.map { track ->
            val uri = if (track.isDownloaded && track.downloadedUri != null) {
                Uri.parse(track.downloadedUri)
            } else {
                Uri.parse(track.uri)
            }
            MediaItem.Builder()
                .setMediaId(track.id)
                .setUri(uri)
                .setMediaMetadata(
                    androidx.media3.common.MediaMetadata.Builder()
                        .setTitle(track.title)
                        .setArtist(track.artist)
                        .setAlbumTitle(track.album)
                        .setArtworkUri(track.artworkUri?.let { Uri.parse(it) })
                        .build()
                )
                .build()
        }
        withPlayer {
            setMediaItems(mediaItems, startIndex, 0)
            prepare()
            play()
        }
    }

    fun playUri(uri: Uri) {
        val mediaItem = MediaItem.Builder()
            .setUri(uri)
            .setMediaMetadata(
                androidx.media3.common.MediaMetadata.Builder()
                    .setTitle(uri.lastPathSegment ?: "External Audio")
                    .build()
            )
            .build()
        
        withPlayer {
            setMediaItem(mediaItem)
            prepare()
            play()
        }
        context.startService(Intent(context, MusicPlaybackService::class.java))
    }

    init {
        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        connectivityManager.registerNetworkCallback(request, networkCallback)

        scope.launch {
            dataStore.data.map { it[SettingsKeys.GAPLESS_PLAYBACK] ?: true }
                .collect { enabled ->
                    withContext(Dispatchers.Main) {
                        _exoPlayer?.skipSilenceEnabled = enabled
                    }
                }
        }
    }

    override fun onCastSessionAvailable() {
        Timber.d("Cast session available — switching to CastPlayer")
        val player = castPlayer ?: return
        player.addListener(object : Player.Listener {
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                _currentIndex.value = player.currentMediaItemIndex
            }
        })
        switchToPlayer(player)
    }

    override fun onCastSessionUnavailable() {
        Timber.d("Cast session unavailable — switching to ExoPlayer")
        switchToPlayer(currentPlayer) // This will ensure ExoPlayer is recreated if needed
    }

    private fun switchToPlayer(newPlayer: Player) {
        val oldPlayer = _currentPlayer.value
        if (newPlayer == oldPlayer) return
        
        val playWhenReady = oldPlayer.playWhenReady
        val currentMediaItemIndex = oldPlayer.currentMediaItemIndex
        val currentPosition = oldPlayer.currentPosition

        val mediaItems = mutableListOf<MediaItem>()
        for (i in 0 until oldPlayer.mediaItemCount) {
            mediaItems.add(oldPlayer.getMediaItemAt(i))
        }

        oldPlayer.stop()
        oldPlayer.clearMediaItems()

        newPlayer.runOnPlayerThread {
            setMediaItems(mediaItems)
            this.playWhenReady = playWhenReady
            prepare()
            seekTo(currentMediaItemIndex, currentPosition)
        }

        currentPlayer = newPlayer
        Timber.d("Switched to ${if (newPlayer is CastPlayer) "CastPlayer" else "ExoPlayer"}")
    }

    private fun retryPlayback() {
        withPlayer {
            val currentIndex = currentMediaItemIndex
            val mediaItems = mutableListOf<MediaItem>()
            for (i in 0 until mediaItemCount) {
                mediaItems.add(getMediaItemAt(i))
            }

            if (mediaItems.isEmpty()) return@withPlayer

            Timber.d("Retrying playback at index $currentIndex")
            stop()
            clearMediaItems()
            setMediaItems(mediaItems, currentIndex, 0)
            prepare()
            play()
        }
    }

    fun saveCurrentPosition() {
        withPlayer {
            val idx = currentMediaItemIndex
            val posMs = currentPosition
            if (_queue.value.isNotEmpty()) {
                scope.launch(Dispatchers.IO) { 
                    queueRepository.saveQueueState(_queue.value, idx, posMs) 
                }
            }
        }
    }

    fun release() {
        if (wifiLock.isHeld) wifiLock.release()
        
        synchronized(this) {
            val oldExo = _exoPlayer
            _exoPlayer = null
            
            oldExo?.let { player ->
                player.runOnPlayerThread {
                    player.stop()
                    player.clearMediaItems()
                    player.release()
                }
            }

            castPlayer?.runOnPlayerThread {
                stop()
                clearMediaItems()
            }

            // Immediately switch to a fresh player so no one uses the released one
            _currentPlayer.value = createExoPlayer()
        }
    }
}
