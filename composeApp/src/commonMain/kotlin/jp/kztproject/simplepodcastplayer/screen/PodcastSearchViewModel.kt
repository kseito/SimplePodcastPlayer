package jp.kztproject.simplepodcastplayer.screen

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

class PodcastSearchViewModel : ViewModel() {

    val text = MutableStateFlow("")

    init {
        text.update { "草不可避" }
    }
}
