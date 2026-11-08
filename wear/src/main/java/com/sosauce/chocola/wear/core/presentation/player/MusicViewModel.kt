package com.sosauce.chocola.wear.core.presentation.player

import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.ImageBitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sosauce.chocola.shared.WatchAppearance
import com.sosauce.chocola.shared.WatchCommand
import com.sosauce.chocola.shared.WatchLibrary
import com.sosauce.chocola.shared.WatchPlayRequest
import com.sosauce.chocola.shared.WatchPlayerState
import com.sosauce.chocola.shared.WatchFolder
import com.sosauce.chocola.wear.core.data.PhonePlayer
import com.sosauce.chocola.wear.core.data.PhoneRepository
import com.sosauce.chocola.wear.core.domain.player.PlayerActions
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.scan
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MusicViewModel(
    private val phoneRepository: PhoneRepository
) : ViewModel() {

    private val player = phoneRepository.player()
        .scan(PhonePlayer()) { latest, new ->
            if (new.state.updatedAt >= latest.state.updatedAt) new else latest
        }

    private val library: Flow<WatchLibrary?> = phoneRepository.library()
    private val isPhoneConnected: Flow<Boolean?> = phoneRepository.isPhoneConnected()

    val state = combine(
        library.onStart { emit(null) },
        player,
        isPhoneConnected.onStart { emit(null) }
    ) { library, player, isPhoneConnected ->
        MusicState(
            isLoading = library == null && isPhoneConnected != false,
            isPhoneConnected = isPhoneConnected != false,
            folders = library?.folders.orEmpty(),
            appearance = library?.appearance ?: WatchAppearance(),
            player = player.state,
            artwork = player.artwork
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        MusicState()
    )

    fun handlePlayerActions(action: PlayerActions) {
        viewModelScope.launch {
            when (action) {
                is PlayerActions.Sync -> phoneRepository.sync()
                is PlayerActions.PlayOrPause -> phoneRepository.command(WatchCommand.PLAY_PAUSE)
                is PlayerActions.SeekToNextMusic -> phoneRepository.command(WatchCommand.NEXT)
                is PlayerActions.SeekToPreviousMusic -> phoneRepository.command(WatchCommand.PREVIOUS)
                is PlayerActions.SeekTo -> phoneRepository.seekTo(action.position)
                is PlayerActions.PlayFolder -> phoneRepository.play(
                    WatchPlayRequest(
                        folder = action.folder,
                        mediaId = action.mediaId,
                        shuffle = action.shuffle
                    )
                )
            }
        }
    }
}

@Stable
data class MusicState(
    val isLoading: Boolean = true,
    val isPhoneConnected: Boolean = true,
    val folders: List<WatchFolder> = emptyList(),
    val appearance: WatchAppearance = WatchAppearance(),
    val player: WatchPlayerState = WatchPlayerState(),
    val artwork: ImageBitmap? = null
)
