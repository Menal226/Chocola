package com.sosauce.chocola.app

import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService
import com.sosauce.chocola.core.data.watch.WatchBridge
import com.sosauce.chocola.shared.WatchCommand
import com.sosauce.chocola.shared.WatchContract
import com.sosauce.chocola.shared.WatchPlayRequest
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class WatchListenerService : WearableListenerService(), KoinComponent {

    private val watchBridge by inject<WatchBridge>()

    override fun onMessageReceived(messageEvent: MessageEvent) {
        val payload = messageEvent.data.decodeToString()

        when (messageEvent.path) {
            WatchContract.PATH_SYNC -> watchBridge.sync()
            WatchContract.PATH_PLAY -> runCatching {
                WatchContract.json.decodeFromString<WatchPlayRequest>(payload)
            }.onSuccess(watchBridge::play)

            WatchContract.PATH_COMMAND -> runCatching {
                WatchCommand.valueOf(payload)
            }.onSuccess(watchBridge::execute)

            WatchContract.PATH_SEEK -> payload.toLongOrNull()?.let(watchBridge::seekTo)
        }
    }
}
