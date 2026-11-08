package com.sosauce.chocola.shared

import kotlinx.serialization.Serializable

@Serializable
data class WatchTrack(
    val mediaId: String = "",
    val title: String = "",
    val artist: String = ""
)

@Serializable
data class WatchFolder(
    val path: String = "",
    val name: String = "",
    val tracks: List<WatchTrack> = emptyList()
)

@Serializable
data class WatchAppearance(
    val seedColor: Int? = null,
    val paletteStyle: String? = null,
    val isAmoled: Boolean = false,
    val useArtTheme: Boolean = false,
    val useSystemFont: Boolean = false
)

@Serializable
data class WatchLibrary(
    val folders: List<WatchFolder> = emptyList(),
    val appearance: WatchAppearance = WatchAppearance()
)

@Serializable
data class WatchPlayerState(
    val track: WatchTrack? = null,
    val isPlaying: Boolean = false,
    val position: Long = 0L,
    val duration: Long = 0L,
    val updatedAt: Long = 0L
)

@Serializable
data class WatchPlayRequest(
    val folder: String,
    val mediaId: String? = null,
    val shuffle: Boolean = false
)

enum class WatchCommand {
    PLAY_PAUSE,
    NEXT,
    PREVIOUS
}
