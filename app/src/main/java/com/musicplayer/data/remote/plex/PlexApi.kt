package com.musicplayer.data.remote.plex

import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Retrofit interface for the Plex Media Server REST API.
 */
interface PlexApi {

    @GET("/")
    suspend fun getServerInfo(
        @Header("X-Plex-Token") token: String,
        @Header("Accept") accept: String = "application/json"
    ): PlexMediaContainer

    @GET("/library/sections")
    suspend fun getLibrarySections(
        @Header("X-Plex-Token") token: String,
        @Header("Accept") accept: String = "application/json"
    ): PlexMediaContainer

    @GET("/library/sections/{sectionKey}/all")
    suspend fun getSectionItems(
        @Header("X-Plex-Token") token: String,
        @Path("sectionKey") sectionKey: String,
        @Query("type") type: Int = 10,  // 10 = track
        @Query("X-Plex-Container-Start") start: Int = 0,
        @Query("X-Plex-Container-Size") size: Int = 500,
        @Header("Accept") accept: String = "application/json"
    ): PlexMediaContainer
}
