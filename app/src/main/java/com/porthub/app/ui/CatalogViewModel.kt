package com.porthub.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.porthub.app.data.CatalogRepository
import com.porthub.app.data.FavoritesStore
import com.porthub.app.data.Game
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class CatalogUiState(
    val games: List<Game> = emptyList(),
    val loading: Boolean = true,
    val refreshing: Boolean = false,
    val error: String? = null,
    val source: CatalogRepository.Source? = null,
)

class CatalogViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = CatalogRepository(app)
    private val favorites = FavoritesStore(app)

    private val _state = MutableStateFlow(CatalogUiState())
    val state: StateFlow<CatalogUiState> = _state.asStateFlow()

    val favoriteIds: StateFlow<Set<String>> = favorites.ids

    var feedUrl: String
        get() = repo.feedUrl
        set(value) {
            repo.feedUrl = value
        }

    init {
        load(force = false)
    }

    fun load(force: Boolean = true) {
        if (_state.value.refreshing) return
        _state.value = _state.value.copy(
            loading = _state.value.games.isEmpty(),
            refreshing = _state.value.games.isNotEmpty(),
            error = null,
        )
        viewModelScope.launch {
            repo.load(force)
                .onSuccess { (games, source) ->
                    _state.value = CatalogUiState(
                        games = games.sortedByDescending { it.stars ?: 0 },
                        loading = false,
                        refreshing = false,
                        error = null,
                        source = source,
                    )
                }
                .onFailure { error ->
                    _state.value = _state.value.copy(
                        loading = false,
                        refreshing = false,
                        error = error.message ?: "Could not load the catalog",
                    )
                }
        }
    }

    fun setFeed(url: String) {
        repo.feedUrl = url
        load(force = true)
    }

    fun toggleFavorite(id: String) = favorites.toggle(id)
}
