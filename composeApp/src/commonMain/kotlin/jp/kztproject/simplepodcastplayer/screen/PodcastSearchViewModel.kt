package jp.kztproject.simplepodcastplayer.screen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import jp.kztproject.simplepodcastplayer.data.AppleSearchApiClient
import jp.kztproject.simplepodcastplayer.data.Podcast
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class PodcastSearchViewModel : ViewModel() {
    private val apiClient = AppleSearchApiClient()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _podcasts = MutableStateFlow<List<Podcast>>(emptyList())
    val podcasts: StateFlow<List<Podcast>> = _podcasts.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    @Deprecated("Use searchQuery instead")
    val text = MutableStateFlow("")

    fun updateSearchQuery(query: String) {
        _searchQuery.update { query }
    }

    fun searchPodcasts() {
        val query = _searchQuery.value.trim()
        if (query.isBlank()) return

        viewModelScope.launch {
            _isLoading.update { true }
            _errorMessage.update { null }

            try {
                val response = apiClient.searchPodcasts(term = query)
                _podcasts.update { response.results }
            } catch (e: Exception) {
                _errorMessage.update { "検索中にエラーが発生しました: ${e.message}" }
                _podcasts.update { emptyList() }
            } finally {
                _isLoading.update { false }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        apiClient.close()
    }
}
