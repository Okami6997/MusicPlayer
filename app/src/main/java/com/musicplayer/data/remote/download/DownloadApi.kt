package com.musicplayer.data.remote.download

import retrofit2.Response
import retrofit2.http.*

/**
 * Retrofit interface for the MusicSearch API.
 * Provides comprehensive search, download, and file management functionality.
 */
interface DownloadApi {

    // ==================== Search & Resolve ====================

    @GET("api/search")
    suspend fun search(
        @Query("q") query: String,
        @Query("limit") limit: Int = 20
    ): Response<DownloadSearchResponse>

    @GET("api/search/expand")
    suspend fun searchExpand(
        @Query("kind") kind: String,
        @Query("source") source: String,
        @Query("id") id: String
    ): Response<SearchExpandResponse>

    @GET("api/resolve")
    suspend fun resolveUrl(
        @Query("url") url: String
    ): Response<ResolveResponse>

    @GET("api/resolve/album")
    suspend fun expandAlbum(
        @Query("url") url: String
    ): Response<ExpandAlbumResponse>

    @GET("api/resolve/playlist")
    suspend fun expandPlaylist(
        @Query("url") url: String
    ): Response<ExpandPlaylistResponse>

    @GET("api/availability")
    suspend fun checkAvailability(
        @Query("url") url: String
    ): Response<AvailabilityResponse>

    @GET("api/preview")
    suspend fun previewAudio(
        @Query("url") url: String
    ): Response<okhttp3.ResponseBody>

    // ==================== Lyrics ====================

    @GET("api/lyrics")
    suspend fun fetchLyrics(
        @Query("track") track: String,
        @Query("artist") artist: String
    ): Response<LyricsResponse>

    // ==================== MusicBrainz Metadata ====================

    @GET("api/musicbrainz")
    suspend fun getMusicBrainzMetadata(
        @Query("isrc") isrc: String
    ): Response<MusicBrainzResponse>

    // ==================== Download ====================

    @POST("api/download")
    suspend fun downloadTrack(
        @Body request: DownloadTrackRequest
    ): Response<DownloadResponse>

    @POST("api/download/batch")
    suspend fun batchDownload(
        @Body request: BatchDownloadRequest
    ): Response<BatchDownloadResponse>

    @POST("api/download/album")
    suspend fun downloadAlbum(
        @Body request: AlbumDownloadRequest
    ): Response<AlbumDownloadResponse>

    @POST("api/download/playlist")
    suspend fun downloadPlaylist(
        @Body request: PlaylistDownloadRequest
    ): Response<PlaylistDownloadResponse>

    // ==================== Queue ====================

    @GET("api/queue")
    suspend fun getQueue(): Response<QueueResponse>

    @POST("api/queue/clear")
    suspend fun clearQueue(): Response<ClearQueueResponse>

    // ==================== Health & Providers ====================

    @GET("api/health")
    suspend fun healthCheck(): Response<HealthResponse>

    @GET("api/providers/health")
    suspend fun providersHealth(): Response<ProvidersHealthResponse>

    @GET("api/check-update")
    suspend fun checkUpdate(): Response<UpdateCheckResponse>

    // ==================== Analysis ====================

    @POST("api/analysis")
    suspend fun analyzeAudio(
        @Body request: AnalysisRequest
    ): Response<AnalysisResponse>

    @POST("api/analysis/batch")
    suspend fun batchAnalyze(
        @Body request: BatchAnalysisRequest
    ): Response<BatchAnalysisResponse>

    // ==================== Resample ====================

    @POST("api/resample")
    suspend fun resampleAudio(
        @Body request: ResampleRequest
    ): Response<ResampleResponse>

    @POST("api/resample/info")
    suspend fun getResampleInfo(
        @Body request: ResampleInfoRequest
    ): Response<ResampleInfoResponse>

    @GET("api/resample/schedule")
    suspend fun getResampleSchedule(): Response<ResampleScheduleResponse>

    @POST("api/resample/schedule")
    suspend fun createResampleSchedule(
        @Body request: ResampleScheduleRequest
    ): Response<ResampleScheduleResponse>

    @DELETE("api/resample/schedule")
    suspend fun deleteResampleSchedule(
        @Query("id") id: String
    ): Response<ResampleScheduleResponse>

    // ==================== Files ====================

    @GET("api/files/list")
    suspend fun listFiles(
        @Query("path") path: String
    ): Response<FileListResponse>

    @GET("api/files/audio")
    suspend fun listAudioFiles(
        @Query("path") path: String,
        @Query("offset") offset: Int = 0,
        @Query("limit") limit: Int = 100
    ): Response<AudioFileListResponse>

    @POST("api/files/metadata")
    suspend fun readFileMetadata(
        @Body request: FileMetadataRequest
    ): Response<FileMetadataResponse>

    @POST("api/files/rename/preview")
    suspend fun previewRename(
        @Body request: RenamePreviewRequest
    ): Response<RenamePreviewResponse>

    @POST("api/files/rename")
    suspend fun executeRename(
        @Body request: RenameRequest
    ): Response<RenameResponse>

    @POST("api/files/delete")
    suspend fun deleteFiles(
        @Body request: DeleteFilesRequest
    ): Response<DeleteFilesResponse>

    // ==================== History ====================

    @GET("api/history/downloads")
    suspend fun getDownloadHistory(): Response<DownloadHistoryResponse>

    @GET("api/history/fetches")
    suspend fun getFetchHistory(): Response<FetchHistoryResponse>

    @GET("api/history/operations")
    suspend fun getOperationHistory(): Response<OperationHistoryResponse>

    // ==================== Settings ====================

    @GET("api/settings")
    suspend fun getSettings(): Response<SettingsResponse>

    @POST("api/settings")
    suspend fun updateSettings(
        @Body request: UpdateSettingsRequest
    ): Response<UpdateSettingsResponse>
}

// ==================== Request Models ====================

data class DownloadTrackRequest(
    val url: String? = null,
    val isrc: String? = null
)

data class BatchDownloadRequest(
    val tracks: List<DownloadTrackRequest>
)

data class AlbumDownloadRequest(
    val url: String
)

data class PlaylistDownloadRequest(
    val url: String
)

data class DeleteFilesRequest(
    val filePaths: List<String>
)

data class ResampleScheduleRequest(
    val sourcePath: String,
    val targetPath: String,
    val targetSampleRate: Int,
    val scheduleTime: String? = null
)
