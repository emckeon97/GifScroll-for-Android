package com.emckeon97.gifscroll.data

import android.content.Context
import com.emckeon97.gifscroll.model.FeedItem
import com.emckeon97.gifscroll.model.Repost
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.json.JSONArray
import java.util.UUID

/**
 * Feed memes the user shared to their personal page.
 * Works offline: persisted per user id locally ("local" when anonymous),
 * synced to the Supabase `reposts` table when signed in.
 */
class RepostService(context: Context) {
    private val prefs =
        context.getSharedPreferences("gifscroll.reposts", Context.MODE_PRIVATE)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val _reposts = MutableStateFlow<List<Repost>>(emptyList())
    val reposts: StateFlow<List<Repost>> = _reposts

    fun isShared(itemId: String): Boolean =
        _reposts.value.any { it.itemId == itemId }

    fun share(item: FeedItem, userId: String, signedIn: Boolean) {
        if (isShared(item.id)) return
        scope.launch {
            val repost = if (signedIn) {
                try {
                    SupabaseManager.insertRepost(
                        userId, item.id, item.title, item.url, item.kind.name
                    )
                } catch (e: Exception) {
                    localRepost(item)
                }
            } else {
                localRepost(item)
            }
            _reposts.value = (listOf(repost) + _reposts.value)
                .distinctBy { it.itemId }
                .sortedByDescending { it.createdAt }
            saveLocal(userId)
        }
    }

    fun unshare(repost: Repost, userId: String, signedIn: Boolean) {
        _reposts.value = _reposts.value.filter { it.id != repost.id }
        saveLocal(userId)
        if (signedIn) {
            scope.launch {
                try {
                    SupabaseManager.deleteRepost(repost.id)
                } catch (e: Exception) {
                    // Already gone locally; remote delete is best-effort.
                }
            }
        }
    }

    /** Loads from Supabase when signed in, otherwise from the local cache. */
    fun refresh(userId: String, signedIn: Boolean) {
        scope.launch {
            if (signedIn) {
                try {
                    _reposts.value = SupabaseManager.fetchReposts(userId)
                        .sortedByDescending { it.createdAt }
                    saveLocal(userId)
                    return@launch
                } catch (e: Exception) {
                    // Fall through to the local cache.
                }
            }
            loadLocal(userId)
        }
    }

    private fun localRepost(item: FeedItem) = Repost(
        id = UUID.randomUUID().toString(),
        itemId = item.id,
        title = item.title,
        url = item.url,
        kind = item.kind.name,
        createdAt = System.currentTimeMillis()
    )

    private fun keyFor(userId: String) = "reposts_$userId"

    private fun saveLocal(userId: String) {
        try {
            val arr = JSONArray()
            _reposts.value.forEach { arr.put(it.toJson()) }
            prefs.edit().putString(keyFor(userId), arr.toString()).apply()
        } catch (e: Exception) {
            // Best effort.
        }
    }

    private fun loadLocal(userId: String) {
        try {
            val raw = prefs.getString(keyFor(userId), null) ?: run {
                _reposts.value = emptyList()
                return
            }
            val arr = JSONArray(raw)
            _reposts.value = (0 until arr.length())
                .mapNotNull { Repost.fromJson(arr.getJSONObject(it)) }
                .sortedByDescending { it.createdAt }
        } catch (e: Exception) {
            _reposts.value = emptyList()
        }
    }
}
