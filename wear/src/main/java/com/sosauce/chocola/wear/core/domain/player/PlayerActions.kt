package com.sosauce.chocola.wear.core.domain.player

sealed interface PlayerActions {
    data object Sync : PlayerActions
    data object PlayOrPause : PlayerActions
    data object SeekToNextMusic : PlayerActions
    data object SeekToPreviousMusic : PlayerActions
    data class SeekTo(val position: Long) : PlayerActions

    data class PlayFolder(
        val folder: String,
        val mediaId: String? = null,
        val shuffle: Boolean = false
    ) : PlayerActions
}
