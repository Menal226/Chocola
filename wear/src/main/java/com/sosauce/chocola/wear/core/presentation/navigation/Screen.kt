package com.sosauce.chocola.wear.core.presentation.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
sealed class Screen : NavKey {
    @Serializable
    data object Folders : Screen()

    @Serializable
    data class FolderDetails(
        val path: String
    ) : Screen()

    @Serializable
    data object NowPlaying : Screen()
}
