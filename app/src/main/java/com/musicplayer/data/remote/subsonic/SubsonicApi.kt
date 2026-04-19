package com.musicplayer.data.remote.subsonic

import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Retrofit interface for the Subsonic REST API.
 * Compatible with Subsonic, OpenSubsonic, and Navidrome.
 */
interface SubsonicApi {
    @GET("rest/ping")
    suspend fun ping(
        @Query("u") username: String,
        @Query("t") token: String,
        @Query("s") salt: String,
        @Query("v") version: String = "1.16.1",
        @Query("c") client: String = "MusicPlayer",
        @Query("f") format: String = "json"
    ): SubsonicResponse<Unit>

    @GET("rest/getArtists")
    suspend fun getArtists(
        @Query("u") username: String,
        @Query("t") token: String,
        @Query("s") salt: String,
        @Query("v") version: String = "1.16.1",
        @Query("c") client: String = "MusicPlayer",
        @Query("f") format: String = "json"
    ): SubsonicResponse<ArtistsResult>

    @GET("rest/getAlbumList2")
    suspend fun getAlbumList(
        @Query("u") username: String,
        @Query("t") token: String,
        @Query("s") salt: String,
        @Query("type") type: String = "alphabeticalByArtist",
        @Query("size") size: Int = 500,
        @Query("offset") offset: Int = 0,
        @Query("v") version: String = "1.16.1",
        @Query("c") client: String = "MusicPlayer",
        @Query("f") format: String = "json"
    ): SubsonicResponse<AlbumListResult>

    @GET("rest/getArtist")
    suspend fun getArtist(
        @Query("id") artistId: String,
        @Query("u") username: String,
        @Query("t") token: String,
        @Query("s") salt: String,
        @Query("v") version: String = "1.16.1",
        @Query("c") client: String = "MusicPlayer",
        @Query("f") format: String = "json"
    ): SubsonicResponse<ArtistResultDetails>

    @GET("rest/getAlbum")
    suspend fun getAlbum(
        @Query("id") albumId: String,
        @Query("u") username: String,
        @Query("t") token: String,
        @Query("s") salt: String,
        @Query("v") version: String = "1.16.1",
        @Query("c") client: String = "MusicPlayer",
        @Query("f") format: String = "json"
    ): SubsonicResponse<AlbumResult>

    @GET("rest/search3")
    suspend fun search(
        @Query("query") query: String,
        @Query("u") username: String,
        @Query("t") token: String,
        @Query("s") salt: String,
        @Query("songCount") songCount: Int = 50,
        @Query("albumCount") albumCount: Int = 20,
        @Query("artistCount") artistCount: Int = 20,
        @Query("v") version: String = "1.16.1",
        @Query("c") client: String = "MusicPlayer",
        @Query("f") format: String = "json"
    ): SubsonicResponse<SearchResult3>

    @GET("rest/getPlaylists")
    suspend fun getPlaylists(
        @Query("u") username: String,
        @Query("t") token: String,
        @Query("s") salt: String,
        @Query("v") version: String = "1.16.1",
        @Query("c") client: String = "MusicPlayer",
        @Query("f") format: String = "json"
    ): SubsonicResponse<PlaylistsResult>

    @GET("rest/getMusicFolders")
    suspend fun getMusicFolders(
        @Query("u") username: String,
        @Query("t") token: String,
        @Query("s") salt: String,
        @Query("v") version: String = "1.16.1",
        @Query("c") client: String = "MusicPlayer",
        @Query("f") format: String = "json"
    ): SubsonicResponse<MusicFoldersResult>

    @GET("rest/getMusicDirectory")
    suspend fun getMusicDirectory(
        @Query("id") id: String,
        @Query("u") username: String,
        @Query("t") token: String,
        @Query("s") salt: String,
        @Query("v") version: String = "1.16.1",
        @Query("c") client: String = "MusicPlayer",
        @Query("f") format: String = "json"
    ): SubsonicResponse<MusicDirectoryResult>

    @GET("rest/getPlaylist")
    suspend fun getPlaylist(
        @Query("id") playlistId: String,
        @Query("u") username: String,
        @Query("t") token: String,
        @Query("s") salt: String,
        @Query("v") version: String = "1.16.1",
        @Query("c") client: String = "MusicPlayer",
        @Query("f") format: String = "json"
    ): SubsonicResponse<PlaylistResult>

    @GET("rest/getRandomSongs")
    suspend fun getRandomSongs(
        @Query("u") username: String,
        @Query("t") token: String,
        @Query("s") salt: String,
        @Query("size") size: Int = 500,
        @Query("v") version: String = "1.16.1",
        @Query("c") client: String = "MusicPlayer",
        @Query("f") format: String = "json"
    ): SubsonicResponse<RandomSongsResult>

    @GET("rest/getStarred")
    suspend fun getStarred(
        @Query("u") username: String,
        @Query("t") token: String,
        @Query("s") salt: String,
        @Query("v") version: String = "1.16.1",
        @Query("c") client: String = "MusicPlayer",
        @Query("f") format: String = "json"
    ): SubsonicResponse<SearchResult3>

    @GET("rest/getGenres")
    suspend fun getGenres(
        @Query("u") username: String,
        @Query("t") token: String,
        @Query("s") salt: String,
        @Query("v") version: String = "1.16.1",
        @Query("c") client: String = "MusicPlayer",
        @Query("f") format: String = "json"
    ): SubsonicResponse<GenresResult>

    @GET("rest/getSongsByGenre")
    suspend fun getSongsByGenre(
        @Query("genre") genre: String,
        @Query("u") username: String,
        @Query("t") token: String,
        @Query("s") salt: String,
        @Query("count") count: Int = 500,
        @Query("v") version: String = "1.16.1",
        @Query("c") client: String = "MusicPlayer",
        @Query("f") format: String = "json"
    ): SubsonicResponse<SongsByGenreResult>

    @GET("rest/getIndexes")
    suspend fun getIndexes(
        @Query("u") username: String,
        @Query("t") token: String,
        @Query("s") salt: String,
        @Query("v") version: String = "1.16.1",
        @Query("c") client: String = "MusicPlayer",
        @Query("f") format: String = "json"
    ): SubsonicResponse<IndexesResult>

    @GET("rest/stream")
    suspend fun getStreamUrl(
        @Query("id") songId: String,
        @Query("u") username: String,
        @Query("t") token: String,
        @Query("s") salt: String,
        @Query("maxBitRate") maxBitRate: Int = 0,
        @Query("v") version: String = "1.16.1",
        @Query("c") client: String = "MusicPlayer"
    ): okhttp3.ResponseBody
}
