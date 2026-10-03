package com.emckeon97.gifscroll.data

import android.content.Context
import org.json.JSONObject

/**
 * Tracks laugh-reacts and learns which keywords the user likes,
 * so the feed can rank matching GIFs first. Persisted locally.
 */
class LikeManager(context: Context) {
    private val prefs =
        context.getSharedPreferences("gifscroll.likes", Context.MODE_PRIVATE)

    /** A laugh-reacted meme with enough media info to display it. */
    data class LikedMeme(
        val id: String,
        val title: String,
        val url: String?,
        val kind: String? // "IMAGE" | "GIF" | "VIDEO"
    )

    private val stopwords = setOf(
        "the", "a", "an", "and", "or", "for", "with", "you", "your",
        "this", "that", "these", "those", "from", "have", "has", "was",
        "were", "are", "is", "it", "its", "of", "to", "in", "on",
        "at", "be", "gif", "gifs"
    )

    fun isLiked(id: String): Boolean =
        prefs.getStringSet(KEY_IDS, emptySet())?.contains(id) == true

    /** How many items the user has laugh-reacted to. */
    fun likeCount(): Int =
        prefs.getStringSet(KEY_IDS, emptySet())?.size ?: 0

    fun toggleLike(id: String, title: String) {
        toggleLike(id, title, null, null)
    }

    fun toggleLike(id: String, title: String, url: String?, kind: String?) {
        val ids = prefs.getStringSet(KEY_IDS, emptySet())?.toMutableSet() ?: mutableSetOf()
        val scores = keywordScores().toMutableMap()
        val liked = likedMemes().toMutableMap()
        if (ids.contains(id)) {
            ids.remove(id)
            liked.remove(id)
            adjust(scores, title, -1)
        } else {
            ids.add(id)
            liked[id] = LikedMeme(id, title, url, kind)
            adjust(scores, title, 1)
        }
        val likedJson = JSONObject()
        liked.values.forEach {
            likedJson.put(it.id, JSONObject()
                .put("title", it.title)
                .put("url", it.url)
                .put("kind", it.kind))
        }
        prefs.edit()
            .putStringSet(KEY_IDS, ids)
            .putString(KEY_LIKED, likedJson.toString())
            .putString(KEY_SCORES, JSONObject(scores as Map<*, *>).toString())
            .apply()
    }

    /** Memes the user laugh-reacted to that have displayable media. */
    fun likedMemes(): Map<String, LikedMeme> {
        val raw = prefs.getString(KEY_LIKED, null) ?: return emptyMap()
        return try {
            val o = JSONObject(raw)
            o.keys().asSequence().associateWith { id ->
                val m = o.getJSONObject(id)
                LikedMeme(
                    id,
                    m.optString("title"),
                    m.optString("url").ifEmpty { null },
                    m.optString("kind").ifEmpty { null }
                )
            }
        } catch (e: Exception) {
            emptyMap()
        }
    }

    /** How strongly a title matches the user's liked keywords. Higher = show first. */
    fun score(title: String): Double =
        keywords(title).sumOf { keywordScores()[it] ?: 0.0 }

    private fun keywords(title: String): List<String> =
        title.lowercase()
            .split(Regex("[^a-z0-9]+"))
            .filter { it.length > 2 && it !in stopwords }

    private fun adjust(scores: MutableMap<String, Double>, title: String, by: Int) {
        keywords(title).forEach { scores[it] = (scores[it] ?: 0.0) + by }
    }

    private fun keywordScores(): Map<String, Double> {
        val raw = prefs.getString(KEY_SCORES, null) ?: return emptyMap()
        return try {
            val o = JSONObject(raw)
            o.keys().asSequence().associateWith { o.optDouble(it) }
        } catch (e: Exception) {
            emptyMap()
        }
    }

    companion object {
        private const val KEY_IDS = "liked_ids"
        private const val KEY_LIKED = "liked_memes"
        private const val KEY_SCORES = "keyword_scores"
    }
}
