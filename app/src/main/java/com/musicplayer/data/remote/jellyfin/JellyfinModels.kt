package com.musicplayer.data.remote.jellyfin

import com.google.gson.annotations.SerializedName

data class JellyfinAuthResponse(
    @SerializedName("AccessToken") val accessToken: String,
    @SerializedName("ServerId") val serverId: String,
    @SerializedName("User") val user: JellyfinUser
)

data class JellyfinUser(
    @SerializedName("Id") val id: String,
    @SerializedName("Name") val name: String
)

data class JellyfinItemsResponse(
    @SerializedName("Items") val items: List<JellyfinItem>,
    @SerializedName("TotalRecordCount") val totalRecordCount: Int
)

data class JellyfinItem(
    @SerializedName("Id") val id: String,
    @SerializedName("Name") val name: String,
    @SerializedName("Type") val type: String,
    @SerializedName("AlbumArtist") val albumArtist: String = "",
    @SerializedName("Album") val album: String = "",
    @SerializedName("AlbumId") val albumId: String = "",
    @SerializedName("RunTimeTicks") val runTimeTicks: Long = 0,
    @SerializedName("IndexNumber") val indexNumber: Int = 0,
    @SerializedName("ParentIndexNumber") val parentIndexNumber: Int = 1,
    @SerializedName("ProductionYear") val productionYear: Int = 0,
    @SerializedName("Genres") val genres: List<String> = emptyList(),
    @SerializedName("ArtistItems") val artistItems: List<JellyfinArtistItem> = emptyList(),
    @SerializedName("ImageTags") val imageTags: Map<String, String> = emptyMap(),
    @SerializedName("MediaSources") val mediaSources: List<JellyfinMediaSource> = emptyList(),
    @SerializedName("Overview") val overview: String = "",
    // Epoch millis of the last time the item was modified on the server.
    // Used by the delta sync algorithm to skip items that haven't changed since the last sync.
    @SerializedName("DateModified") val dateModified: String? = null
)

data class JellyfinArtistItem(
    @SerializedName("Id") val id: String,
    @SerializedName("Name") val name: String
)

data class JellyfinMediaSource(
    @SerializedName("Id") val id: String,
    @SerializedName("Bitrate") val bitrate: Int = 0,
    @SerializedName("Size") val size: Long = 0,
    @SerializedName("Container") val container: String = ""
)

data class JellyfinPublicInfo(
    @SerializedName("ServerName") val serverName: String = "",
    @SerializedName("Version") val version: String = "",
    @SerializedName("Id") val id: String = ""
)

data class JellyfinAuthBody(
    @SerializedName("Username") val username: String,
    @SerializedName("Pw") val password: String
)
