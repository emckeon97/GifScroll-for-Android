package com.emckeon97.gifscroll.data

import com.emckeon97.gifscroll.Secrets
import com.emckeon97.gifscroll.model.FeedItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.IOException

/**
 * The main feed: trending GIFs from Klipy (https://api.klipy.com).
 */
object KlipyService {
    private val client = OkHttpClient()

    suspend fun memeFeed(): List<FeedItem> = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url("https://api.klipy.com/api/v1/${Secrets.KLIPY_API_KEY}/gifs/trending?per_page=50&page=1")
            .build()
        val body = client.newCall(request).execute().use { resp ->
            if (!resp.isSuccessful) throw IOException("HTTP ${resp.code}")
            resp.body?.string() ?: throw IOException("empty body")
        }
        parse(body)
    }

    private fun parse(json: String): List<FeedItem> {
        val out = mutableListOf<FeedItem>()
        val items = JSONObject(json)
            .optJSONObject("data")
            ?.optJSONArray("data") ?: return out
        for (i in 0 until items.length()) {
            val item = items.getJSONObject(i)
            // Klipy IDs are JSON numbers.
            val id = item.opt("id")?.toString() ?: continue
            if (id.isEmpty() || id == "null") continue
            val title = item.optString("title")
            val url = item.optJSONObject("file")
                ?.optJSONObject("md")
                ?.optJSONObject("mp4")
                ?.optString("url")
            if (url.isNullOrEmpty()) continue
            out.add(FeedItem(id, title, FeedItem.Kind.VIDEO, url))
        }
        return out
    }
}
