package com.musicplayer.data.remote.plex

import com.google.gson.annotations.SerializedName

data class PlexMediaContainer(
    @SerializedName("MediaContainer") val mediaContainer: PlexContainer
)

data class PlexContainer(
    @SerializedName("size") val size: Int = 0,
    @SerializedName("totalSize") val totalSize: Int = 0,
    @SerializedName("Metadata") val metadata: List<PlexMetadata> = emptyList(),
    @SerializedName("Directory") val directories: List<PlexDirectory> = emptyList(),
    @SerializedName("machineIdentifier") val machineIdentifier: String = "",
    @SerializedName("version") val version: String = ""
)

data class PlexMetadata(
    @SerializedName("ratingKey") val ratingKey: String,
    @SerializedName("key") val key: String = "",
    @SerializedName("title") val title: String = "",
    @SerializedName("grandparentTitle") val grandparentTitle: String = "",
    @SerializedName("parentTitle") val parentTitle: String = "",
    @SerializedName("parentRatingKey") val parentRatingKey: String = "",
    @SerializedName("grandparentRatingKey") val grandparentRatingKey: String = "",
    @SerializedName("type") val type: String = "",
    @SerializedName("duration") val duration: Long = 0,
    @SerializedName("index") val index: Int = 0,
    @SerializedName("parentIndex") val parentIndex: Int = 1,
    @SerializedName("year") val year: Int = 0,
    @SerializedName("thumb") val thumb: String = "",
    @SerializedName("parentThumb") val parentThumb: String = "",
    @SerializedName("grandparentThumb") val grandparentThumb: String = "",
    @SerializedName("Media") val media: List<PlexMedia> = emptyList(),
    // Epoch seconds of the last update to this item on the server.
    // Used by the delta sync algorithm to skip items that haven't changed since the last sync.
    @SerializedName("updatedAt") val updatedAt: Long = 0L
)

data class PlexMedia(
    @SerializedName("id") val id: Int = 0,
    @SerializedName("duration") val duration: Long = 0,
    @SerializedName("bitrate") val bitrate: Int = 0,
    @SerializedName("container") val container: String = "",
    @SerializedName("Part") val parts: List<PlexPart> = emptyList()
)

data class PlexPart(
    @SerializedName("id") val id: Int = 0,
    @SerializedName("key") val key: String = "",
    @SerializedName("duration") val duration: Long = 0,
    @SerializedName("file") val file: String = "",
    @SerializedName("size") val size: Long = 0,
    @SerializedName("container") val container: String = ""
)

data class PlexDirectory(
    @SerializedName("key") val key: String = "",
    @SerializedName("title") val title: String = "",
    @SerializedName("type") val type: String = ""
)
