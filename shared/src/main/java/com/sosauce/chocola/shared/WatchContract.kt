package com.sosauce.chocola.shared

import kotlinx.serialization.json.Json

object WatchContract {
    const val PHONE_CAPABILITY = "chocola_phone"
    const val WATCH_CAPABILITY = "chocola_watch"

    const val PATH_LIBRARY = "/chocola/library"
    const val PATH_PLAYER = "/chocola/player"

    const val PATH_SYNC = "/chocola/sync"
    const val PATH_PLAY = "/chocola/play"
    const val PATH_COMMAND = "/chocola/command"
    const val PATH_SEEK = "/chocola/seek"
    const val PATH_ARTWORK = "/chocola/artwork"

    const val KEY_LIBRARY = "library"
    const val KEY_PLAYER = "player"
    const val KEY_ARTWORK = "artwork"
    const val KEY_MEDIA_ID = "mediaId"

    val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }
}
