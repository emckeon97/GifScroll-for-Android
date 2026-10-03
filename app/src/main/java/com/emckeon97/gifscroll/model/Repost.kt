package com.emckeon97.gifscroll.model

import org.json.JSONObject

/** A feed meme the user shared to their personal page. */
data class Repost(
    val id: String,
    val itemId: String,
    val title: String,
    val url: String,
    val kind: String,
    val createdAt: Long
) {
    fun toJson(): JSONObject = JSONObject()
        .put("id", id)
        .put("item_id", itemId)
        .put("title", title)
        .put("url", url)
        .put("kind", kind)
        .put("created_at", createdAt)

    companion object {
        fun fromJson(o: JSONObject): Repost? {
            val id = o.optString("id").ifEmpty { return null }
            val url = o.optString("url").ifEmpty { return null }
            return Repost(
                id = id,
                itemId = o.optString("item_id"),
                title = o.optString("title"),
                url = url,
                kind = o.optString("kind").ifEmpty { "IMAGE" },
                createdAt = parseDate(o)
            )
        }

        /** Local cache stores epoch millis; Supabase stores ISO-8601. */
        private fun parseDate(o: JSONObject): Long {
            val raw = o.opt("created_at")
            if (raw is Number) return raw.toLong()
            return try {
                java.time.OffsetDateTime.parse(o.optString("created_at"))
                    .toInstant().toEpochMilli()
            } catch (e: Exception) {
                System.currentTimeMillis()
            }
        }
    }
}
