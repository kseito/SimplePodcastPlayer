package jp.kztproject.simplepodcastplayer.screen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.aakira.napier.Napier
import jp.kztproject.simplepodcastplayer.data.IAppleSearchApiClient
import jp.kztproject.simplepodcastplayer.data.Podcast
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class PodcastSearchViewModel(private val apiClient: IAppleSearchApiClient) : ViewModel() {

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
                Napier.d("Searching for: $query")
                val response = apiClient.searchPodcasts(term = query)
                Napier.d("Found ${response.results.size} podcasts")
                _podcasts.update { response.results }
            } catch (e: Exception) {
                Napier.e("Error occurred during search", e)
                _errorMessage.update { "検索中にエラーが発生しました: ${e.message}" }
                _podcasts.update { emptyList() }
            } finally {
                Napier.d("Loading finished")
                _isLoading.update { false }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        apiClient.close()
    }
}
