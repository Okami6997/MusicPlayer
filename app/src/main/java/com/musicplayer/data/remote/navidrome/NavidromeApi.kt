package com.musicplayer.data.remote.navidrome

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Query

/**
 * Retrofit interface for the Navidrome native REST API.
 * Uses JWT Bearer token authentication and pagination via _start/_end/_sort/_order.
 */
interface NavidromeApi {

    @POST("auth/login")
    suspend fun login(
        @Body request: NavidromeLoginRequest
    ): NavidromeLoginResponse

    @GET("api/song")
    suspend fun getSongs(
        @Header("x-nd-authorization") authorization: String,
        @Query("_start") start: Int = 0,
        @Query("_end") end: Int = 500,
        @Query("_sort") sort: String = "title",
        @Query("_order") order: String = "ASC",
        @Query("missing") missing: Boolean = false
    ): Response<List<NavidromeSong>>

    @GET("api/playlist")
    suspend fun getPlaylists(
        @Header("x-nd-authorization") authorization: String,
        @Query("_start") start: Int = 0,
        @Query("_end") end: Int = 1000,
        @Query("_sort") sort: String = "name",
        @Query("_order") order: String = "ASC"
    ): Response<List<NavidromePlaylist>>

    @GET("api/transcoding")
    suspend fun getTranscodings(
        @Header("x-nd-authorization") authorization: String,
        @Query("_start") start: Int = 0,
        @Query("_end") end: Int = 1000,
        @Query("_sort") sort: String = "name",
        @Query("_order") order: String = "ASC"
    ): Response<List<NavidromeTranscoding>>

    @GET("api/album")
    suspend fun getAlbums(
        @Header("x-nd-authorization") authorization: String,
        @Query("_start") start: Int = 0,
        @Query("_end") end: Int = 500,
        @Query("_sort") sort: String = "name",
        @Query("_order") order: String = "ASC"
    ): Response<List<NavidromeAlbum>>

    @GET("api/artist")
    suspend fun getArtists(
        @Header("x-nd-authorization") authorization: String,
        @Query("_start") start: Int = 0,
        @Query("_end") end: Int = 500,
        @Query("_sort") sort: String = "name",
        @Query("_order") order: String = "ASC"
    ): Response<List<NavidromeArtist>>
}
