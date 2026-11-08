@file:OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)

package com.sosauce.chocola.core.data.watch

import android.content.ComponentName
import android.content.Context
import android.graphics.Bitmap
import android.os.Build
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.util.fastFilter
import androidx.compose.ui.util.fastFirstOrNull
import androidx.compose.ui.util.fastMap
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import coil3.imageLoader
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.toBitmap
import com.google.android.gms.wearable.Asset
import com.google.android.gms.wearable.CapabilityClient
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import com.google.common.util.concurrent.MoreExecutors
import com.sosauce.chocola.app.PlaybackService
import com.sosauce.chocola.core.data.datastore.PreferencesKeys.PALETTE_STYLE
import com.sosauce.chocola.core.data.datastore.PreferencesKeys.THEME
import com.sosauce.chocola.core.data.datastore.PreferencesKeys.USE_ART_THEME
import com.sosauce.chocola.core.data.datastore.PreferencesKeys.USE_SYSTEM_FONT
import com.sosauce.chocola.core.data.datastore.dataStore
import com.sosauce.chocola.core.data.library.AbstractTracksScanner
import com.sosauce.chocola.core.data.mapper.toMediaItem
import com.sosauce.chocola.core.designsystem.CutePaletteStyle
import com.sosauce.chocola.core.designsystem.CuteTheme
import com.sosauce.chocola.core.domain.model.CuteTrack
import com.sosauce.chocola.core.presentation.util.playOrPause
import com.sosauce.chocola.core.presentation.util.playRandom
import com.sosauce.chocola.core.presentation.util.toPaletteStyle
import com.sosauce.chocola.shared.WatchAppearance
import com.sosauce.chocola.shared.WatchCommand
import com.sosauce.chocola.shared.WatchContract
import com.sosauce.chocola.shared.WatchFolder
import com.sosauce.chocola.shared.WatchLibrary
import com.sosauce.chocola.shared.WatchPlayRequest
import com.sosauce.chocola.shared.WatchPlayerState
import com.sosauce.chocola.shared.WatchTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.ByteArrayOutputStream
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class WatchBridge(
    private val context: Context,
    private val ioScope: CoroutineScope,
    private val abstractTracksScanner: AbstractTracksScanner
) {

    private val dataClient by lazy { Wearable.getDataClient(context) }
    private val capabilityClient by lazy { Wearable.getCapabilityClient(context) }

    @Volatile
    private var hasWatch: Boolean? = null
    private val isWatchConnected = MutableStateFlow(false)
    private var player: Player? = null
    private var controller: MediaController? = null
    private var releaseControllerJob: Job? = null
    private var publishPlayerJob: Job? = null
    private var artwork: Pair<String, Asset?>? = null

    private val listener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) {
            if (
                events.containsAny(
                    Player.EVENT_IS_PLAYING_CHANGED,
                    Player.EVENT_MEDIA_ITEM_TRANSITION,
                    Player.EVENT_MEDIA_METADATA_CHANGED,
                    Player.EVENT_POSITION_DISCONTINUITY,
                    Player.EVENT_PLAYBACK_STATE_CHANGED
                )
            ) {
                publishPlayerState()
            }
        }
    }

    init {
        ioScope.launch {
            isWatchConnected.value = hasWatch()
            isWatchConnected
                .flatMapLatest { connected ->
                    if (connected) {
                        combine(
                            abstractTracksScanner.latestTracks().filter { it.isNotEmpty() },
                            context.dataStore.data
                                .map { preferences ->
                                    listOf(
                                        preferences[THEME],
                                        preferences[PALETTE_STYLE],
                                        preferences[USE_ART_THEME],
                                        preferences[USE_SYSTEM_FONT]
                                    )
                                }
                                .distinctUntilChanged()
                        ) { tracks, _ -> tracks }
                    } else {
                        emptyFlow()
                    }
                }
                .debounce(500.milliseconds)
                .collectLatest { publishLibrary(it) }
        }
    }

    fun attach(player: Player) {
        this.player = player
        player.addListener(listener)
        publishPlayerState()
    }

    fun detach() {
        player?.removeListener(listener)
        player = null
        publishPlayerState()
    }

    suspend fun hasWatch(): Boolean {
        hasWatch?.let { return it }
        return runCatching {
            capabilityClient
                .getCapability(WatchContract.WATCH_CAPABILITY, CapabilityClient.FILTER_ALL)
                .await()
                .nodes
                .isNotEmpty()
        }.getOrDefault(false).also { hasWatch = it }
    }

    fun sync() {
        hasWatch = true
        isWatchConnected.value = true
        ioScope.launch { publishLibrary(latestTracks()) }
        publishPlayerState()
    }

    fun play(request: WatchPlayRequest) {
        ioScope.launch {
            val tracks = latestTracks().fastFilter { it.folder == request.folder }
            if (tracks.isEmpty()) return@launch
            val mediaItems = tracks.fastMap { it.toMediaItem() }

            withController {
                if (request.shuffle) {
                    setMediaItems(mediaItems)
                    prepare()
                    playRandom()
                } else {
                    val startIndex = tracks
                        .indexOfFirst { it.mediaId == request.mediaId }
                        .coerceAtLeast(0)
                    shuffleModeEnabled = false
                    setMediaItems(mediaItems, startIndex, 0)
                    prepare()
                    play()
                }
            }
        }
    }

    fun execute(command: WatchCommand) {
        ioScope.launch {
            when (command) {
                WatchCommand.PLAY_PAUSE -> withController { playOrPause() }
                WatchCommand.NEXT -> withController { seekToNextMediaItem() }
                WatchCommand.PREVIOUS -> withController { seekToPreviousMediaItem() }
            }
        }
    }

    fun seekTo(position: Long) {
        ioScope.launch { withController { seekTo(position) } }
    }

    @Synchronized
    private fun publishPlayerState() {
        publishPlayerJob?.cancel()
        publishPlayerJob = ioScope.launch {
            delay(150.milliseconds)
            if (!hasWatch()) return@launch

            val state = withContext(Dispatchers.Main) { playerSnapshot() }
            val artworkAsset = state.track?.let { track -> artworkFor(track.mediaId) }

            val request = PutDataMapRequest.create(WatchContract.PATH_PLAYER).apply {
                dataMap.putString(WatchContract.KEY_PLAYER, WatchContract.json.encodeToString(state))
                artworkAsset?.let { dataMap.putAsset(WatchContract.KEY_ARTWORK, it) }
            }.asPutDataRequest().setUrgent()

            runCatching { dataClient.putDataItem(request).await() }
        }
    }

    private fun playerSnapshot(): WatchPlayerState {
        val player = player
        val item = player?.currentMediaItem

        return WatchPlayerState(
            track = item?.let {
                WatchTrack(
                    mediaId = it.mediaId,
                    title = it.mediaMetadata.title?.toString().orEmpty(),
                    artist = it.mediaMetadata.artist?.toString().orEmpty()
                )
            },
            isPlaying = player?.isPlaying ?: false,
            position = player?.currentPosition ?: 0L,
            duration = player?.duration?.takeIf { it != C.TIME_UNSET && it > 0 } ?: 0L,
            updatedAt = System.currentTimeMillis()
        )
    }

    private suspend fun artworkFor(mediaId: String): Asset? {
        artwork?.takeIf { it.first == mediaId }?.let { return it.second }

        val artUri = withContext(Dispatchers.Main) {
            player?.currentMediaItem?.takeIf { it.mediaId == mediaId }?.mediaMetadata?.artworkUri
        }

        val asset = loadArtwork(artUri, ARTWORK_SIZE)
        artwork = mediaId to asset
        return asset
    }

    fun sendArtwork(mediaId: String) {
        ioScope.launch {
            val artUri = latestTracks().fastFirstOrNull { it.mediaId == mediaId }?.artUri
            val asset = artUri?.let { loadArtwork(it, THUMBNAIL_SIZE) }

            val request = PutDataMapRequest.create("${WatchContract.PATH_ARTWORK}/$mediaId").apply {
                dataMap.putString(WatchContract.KEY_MEDIA_ID, mediaId)
                asset?.let { dataMap.putAsset(WatchContract.KEY_ARTWORK, it) }
            }.asPutDataRequest().setUrgent()

            runCatching { dataClient.putDataItem(request).await() }
        }
    }

    private suspend fun loadArtwork(data: Any?, size: Int): Asset? {
        val request = ImageRequest.Builder(context)
            .data(data)
            .size(size)
            .allowHardware(false)
            .build()

        return runCatching {
            context.imageLoader.execute(request).image?.toBitmap()?.let { bitmap ->
                val stream = ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.JPEG, 85, stream)
                Asset.createFromBytes(stream.toByteArray())
            }
        }.getOrNull()
    }

    private suspend fun publishLibrary(tracks: List<CuteTrack>) {
        val folders = tracks
            .groupBy { it.folder }
            .toSortedMap()
            .map { (folder, folderTracks) ->
                WatchFolder(
                    path = folder,
                    name = folder.substringAfterLast('/'),
                    tracks = folderTracks.fastMap { WatchTrack(it.mediaId, it.title, it.artist) }
                )
            }

        val library = WatchLibrary(
            folders = folders,
            appearance = appearance()
        )

        val request = PutDataMapRequest.create(WatchContract.PATH_LIBRARY).apply {
            dataMap.putAsset(
                WatchContract.KEY_LIBRARY,
                Asset.createFromBytes(WatchContract.json.encodeToString(library).encodeToByteArray())
            )
        }.asPutDataRequest().setUrgent()

        runCatching { dataClient.putDataItem(request).await() }
    }

    private suspend fun appearance(): WatchAppearance {
        val preferences = context.dataStore.data.first()

        return WatchAppearance(
            seedColor = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                dynamicDarkColorScheme(context).primary.toArgb()
            } else null,
            paletteStyle = runCatching {
                (preferences[PALETTE_STYLE] ?: CutePaletteStyle.FIDELITY).toPaletteStyle().name
            }.getOrNull(),
            isAmoled = preferences[THEME] == CuteTheme.AMOLED,
            useArtTheme = preferences[USE_ART_THEME] ?: false,
            useSystemFont = preferences[USE_SYSTEM_FONT] ?: false
        )
    }

    private suspend fun latestTracks(): List<CuteTrack> =
        withTimeoutOrNull(5.seconds) {
            abstractTracksScanner.latestTracks().first { it.isNotEmpty() }
        }.orEmpty()

    private suspend fun withController(block: MediaController.() -> Unit) {
        withContext(Dispatchers.Main) {
            releaseControllerJob?.cancel()
            val mediaController = controller
                ?: runCatching { connectController() }.getOrNull()?.also { controller = it }
                ?: return@withContext

            mediaController.block()

            releaseControllerJob = ioScope.launch(Dispatchers.Main) {
                delay(10.seconds)
                controller?.release()
                controller = null
            }
        }
    }

    private suspend fun connectController(): MediaController =
        suspendCancellableCoroutine { continuation ->
            val future = MediaController
                .Builder(context, SessionToken(context, ComponentName(context, PlaybackService::class.java)))
                .buildAsync()

            future.addListener(
                {
                    runCatching { future.get() }
                        .onSuccess { continuation.resume(it) }
                        .onFailure { continuation.resumeWithException(it) }
                },
                MoreExecutors.directExecutor()
            )
            continuation.invokeOnCancellation { MediaController.releaseFuture(future) }
        }

    companion object {
        private const val ARTWORK_SIZE = 320
        private const val THUMBNAIL_SIZE = 128
    }
}
