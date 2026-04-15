package com.musicplayer.data.remote.subsonic

import com.google.gson.annotations.SerializedName

data class SubsonicResponse<T>(
    @SerializedName("subsonic-response") val response: SubsonicResponseBody<T>
)

data class SubsonicResponseBody<T>(
    val status: String,
    val version: String,
    val artists: ArtistsResult? = null,
    val albumList2: AlbumListResult? = null,
    val album: AlbumResult? = null,
    val searchResult3: SearchResult3? = null,
    val playlists: PlaylistsResult? = null,
    val playlist: PlaylistResult? = null,
    val error: SubsonicError? = null
)

data class SubsonicError(
    val code: Int,
    val message: String
)

data class ArtistsResult(
    val index: List<ArtistIndex>
)

data class ArtistIndex(
    val name: String,
    val artist: List<SubsonicArtist>
)

data class SubsonicArtist(
    val id: String,
    val name: String,
    val albumCount: Int = 0,
    val coverArt: String? = null
)

data class AlbumListResult(
    val album: List<SubsonicAlbum>
)

data class AlbumResult(
    val id: String,
    val name: String,
    val artist: String,
    val year: Int = 0,
    val coverArt: String? = null,
    val song: List<SubsonicSong> = emptyList()
)

data class SubsonicAlbum(
    val id: String,
    val name: String,
    val artist: String,
    val year: Int = 0,
    val coverArt: String? = null,
    val songCount: Int = 0
)

data class SubsonicSong(
    val id: String,
    val title: String,
    val artist: String,
    val album: String,
    val albumId: String = "",
    val duration: Int = 0,
    val track: Int = 0,
    val discNumber: Int = 1,
    val year: Int = 0,
    val genre: String = "",
    val coverArt: String? = null,
    val bitRate: Int = 0,
    val contentType: String = "",
    val suffix: String = ""
)

data class SearchResult3(
    val artist: List<SubsonicArtist> = emptyList(),
    val album: List<SubsonicAlbum> = emptyList(),
    val song: List<SubsonicSong> = emptyList()
)

data class PlaylistsResult(
    val playlist: List<SubsonicPlaylistSummary>
)

data class SubsonicPlaylistSummary(
    val id: String,
    val name: String,
    val comment: String = "",
    val songCount: Int = 0,
    val duration: Int = 0,
    val coverArt: String? = null
)

data class PlaylistResult(
    val id: String,
    val name: String,
    val comment: String = "",
    val coverArt: String? = null,
    val entry: List<SubsonicSong> = emptyList()
)
