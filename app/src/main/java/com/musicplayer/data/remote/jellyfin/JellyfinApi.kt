package com.musicplayer.data.remote.jellyfin

import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Retrofit interface for the Jellyfin / Emby REST API.
 * Both servers share the same Items-based API shape.
 */
interface JellyfinApi {

    @GET("System/Info/Public")
    suspend fun getPublicInfo(): JellyfinPublicInfo

    @GET("Users/AuthenticateByName")
    suspend fun authenticate(
        @Header("Authorization") authHeader: String,
        @retrofit2.http.Body body: JellyfinAuthBody
    ): JellyfinAuthResponse

    @GET("Items")
    suspend fun getItems(
        @Header("X-Emby-Token") token: String,
        @Query("IncludeItemTypes") includeItemTypes: String = "Audio",
        @Query("Recursive") recursive: Boolean = true,
        @Query("Fields") fields: String = "MediaSources,Genres,ArtistItems",
        @Query("StartIndex") startIndex: Int = 0,
        @Query("Limit") limit: Int = 500,
        @Query("SortBy") sortBy: String = "Album,SortName",
        @Query("SortOrder") sortOrder: String = "Ascending"
    ): JellyfinItemsResponse

    @GET("Items/{itemId}")
    suspend fun getItem(
        @Header("X-Emby-Token") token: String,
        @Path("itemId") itemId: String
    ): JellyfinItem
}
