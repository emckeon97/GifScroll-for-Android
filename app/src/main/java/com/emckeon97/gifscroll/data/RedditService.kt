package com.emckeon97.gifscroll.data

import com.emckeon97.gifscroll.model.FeedItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.IOException

/**
 * The main feed: funny memes, GIFs, and videos from Reddit's meme
 * communities, via the Arctic Shift mirror. Free, no API key —
 * Reddit's own endpoints now bot-block unauthenticated requests.
 */
object RedditService {
    private val client = OkHttpClient()
    private val subreddits = listOf("memes", "dankmemes", "funny")

    suspend fun memeFeed(): List<FeedItem> = withContext(Dispatchers.IO) {
        val sub = subreddits.random()
        val request = Request.Builder()
            .url("https://arctic-shift.photon-reddit.com/api/posts/search?subreddit=$sub&sort=desc&limit=50")
            .header("User-Agent", "GifScroll/1.0 (by /u/gifscroll)")
            .build()
        val body = client.newCall(request).execute().use { resp ->
            if (!resp.isSuccessful) throw IOException("HTTP ${resp.code}")
            resp.body?.string() ?: throw IOException("empty body")
        }
        parse(body)
    }

    private fun parse(json: String): List<FeedItem> {
        val out = mutableListOf<FeedItem>()
        val posts = JSONObject(json).optJSONArray("data") ?: return out
        for (i in 0 until posts.length()) {
            val p = posts.getJSONObject(i)
            if (p.optBoolean("over_18") || p.optBoolean("stickied")) continue
            val title = p.optString("title").trim()
            if (title.isEmpty() || title == "[deleted]" || title == "[removed]") continue
            val id = p.optString("id")
            if (id.isEmpty()) continue

            // Video posts: use the direct MP4 fallback, not the v.redd.it page URL.
            if (p.optBoolean("is_video")) {
                val fallback = p.optJSONObject("media")
                    ?.optJSONObject("reddit_video")
                    ?.optString("fallback_url")
                    ?.replace("&amp;", "&")
                if (!fallback.isNullOrEmpty()) {
                    out.add(FeedItem(id, title, FeedItem.Kind.VIDEO, fallback))
                    continue
                }
            }

            val url = p.optString("url")
            if (url.isEmpty()) continue
            val ext = url.substringAfterLast('.', "").substringBefore('?').lowercase()
            when (ext) {
                "gif" -> out.add(FeedItem(id, title, FeedItem.Kind.GIF, url))
                "jpg", "jpeg", "png" ->
                    out.add(FeedItem(id, title, FeedItem.Kind.IMAGE, url))
            }
        }
        return out
    }
}
