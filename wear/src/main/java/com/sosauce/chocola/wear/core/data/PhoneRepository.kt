package com.sosauce.chocola.wear.core.data

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.LruCache
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.google.android.gms.wearable.Asset
import com.google.android.gms.wearable.CapabilityClient
import com.google.android.gms.wearable.DataClient
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataMap
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.Node
import com.google.android.gms.wearable.Wearable
import com.sosauce.chocola.shared.WatchCommand
import com.sosauce.chocola.shared.WatchContract
import com.sosauce.chocola.shared.WatchLibrary
import com.sosauce.chocola.shared.WatchPlayRequest
import com.sosauce.chocola.shared.WatchPlayerState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.time.Duration.Companion.seconds

data class PhonePlayer(
    val state: WatchPlayerState = WatchPlayerState(),
    val artwork: ImageBitmap? = null
)

class PhoneRepository(
    context: Context
) {

    private val dataClient = Wearable.getDataClient(context)
    private val messageClient = Wearable.getMessageClient(context)
    private val capabilityClient = Wearable.getCapabilityClient(context)
    private val artworkCache = LruCache<String, ImageBitmap>(ARTWORK_CACHE_SIZE)

    fun library(): Flow<WatchLibrary> = dataItems(WatchContract.PATH_LIBRARY) { dataMap ->
        dataMap.getAsset(WatchContract.KEY_LIBRARY)
            ?.let { readAsset(it) }
            ?.let { WatchContract.json.decodeFromString<WatchLibrary>(it.decodeToString()) }
    }

    fun player(): Flow<PhonePlayer> = dataItems(WatchContract.PATH_PLAYER) { dataMap ->
        val state = dataMap.getString(WatchContract.KEY_PLAYER)
            ?.let { WatchContract.json.decodeFromString<WatchPlayerState>(it) }
            ?: return@dataItems null

        PhonePlayer(
            state = state,
            artwork = dataMap.getAsset(WatchContract.KEY_ARTWORK)
                ?.let { readAsset(it) }
                ?.let { BitmapFactory.decodeByteArray(it, 0, it.size)?.asImageBitmap() }
        )
    }

    fun isPhoneConnected(): Flow<Boolean> = callbackFlow {
        val listener = CapabilityClient.OnCapabilityChangedListener { info ->
            trySend(info.nodes.isNotEmpty())
        }
        capabilityClient.addListener(listener, WatchContract.PHONE_CAPABILITY)
        trySend(phoneNode() != null)
        awaitClose { capabilityClient.removeListener(listener) }
    }

    suspend fun sync() = send(WatchContract.PATH_SYNC, ByteArray(0))

    suspend fun play(request: WatchPlayRequest) =
        send(WatchContract.PATH_PLAY, WatchContract.json.encodeToString(request).encodeToByteArray())

    suspend fun command(command: WatchCommand) =
        send(WatchContract.PATH_COMMAND, command.name.encodeToByteArray())

    suspend fun seekTo(position: Long) =
        send(WatchContract.PATH_SEEK, position.toString().encodeToByteArray())

    suspend fun artwork(mediaId: String): ImageBitmap? = withContext(Dispatchers.IO) {
        artworkCache.get(mediaId)?.let { return@withContext it }

        val path = "${WatchContract.PATH_ARTWORK}/$mediaId"
        val dataMap = dataMap(path) ?: run {
            send(WatchContract.PATH_ARTWORK, mediaId.encodeToByteArray())
            withTimeoutOrNull(20.seconds) { dataItems(path) { it }.first() }
        }

        dataMap?.getAsset(WatchContract.KEY_ARTWORK)
            ?.let { readAsset(it) }
            ?.let { BitmapFactory.decodeByteArray(it, 0, it.size)?.asImageBitmap() }
            ?.also { artworkCache.put(mediaId, it) }
    }

    private suspend fun send(path: String, data: ByteArray): Boolean {
        val node = phoneNode() ?: return false
        return runCatching { messageClient.sendMessage(node.id, path, data).await() }.isSuccess
    }

    private suspend fun phoneNode(): Node? = runCatching {
        capabilityClient
            .getCapability(WatchContract.PHONE_CAPABILITY, CapabilityClient.FILTER_REACHABLE)
            .await()
            .nodes
            .let { nodes -> nodes.firstOrNull { it.isNearby } ?: nodes.firstOrNull() }
    }.getOrNull()

    private suspend fun readAsset(asset: Asset): ByteArray? = runCatching {
        dataClient.getFdForAsset(asset).await().inputStream.use { it.readBytes() }
    }.getOrNull()

    private fun <T> dataItems(
        path: String,
        transform: suspend (DataMap) -> T?
    ): Flow<T> = callbackFlow {
        val uri = dataItemUri(path)

        val listener = DataClient.OnDataChangedListener { events ->
            val dataMap = events
                .filter { it.type == DataEvent.TYPE_CHANGED && it.dataItem.uri.path == path }
                .map { DataMapItem.fromDataItem(it.dataItem).dataMap }
                .lastOrNull()
                ?: return@OnDataChangedListener

            launch { transform(dataMap)?.let { send(it) } }
        }
        dataClient.addListener(listener, uri, DataClient.FILTER_LITERAL)

        dataMap(path)?.let { transform(it) }?.let { send(it) }

        awaitClose { dataClient.removeListener(listener) }
    }.flowOn(Dispatchers.IO)

    private suspend fun dataMap(path: String): DataMap? =
        runCatching { dataClient.getDataItems(dataItemUri(path)).await() }.getOrNull()?.let { buffer ->
            val dataMap = buffer.map { DataMapItem.fromDataItem(it).dataMap }.lastOrNull()
            buffer.release()
            dataMap
        }

    private fun dataItemUri(path: String): Uri =
        Uri.Builder().scheme("wear").authority("*").path(path).build()

    companion object {
        private const val ARTWORK_CACHE_SIZE = 96
    }
}
