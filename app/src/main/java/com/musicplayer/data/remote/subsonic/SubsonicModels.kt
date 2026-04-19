package com.musicplayer.data.remote.subsonic

import com.google.gson.annotations.SerializedName

data class SubsonicResponse<T>(
    @SerializedName("subsonic-response") val response: SubsonicResponseBody<T>
)

data class SubsonicResponseBody<T>(
    val status: String,
    val version: String,
    val artists: ArtistsResult? = null,
    val artist: ArtistResultDetails? = null,
    val albumList2: AlbumListResult? = null,
    val album: AlbumResult? = null,
    val searchResult3: SearchResult3? = null,
    val searchResult2: SearchResult2? = null,
    val searchResult: SearchResult? = null,
    val playlists: PlaylistsResult? = null,
    val playlist: PlaylistResult? = null,
    val musicFolders: MusicFoldersResult? = null,
    val directory: MusicDirectoryResult? = null,
    val genres: GenresResult? = null,
    val songsByGenre: SongsByGenreResult? = null,
    val randomSongs: RandomSongsResult? = null,
    val indexes: IndexesResult? = null,
    val error: SubsonicError? = null
)

data class SearchResult2(
    val song: List<SubsonicSong>? = null
)

data class SearchResult(
    val song: List<SubsonicSong>? = null
)

data class IndexesResult(
    val lastModified: Long,
    val index: List<ArtistIndex>? = null,
    val child: List<SubsonicChild>? = null
)

data class GenresResult(
    val genre: List<SubsonicGenre>? = null
)

data class SubsonicGenre(
    val value: String,
    val songCount: Int? = null,
    val albumCount: Int? = null
)

data class SongsByGenreResult(
    val song: List<SubsonicSong>? = null
)

data class RandomSongsResult(
    val song: List<SubsonicSong>? = null
)

data class SubsonicError(
    val code: Int,
    val message: String
)

data class MusicFoldersResult(
    val musicFolder: List<MusicFolder>? = null
)

data class MusicFolder(
    val id: String,
    val name: String? = null
)

data class MusicDirectoryResult(
    val id: String,
    val name: String? = null,
    val child: List<SubsonicChild>? = null
)

data class SubsonicChild(
    val id: String,
    val parent: String? = null,
    val isDir: Boolean = false,
    val title: String? = null,
    val album: String? = null,
    val artist: String? = null,
    val track: Int? = null,
    val year: Int? = null,
    val genre: String? = null,
    val coverArt: String? = null,
    val duration: Int? = null,
    val bitRate: Int? = null,
    val size: Long? = null,
    val suffix: String? = null,
    val contentType: String? = null,
    val albumId: String? = null,
    val artistId: String? = null,
    val type: String? = null,
    val discNumber: Int? = null
)

data class ArtistsResult(
    val index: List<ArtistIndex>? = null
)

data class ArtistResultDetails(
    val id: String,
    val name: String? = null,
    val albumCount: Int? = null,
    val coverArt: String? = null,
    val album: List<SubsonicAlbum>? = null
)

data class ArtistIndex(
    val name: String? = null,
    val artist: List<SubsonicArtist>? = null
)

data class SubsonicArtist(
    val id: String,
    val name: String? = null,
    val albumCount: Int? = null,
    val coverArt: String? = null
)

data class AlbumListResult(
    val album: List<SubsonicAlbum>? = null
)

data class AlbumResult(
    val id: String,
    val name: String? = null,
    val artist: String? = null,
    val year: Int? = null,
    val coverArt: String? = null,
    val song: List<SubsonicSong>? = null
)

data class SubsonicAlbum(
    val id: String,
    val name: String? = null,
    val artist: String? = null,
    val year: Int? = null,
    val coverArt: String? = null,
    val songCount: Int? = null
)

data class SubsonicSong(
    val id: String,
    val title: String? = null,
    val artist: String? = null,
    val album: String? = null,
    val albumId: String? = null,
    val duration: Int? = null,
    val track: Int? = null,
    val discNumber: Int? = null,
    val year: Int? = null,
    val genre: String? = null,
    val coverArt: String? = null,
    val bitRate: Int? = null,
    val contentType: String? = null,
    val suffix: String? = null
)

data class SearchResult3(
    val artist: List<SubsonicArtist>? = null,
    val album: List<SubsonicAlbum>? = null,
    val song: List<SubsonicSong>? = null
)

data class PlaylistsResult(
    val playlist: List<SubsonicPlaylistSummary>? = null
)

data class SubsonicPlaylistSummary(
    val id: String,
    val name: String? = null,
    val comment: String? = null,
    val songCount: Int? = null,
    val duration: Int? = null,
    val coverArt: String? = null
)

data class PlaylistResult(
    val id: String,
    val name: String? = null,
    val comment: String? = null,
    val coverArt: String? = null,
    val entry: List<SubsonicSong>? = null
)
