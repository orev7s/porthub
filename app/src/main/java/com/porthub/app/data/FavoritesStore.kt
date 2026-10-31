package com.porthub.app.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Favourites live in SharedPreferences — small enough that a database would be noise. */
class FavoritesStore(context: Context) {

    private val prefs = context.getSharedPreferences("porthub_favorites", Context.MODE_PRIVATE)
    private val _ids = MutableStateFlow(prefs.getStringSet(KEY, emptySet())?.toSet() ?: emptySet())

    val ids: StateFlow<Set<String>> = _ids.asStateFlow()

    fun toggle(id: String) {
        val next = _ids.value.toMutableSet()
        if (!next.add(id)) next.remove(id)
        _ids.value = next
        prefs.edit().putStringSet(KEY, next).apply()
    }

    fun isFavorite(id: String): Boolean = id in _ids.value

    private companion object {
        const val KEY = "ids"
    }
}
