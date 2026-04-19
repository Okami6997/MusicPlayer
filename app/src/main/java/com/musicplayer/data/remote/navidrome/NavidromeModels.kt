package com.musicplayer.data.remote.navidrome

import com.google.gson.annotations.SerializedName

data class NavidromeLoginRequest(
    val username: String,
    val password: String
)

data class NavidromeLoginResponse(
    val id: String,
    val isAdmin: Boolean,
    val name: String,
    val token: String,
    val username: String
)

data class NavidromeSong(
    val id: String,
    val title: String? = null,
    val artist: String? = null,
    val artistId: String? = null,
    val album: String? = null,
    val albumId: String? = null,
    val albumArtist: String? = null,
    val albumArtistId: String? = null,
    val duration: Double? = null,
    val trackNumber: Int? = null,
    val discNumber: Int? = null,
    val year: Int? = null,
    val genre: String? = null,
    val genres: List<NavidromeGenreRef>? = null,
    val bitRate: Int? = null,
    val sampleRate: Int? = null,
    val channels: Int? = null,
    val size: Long? = null,
    val suffix: String? = null,
    val path: String? = null,
    val coverArtId: String? = null,
    @SerializedName("mediaFileId") val mediaFileId: String? = null,
    val playCount: Int? = null,
    val starred: Boolean? = null,
    val starredAt: String? = null,
    val rating: Int? = null,
    val missing: Boolean? = null,
    val createdAt: String? = null,
    val updatedAt: String? = null
)

data class NavidromeGenreRef(
    val id: String? = null,
    val name: String? = null
)

data class NavidromePlaylist(
    val id: String,
    val name: String? = null,
    val comment: String? = null,
    val songCount: Int? = null,
    val duration: Double? = null,
    val ownerName: String? = null,
    val ownerId: String? = null,
    @SerializedName("public") val isPublic: Boolean? = null,
    val coverArtId: String? = null,
    val createdAt: String? = null,
    val updatedAt: String? = null
)

data class NavidromeTranscoding(
    val id: String,
    val name: String? = null,
    val targetFormat: String? = null,
    val defaultBitRate: Int? = null,
    val command: String? = null
)

data class NavidromeAlbum(
    val id: String,
    val name: String? = null,
    val artist: String? = null,
    val artistId: String? = null,
    val albumArtist: String? = null,
    val albumArtistId: String? = null,
    val songCount: Int? = null,
    val duration: Double? = null,
    val year: Int? = null,
    val genre: String? = null,
    val coverArtId: String? = null,
    val createdAt: String? = null,
    val updatedAt: String? = null
)

data class NavidromeArtist(
    val id: String,
    val name: String? = null,
    val albumCount: Int? = null,
    val songCount: Int? = null,
    val coverArtId: String? = null,
    val size: Long? = null
)
