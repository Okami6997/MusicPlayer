package com.musicplayer.service

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.wifi.WifiManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.Uri
import androidx.media3.cast.CastPlayer
import androidx.media3.cast.SessionAvailabilityListener
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import com.google.android.gms.cast.framework.CastContext
import com.musicplayer.data.repository.QueueRepository
import com.musicplayer.domain.model.Track
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

private const val CONNECT_TIMEOUT_MS = 15000
private const val READ_TIMEOUT_MS = 30000

/**
 * Manages the active [Player] instance, switching between [ExoPlayer] (local/network)
 * and [CastPlayer] (Chromecast) transparently.
 */
@Singleton
class PlayerHolder @Inject constructor(
    @ApplicationContext private val context: Context,
    private val queueRepository: QueueRepository,
    private val okHttpClient: OkHttpClient,
) : SessionAvailabilityListener {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val wifiLock: WifiManager.WifiLock by lazy {
        (context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager)
            .createWifiLock(WifiManager.WIFI_MODE_FULL_HIGH_PERF, "MusicPlayer:WifiLock")
    }

    private val exoPlayer: ExoPlayer by lazy {
        val dataSourceFactory = OkHttpDataSource.Factory(okHttpClient)
        ExoPlayer.Builder(context)
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
    }

    // Network monitoring
    private val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    private val _isNetworkAvailable = MutableStateFlow(true)
    val isNetworkAvailable: StateFlow<Boolean> = _isNetworkAvailable.asStateFlow()

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            Timber.d("Network available")
            _isNetworkAvailable.value = true
            scope.launch { withContext(Dispatchers.Main) { retryPlayback() } }
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

    private val _currentPlayer = MutableStateFlow<Player>(exoPlayer)
    val currentPlayerFlow: StateFlow<Player> = _currentPlayer.asStateFlow()

    private val _queue = MutableStateFlow<List<Track>>(emptyList())
    val queue: StateFlow<List<Track>> = _queue.asStateFlow()

    private val _currentIndex = MutableStateFlow(0)
    val currentIndex: StateFlow<Int> = _currentIndex.asStateFlow()

    var currentPlayer: Player
        get() = _currentPlayer.value
        private set(value) {
            _currentPlayer.value = value
        }

    /**
     * Restores queue state in the holder (e.g. after [onPlaybackResumption]) without
     * touching the player — Media3 will set the items on the player itself.
     */
    fun restoreQueue(tracks: List<Track>, index: Int) {
        _queue.value = tracks
        _currentIndex.value = index
    }

    fun playTracks(tracks: List<Track>, startIndex: Int = 0) {
        _queue.value = tracks
        _currentIndex.value = startIndex
        scope.launch { queueRepository.saveQueueState(tracks, startIndex) }

        // Start the service to ensure the MediaSession is active and notification is shown
        context.startService(Intent(context, MusicPlaybackService::class.java))

        val mediaItems = tracks.map { track ->
            MediaItem.Builder()
                .setMediaId(track.id)
                .setUri(track.uri)
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle(track.title)
                        .setArtist(track.artist)
                        .setAlbumTitle(track.album)
                        .setArtworkUri(track.artworkUri?.let { Uri.parse(it) })
                        .build()
                )
                .build()
        }
        currentPlayer.setMediaItems(mediaItems, startIndex, 0)
        currentPlayer.prepare()
        currentPlayer.play()
    }

    init {
        exoPlayer.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                if (isPlaying) {
                    if (!wifiLock.isHeld) wifiLock.acquire()
                } else {
                    if (wifiLock.isHeld) wifiLock.release()
                }
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                val idx = exoPlayer.currentMediaItemIndex
                _currentIndex.value = idx
                if (_queue.value.isNotEmpty()) {
                    scope.launch { queueRepository.saveQueueState(_queue.value, idx) }
                }
            }
        })
        
        // Register network callback
        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        connectivityManager.registerNetworkCallback(request, networkCallback)
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
        switchToPlayer(exoPlayer)
    }

    private fun switchToPlayer(newPlayer: Player) {
        if (newPlayer == currentPlayer) return
        val playWhenReady = currentPlayer.playWhenReady
        val currentMediaItemIndex = currentPlayer.currentMediaItemIndex
        val currentPosition = currentPlayer.currentPosition

        val mediaItems = mutableListOf<MediaItem>()
        for (i in 0 until currentPlayer.mediaItemCount) {
            mediaItems.add(currentPlayer.getMediaItemAt(i))
        }

        currentPlayer.stop()
        currentPlayer.clearMediaItems()

        newPlayer.setMediaItems(mediaItems)
        newPlayer.playWhenReady = playWhenReady
        newPlayer.prepare()
        newPlayer.seekTo(currentMediaItemIndex, currentPosition)

        currentPlayer = newPlayer
        Timber.d("Switched to ${if (newPlayer is CastPlayer) "CastPlayer" else "ExoPlayer"}")
    }

    private fun retryPlayback() {
        val player = currentPlayer
        val currentIndex = player.currentMediaItemIndex
        val mediaItems = mutableListOf<MediaItem>()
        for (i in 0 until player.mediaItemCount) {
            mediaItems.add(player.getMediaItemAt(i))
        }

        if (mediaItems.isEmpty()) {
            Timber.w("No media items to retry")
            return
        }

        Timber.d("Retrying playback at index $currentIndex")
        player.stop()
        player.clearMediaItems()
        player.setMediaItems(mediaItems, currentIndex, 0)
        player.prepare()
        player.play()
    }

    fun saveCurrentPosition() {
        val player = currentPlayer
        val idx = player.currentMediaItemIndex
        val posMs = player.currentPosition
        if (_queue.value.isNotEmpty()) {
            scope.launch { queueRepository.saveQueueState(_queue.value, idx, posMs) }
        }
    }

    fun release() {
        if (wifiLock.isHeld) wifiLock.release()
        connectivityManager.unregisterNetworkCallback(networkCallback)
        castPlayer?.setSessionAvailabilityListener(null)
        castPlayer?.release()
        exoPlayer.release()
        scope.cancel()
    }
}
