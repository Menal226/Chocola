package com.sosauce.chocola.app

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.util.LruCache
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.FlagSet
import androidx.media3.common.ForwardingPlayer
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import coil3.imageLoader
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.toBitmap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

@OptIn(UnstableApi::class)
class EmbeddedArtworkPlayer(
    player: Player,
    private val context: Context,
    private val scope: CoroutineScope
) : ForwardingPlayer(player) {

    private val listeners = mutableSetOf<Player.Listener>()
    private val artworks = LruCache<Uri, ByteArray>(ARTWORK_CACHE_SIZE)
    private val artworkJobs = mutableMapOf<Uri, Job>()

    init {
        player.addListener(object : Player.Listener {
            override fun onEvents(player: Player, events: Player.Events) {
                if (
                    events.containsAny(
                        Player.EVENT_MEDIA_ITEM_TRANSITION,
                        Player.EVENT_MEDIA_METADATA_CHANGED,
                        Player.EVENT_TIMELINE_CHANGED,
                        Player.EVENT_SHUFFLE_MODE_ENABLED_CHANGED,
                        Player.EVENT_REPEAT_MODE_CHANGED
                    )
                ) {
                    loadArtworks()
                }
                if (events.contains(Player.EVENT_MEDIA_METADATA_CHANGED) && hasLoadedArtwork()) {
                    scope.launch { notifyMediaMetadataChanged() }
                }
            }
        })
    }

    override fun addListener(listener: Player.Listener) {
        super.addListener(listener)
        listeners += listener
    }

    override fun removeListener(listener: Player.Listener) {
        super.removeListener(listener)
        listeners -= listener
    }

    override fun getMediaMetadata(): MediaMetadata {
        val metadata = super.getMediaMetadata()
        if (metadata.artworkData != null) return metadata.buildUpon().setArtworkUri(null).build()

        val artworkData = metadata.artworkUri
            ?.let { artworks.get(it) }
            ?.takeIf { it.isNotEmpty() }
            ?: return metadata

        return metadata.buildUpon()
            .setArtworkUri(null)
            .setArtworkData(artworkData, MediaMetadata.PICTURE_TYPE_FRONT_COVER)
            .build()
    }

    private fun hasLoadedArtwork(): Boolean {
        val metadata = super.getMediaMetadata()
        return metadata.artworkData == null &&
            metadata.artworkUri?.let { artworks.get(it) }?.isNotEmpty() == true
    }

    private fun loadArtworks() {
        listOf(currentMediaItemIndex, nextMediaItemIndex, previousMediaItemIndex)
            .filter { it != C.INDEX_UNSET && it < mediaItemCount }
            .map { getMediaItemAt(it).mediaMetadata }
            .filter { it.artworkData == null }
            .mapNotNull { it.artworkUri }
            .forEach(::loadArtwork)
    }

    private fun loadArtwork(artworkUri: Uri) {
        if (artworks.get(artworkUri) != null || artworkJobs[artworkUri]?.isActive == true) return

        artworkJobs[artworkUri] = scope.launch {
            val request = ImageRequest.Builder(context)
                .data(artworkUri)
                .size(ARTWORK_SIZE)
                .allowHardware(false)
                .build()

            val artworkData = withContext(Dispatchers.Default) {
                runCatching {
                    context.imageLoader.execute(request).image?.toBitmap()?.let { bitmap ->
                        val stream = ByteArrayOutputStream()
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 85, stream)
                        stream.toByteArray()
                    }
                }.getOrNull()
            }

            artworks.put(artworkUri, artworkData ?: ByteArray(0))
            artworkJobs.remove(artworkUri)
            if (artworkData != null && super.getMediaMetadata().artworkUri == artworkUri) {
                notifyMediaMetadataChanged()
            }
        }
    }

    private fun notifyMediaMetadataChanged() {
        val metadata = mediaMetadata
        val events = Player.Events(FlagSet.Builder().add(Player.EVENT_MEDIA_METADATA_CHANGED).build())
        listeners.toList().forEach { listener ->
            listener.onMediaMetadataChanged(metadata)
            listener.onEvents(this, events)
        }
    }

    companion object {
        private const val ARTWORK_SIZE = 512
        private const val ARTWORK_CACHE_SIZE = 8
    }
}
