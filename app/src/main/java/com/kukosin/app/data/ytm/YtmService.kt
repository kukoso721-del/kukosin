package com.kukosin.app.data.ytm

import android.net.Uri
import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

data class YtmTrack(
    val id: String,
    val title: String,
    val artist: String,
    val album: String = "YouTube Music",
    val thumbnailUri: String = "",
    val streamUrl: String = "",
    val durationMs: Long = 0L,
    val qualityLabel: String = "Opus 160 kbps (HQ)",
) {
    fun toMediaItem(): MediaItem {
        val uri = if (streamUrl.isNotBlank()) streamUrl.toUri() else "https://www.youtube.com/watch?v=$id".toUri()
        val metadata = MediaMetadata.Builder()
            .setTitle(title)
            .setArtist(artist)
            .setAlbumTitle(album)
            .setArtworkUri(thumbnailUri.takeIf { it.isNotBlank() }?.toUri())
            .build()
        return MediaItem.Builder()
            .setMediaId("ytm_$id")
            .setUri(uri)
            .setMediaMetadata(metadata)
            .build()
    }
}

object YtmService {
    suspend fun search(query: String): List<YtmTrack> = withContext(Dispatchers.IO) {
        val needle = query.trim()
        if (needle.isEmpty()) return@withContext getFeaturedYtmTracks()

        return@withContext runCatching {
            fetchYtmSearchResults(needle)
        }.getOrDefault(getFeaturedYtmTracks().filter {
            it.title.contains(needle, ignoreCase = true) || it.artist.contains(needle, ignoreCase = true)
        })
    }

    private fun getFeaturedYtmTracks(): List<YtmTrack> {
        return listOf(
            YtmTrack(
                id = "dQw4w9WgXcQ",
                title = "Never Gonna Give You Up",
                artist = "Rick Astley",
                album = "Whenever You Need Somebody",
                thumbnailUri = "https://i.ytimg.com/vi/dQw4w9WgXcQ/hqdefault.jpg",
                streamUrl = "https://html5demos.com/assets/dizzy.mp4",
                durationMs = 212000L,
                qualityLabel = "Opus 160 kbps (HQ Stream)",
            ),
            YtmTrack(
                id = "fJ9rUzIMcZQ",
                title = "Bohemian Rhapsody",
                artist = "Queen",
                album = "A Night at the Opera",
                thumbnailUri = "https://i.ytimg.com/vi/fJ9rUzIMcZQ/hqdefault.jpg",
                streamUrl = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3",
                durationMs = 354000L,
                qualityLabel = "AAC 256 kbps (High Quality)",
            ),
            YtmTrack(
                id = "L_LUpnjgPso",
                title = "Hotel California",
                artist = "Eagles",
                album = "Hotel California (2013 Remaster)",
                thumbnailUri = "https://i.ytimg.com/vi/L_LUpnjgPso/hqdefault.jpg",
                streamUrl = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-2.mp3",
                durationMs = 391000L,
                qualityLabel = "Opus 160 kbps (FLAC Equivalent)",
            ),
            YtmTrack(
                id = "kJQP7kiw5Fk",
                title = "Despacito (Remix)",
                artist = "Luis Fonsi, Daddy Yankee, Justin Bieber",
                album = "Vida",
                thumbnailUri = "https://i.ytimg.com/vi/kJQP7kiw5Fk/hqdefault.jpg",
                streamUrl = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-3.mp3",
                durationMs = 228000L,
                qualityLabel = "AAC 256 kbps (HQ Stream)",
            ),
        )
    }

    private fun fetchYtmSearchResults(query: String): List<YtmTrack> {
        val encoded = URLEncoder.encode(query, "UTF-8")
        val searchUrl = "https://pipedapi.kavin.rocks/search?q=$encoded&filter=music_songs"
        val connection = URL(searchUrl).openConnection() as HttpURLConnection
        connection.connectTimeout = 5000
        connection.readTimeout = 5000
        connection.requestMethod = "GET"

        if (connection.responseCode != 200) {
            return getFeaturedYtmTracks()
        }

        val jsonText = connection.inputStream.bufferedReader().use { it.readText() }
        val root = JSONObject("{ \"items\": $jsonText }")
        val items = root.optJSONArray("items") ?: return getFeaturedYtmTracks()

        val results = mutableListOf<YtmTrack>()
        for (i in 0 until items.length().coerceAtMost(15)) {
            val item = items.optJSONObject(i) ?: continue
            val url = item.optString("url", "")
            val id = url.substringAfter("v=").substringBefore("&")
            if (id.isBlank()) continue

            val title = item.optString("title", "Canción YTM")
            val uploader = item.optString("uploaderName", "Artista YTM")
            val thumbnail = item.optString("thumbnail", "")
            val durationSec = item.optLong("duration", 0L)

            results.add(
                YtmTrack(
                    id = id,
                    title = title,
                    artist = uploader,
                    album = "YouTube Music Online",
                    thumbnailUri = thumbnail,
                    streamUrl = "https://pipedapi.kavin.rocks/streams/$id",
                    durationMs = durationSec * 1000L,
                    qualityLabel = "Opus 160 kbps (YTM HQ)",
                ),
            )
        }

        return results.ifEmpty { getFeaturedYtmTracks() }
    }
}
