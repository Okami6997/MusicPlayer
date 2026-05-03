package com.musicplayer.data.remote.download

import com.google.gson.annotations.SerializedName

// ==================== Search Response (Actual API Structure) ====================

data class DownloadSearchResponse(
    @SerializedName("q") val query: String? = null,
    @SerializedName("offset") val offset: Int? = null,
    @SerializedName("has_more") val hasMore: Boolean? = null,
    @SerializedName("source") val source: String? = null,
    @SerializedName("source_status") val sourceStatus: SourceStatus? = null,
    @SerializedName("albums") val albums: List<AlbumResult>? = null,
    @SerializedName("artists") val artists: List<ArtistResult>? = null,
    @SerializedName("tracks") val tracks: List<DownloadTrack>? = null,
    @SerializedName("spotify_tracks") val spotifyTracks: List<DownloadTrack>? = null,
    @SerializedName("youtube_tracks") val youtubeTracks: List<DownloadTrack>? = null,
    @SerializedName("soundcloud_tracks") val soundcloudTracks: List<DownloadTrack>? = null,
    @SerializedName("amazon_tracks") val amazonTracks: List<DownloadTrack>? = null,
    @SerializedName("itunes_tracks") val itunesTracks: List<DownloadTrack>? = null,
    @SerializedName("tidal_tracks") val tidalTracks: List<DownloadTrack>? = null,
    @SerializedName("deezer_tracks") val deezerTracks: List<DownloadTrack>? = null,
    @SerializedName("itunes_albums") val itunesAlbums: List<AlbumResult>? = null,
    @SerializedName("deezer_albums") val deezerAlbums: List<AlbumResult>? = null
)

data class SourceStatus(
    @SerializedName("qobuz_tracks") val qobuzTracks: ServiceStatus? = null,
    @SerializedName("tidal_tracks") val tidalTracks: ServiceStatus? = null
)

data class ServiceStatus(
    @SerializedName("status") val status: String? = null,
    @SerializedName("latency_ms") val latencyMs: Int? = null,
    @SerializedName("error") val error: String? = null
)

data class AlbumResult(
    @SerializedName("id") val id: String,
    @SerializedName("title") val title: String,
    @SerializedName("artist") val artist: String,
    @SerializedName("cover_url") val coverUrl: String? = null,
    @SerializedName("year") val year: String? = null,
    @SerializedName("release_date") val releaseDate: String? = null,
    @SerializedName("tracks_count") val tracksCount: Int? = null,
    @SerializedName("service") val service: String? = null,
    @SerializedName("source") val source: String? = null,
    @SerializedName("hires") val hires: Boolean? = null
)

data class ArtistResult(
    @SerializedName("id") val id: String,
    @SerializedName("name") val name: String,
    @SerializedName("image_url") val imageUrl: String? = null,
    @SerializedName("albums_count") val albumsCount: Int? = null,
    @SerializedName("service") val service: String? = null,
    @SerializedName("source") val source: String? = null
)

// ==================== Track Model ====================

data class DownloadTrack(
    @SerializedName("id") val id: String,
    @SerializedName("title") val title: String,
    @SerializedName("artist") val artist: String,
    @SerializedName("album") val album: String? = null,
    @SerializedName("duration_ms") val durationMs: Long? = null,
    @SerializedName("cover_url") val coverUrl: String? = null,
    @SerializedName("url") val url: String? = null,
    @SerializedName("year") val year: String? = null,
    @SerializedName("isrc") val isrc: String? = null,
    @SerializedName("service") val service: String? = null,
    @SerializedName("source") val source: String? = null,
    @SerializedName("hires") val hires: Boolean? = null,
    @SerializedName("bit_depth") val bitDepth: Int? = null,
    @SerializedName("sample_rate") val sampleRate: Int? = null,
    @SerializedName("preview_url") val previewUrl: String? = null,
    @SerializedName("asin") val asin: String? = null
) {
    val duration: Int?
        get() = durationMs?.let { (it / 1000).toInt() }
}

// ==================== Resolve ====================

data class ResolveResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("data") val data: ResolveData? = null,
    @SerializedName("message") val message: String? = null
)

data class ResolveData(
    @SerializedName("uniqueId") val uniqueId: String? = null,
    @SerializedName("isrc") val isrc: String? = null,
    @SerializedName("title") val title: String? = null,
    @SerializedName("artist") val artist: String? = null,
    @SerializedName("album") val album: String? = null,
    @SerializedName("links") val links: Map<String, PlatformLink>? = null
)

data class PlatformLink(
    @SerializedName("url") val url: String? = null,
    @SerializedName("country") val country: String? = null,
    @SerializedName("nativeUri") val nativeUri: String? = null
)

data class AvailabilityResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("data") val data: AvailabilityData? = null,
    @SerializedName("message") val message: String? = null
)

data class AvailabilityData(
    @SerializedName("url") val url: String? = null,
    @SerializedName("platforms") val platforms: Map<String, PlatformAvailability>? = null
)

data class PlatformAvailability(
    @SerializedName("available") val available: Boolean? = null,
    @SerializedName("url") val url: String? = null,
    @SerializedName("country") val country: String? = null
)

// ==================== Lyrics ====================

data class LyricsResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("data") val data: LyricsData? = null,
    @SerializedName("message") val message: String? = null
)

data class LyricsData(
    @SerializedName("syncedLyrics") val syncedLyrics: String? = null,
    @SerializedName("plainLyrics") val plainLyrics: String? = null,
    @SerializedName("track") val track: String? = null,
    @SerializedName("artist") val artist: String? = null,
    @SerializedName("album") val album: String? = null,
    @SerializedName("duration") val duration: Int? = null,
    @SerializedName("instrumental") val instrumental: Boolean? = null,
    @SerializedName("lang") val lang: String? = null
)

// ==================== MusicBrainz Metadata ====================

data class MusicBrainzResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("data") val data: MusicBrainzData? = null,
    @SerializedName("message") val message: String? = null
)

data class MusicBrainzData(
    @SerializedName("isrc") val isrc: String? = null,
    @SerializedName("title") val title: String? = null,
    @SerializedName("artist") val artist: String? = null,
    @SerializedName("album") val album: String? = null,
    @SerializedName("year") val year: Int? = null,
    @SerializedName("genre") val genre: List<String>? = null,
    @SerializedName("label") val label: String? = null
)

// ==================== Download ====================

data class DownloadResponse(
    @SerializedName("success") val success: Boolean? = null,
    @SerializedName("message") val message: String? = null,
    @SerializedName("track") val track: DownloadTrack? = null,
    @SerializedName("downloadId") val downloadId: String? = null,
    @SerializedName("task_id") val taskId: String? = null
) {
    val isSuccess: Boolean
        get() = success == true || taskId != null
}

data class BatchDownloadResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("message") val message: String? = null,
    @SerializedName("batchId") val batchId: String? = null,
    @SerializedName("tracks") val tracks: List<DownloadTrack>? = null
)

data class AlbumDownloadResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("message") val message: String? = null,
    @SerializedName("albumId") val albumId: String? = null,
    @SerializedName("tracks") val tracks: List<DownloadTrack>? = null
)

data class PlaylistDownloadResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("message") val message: String? = null,
    @SerializedName("playlistId") val playlistId: String? = null,
    @SerializedName("tracks") val tracks: List<DownloadTrack>? = null
)

// ==================== Queue ====================

data class QueueResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("queue") val queue: List<QueueItem>? = null,
    @SerializedName("message") val message: String? = null
)

data class QueueItem(
    @SerializedName("id") val id: String,
    @SerializedName("title") val title: String,
    @SerializedName("artist") val artist: String,
    @SerializedName("status") val status: String, // pending, downloading, completed, failed
    @SerializedName("progress") val progress: Int? = null,
    @SerializedName("addedAt") val addedAt: String? = null,
    @SerializedName("completedAt") val completedAt: String? = null
)

data class ClearQueueResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("message") val message: String? = null
)

// ==================== Analysis ====================

data class AnalysisRequest(
    @SerializedName("filePath") val filePath: String
)

data class AnalysisResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("data") val data: AnalysisData? = null,
    @SerializedName("message") val message: String? = null
)

data class AnalysisData(
    @SerializedName("filePath") val filePath: String? = null,
    @SerializedName("format") val format: String? = null,
    @SerializedName("duration") val duration: Double? = null,
    @SerializedName("sampleRate") val sampleRate: Int? = null,
    @SerializedName("channels") val channels: Int? = null,
    @SerializedName("bitDepth") val bitDepth: Int? = null,
    @SerializedName("bitrate") val bitrate: Int? = null,
    @SerializedName("codec") val codec: String? = null
)

data class BatchAnalysisRequest(
    @SerializedName("filePaths") val filePaths: List<String>
)

data class BatchAnalysisResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("results") val results: List<AnalysisData>? = null,
    @SerializedName("message") val message: String? = null
)

// ==================== Resample ====================

data class ResampleRequest(
    @SerializedName("sourcePath") val sourcePath: String,
    @SerializedName("targetPath") val targetPath: String,
    @SerializedName("targetSampleRate") val targetSampleRate: Int
)

data class ResampleResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("message") val message: String? = null,
    @SerializedName("sourcePath") val sourcePath: String? = null,
    @SerializedName("targetPath") val targetPath: String? = null
)

data class ResampleInfoRequest(
    @SerializedName("filePaths") val filePaths: List<String>
)

data class ResampleInfoResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("results") val results: List<ResampleFileInfo>? = null,
    @SerializedName("message") val message: String? = null
)

data class ResampleFileInfo(
    @SerializedName("filePath") val filePath: String? = null,
    @SerializedName("sampleRate") val sampleRate: Int? = null,
    @SerializedName("channels") val channels: Int? = null,
    @SerializedName("bitDepth") val bitDepth: Int? = null,
    @SerializedName("duration") val duration: Double? = null
)

// ==================== Files ====================

data class FileListResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("path") val path: String? = null,
    @SerializedName("files") val files: List<FileInfo>? = null,
    @SerializedName("directories") val directories: List<String>? = null,
    @SerializedName("message") val message: String? = null
)

data class FileInfo(
    @SerializedName("name") val name: String,
    @SerializedName("path") val path: String,
    @SerializedName("size") val size: Long? = null,
    @SerializedName("modified") val modified: String? = null,
    @SerializedName("isDirectory") val isDirectory: Boolean? = null
)

data class AudioFileListResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("path") val path: String? = null,
    @SerializedName("files") val files: List<AudioFileInfo>? = null,
    @SerializedName("message") val message: String? = null
)

data class AudioFileInfo(
    @SerializedName("name") val name: String,
    @SerializedName("path") val path: String,
    @SerializedName("size") val size: Long? = null,
    @SerializedName("duration") val duration: Double? = null,
    @SerializedName("format") val format: String? = null,
    @SerializedName("sampleRate") val sampleRate: Int? = null
)

data class FileMetadataRequest(
    @SerializedName("filePath") val filePath: String
)

data class FileMetadataResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("data") val data: AudioFileMetadata? = null,
    @SerializedName("message") val message: String? = null
)

data class AudioFileMetadata(
    @SerializedName("filePath") val filePath: String? = null,
    @SerializedName("title") val title: String? = null,
    @SerializedName("artist") val artist: String? = null,
    @SerializedName("album") val album: String? = null,
    @SerializedName("year") val year: Int? = null,
    @SerializedName("genre") val genre: String? = null,
    @SerializedName("trackNumber") val trackNumber: Int? = null,
    @SerializedName("totalTracks") val totalTracks: Int? = null,
    @SerializedName("duration") val duration: Double? = null,
    @SerializedName("format") val format: String? = null,
    @SerializedName("sampleRate") val sampleRate: Int? = null,
    @SerializedName("bitrate") val bitrate: Int? = null
)

data class RenamePreviewRequest(
    @SerializedName("filePaths") val filePaths: List<String>,
    @SerializedName("pattern") val pattern: String
)

data class RenamePreviewResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("changes") val changes: List<RenameChange>? = null,
    @SerializedName("message") val message: String? = null
)

data class RenameChange(
    @SerializedName("original") val original: String,
    @SerializedName("new") val new: String,
    @SerializedName("valid") val valid: Boolean
)

data class RenameRequest(
    @SerializedName("filePaths") val filePaths: List<String>,
    @SerializedName("pattern") val pattern: String
)

data class RenameResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("renamed") val renamed: List<String>? = null,
    @SerializedName("failed") val failed: List<String>? = null,
    @SerializedName("message") val message: String? = null
)

// ==================== History ====================

data class DownloadHistoryResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("history") val history: List<DownloadHistoryItem>? = null,
    @SerializedName("message") val message: String? = null
)

data class DownloadHistoryItem(
    @SerializedName("id") val id: String,
    @SerializedName("title") val title: String,
    @SerializedName("artist") val artist: String,
    @SerializedName("url") val url: String? = null,
    @SerializedName("downloadedAt") val downloadedAt: String? = null,
    @SerializedName("status") val status: String
)

data class ClearHistoryResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("message") val message: String? = null
)

data class OperationHistoryResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("history") val history: List<OperationHistoryItem>? = null,
    @SerializedName("message") val message: String? = null
)

data class OperationHistoryItem(
    @SerializedName("id") val id: String,
    @SerializedName("type") val type: String, // analysis, resample, rename
    @SerializedName("description") val description: String,
    @SerializedName("timestamp") val timestamp: String,
    @SerializedName("status") val status: String
)

// ==================== Settings ====================

data class SettingsResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("settings") val settings: Map<String, Any>? = null,
    @SerializedName("message") val message: String? = null
)

data class UpdateSettingsRequest(
    @SerializedName("settings") val settings: Map<String, Any>
)

data class UpdateSettingsResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("message") val message: String? = null
)

// ==================== Expand ====================

data class ExpandAlbumResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("album") val album: AlbumInfo? = null,
    @SerializedName("tracks") val tracks: List<DownloadTrack>? = null,
    @SerializedName("message") val message: String? = null
)

data class AlbumInfo(
    @SerializedName("title") val title: String? = null,
    @SerializedName("artist") val artist: String? = null,
    @SerializedName("year") val year: Int? = null,
    @SerializedName("coverArt") val coverArt: String? = null
)

data class ExpandPlaylistResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("playlist") val playlist: PlaylistInfo? = null,
    @SerializedName("tracks") val tracks: List<DownloadTrack>? = null,
    @SerializedName("message") val message: String? = null
)

data class PlaylistInfo(
    @SerializedName("title") val title: String? = null,
    @SerializedName("owner") val owner: String? = null,
    @SerializedName("trackCount") val trackCount: Int? = null
)

// ==================== Search Expand ====================

data class SearchExpandResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("data") val data: Any? = null,
    @SerializedName("message") val message: String? = null
)

// ==================== Health & Providers ====================

data class HealthResponse(
    @SerializedName("status") val status: String,
    @SerializedName("version") val version: String? = null,
    @SerializedName("timestamp") val timestamp: String? = null
)

data class ProvidersHealthResponse(
    @SerializedName("status") val status: String,
    @SerializedName("providers") val providers: Map<String, ProviderHealth>? = null
)

data class ProviderHealth(
    @SerializedName("status") val status: String,
    @SerializedName("latency_ms") val latencyMs: Int? = null,
    @SerializedName("error") val error: String? = null
)

data class UpdateCheckResponse(
    @SerializedName("has_update") val hasUpdate: Boolean,
    @SerializedName("current_version") val currentVersion: String? = null,
    @SerializedName("latest_version") val latestVersion: String? = null,
    @SerializedName("download_url") val downloadUrl: String? = null
)

// ==================== Resample Schedule ====================

data class ResampleScheduleResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("schedules") val schedules: List<ResampleSchedule>? = null,
    @SerializedName("message") val message: String? = null
)

data class ResampleSchedule(
    @SerializedName("id") val id: String,
    @SerializedName("sourcePath") val sourcePath: String,
    @SerializedName("targetPath") val targetPath: String,
    @SerializedName("targetSampleRate") val targetSampleRate: Int,
    @SerializedName("scheduleTime") val scheduleTime: String? = null,
    @SerializedName("status") val status: String
)

// ==================== Delete Files ====================

data class DeleteFilesResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("deleted") val deleted: List<String>? = null,
    @SerializedName("failed") val failed: List<String>? = null,
    @SerializedName("message") val message: String? = null
)

// ==================== Fetch History ====================

data class FetchHistoryResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("history") val history: List<FetchHistoryItem>? = null,
    @SerializedName("message") val message: String? = null
)

data class FetchHistoryItem(
    @SerializedName("id") val id: String,
    @SerializedName("query") val query: String,
    @SerializedName("timestamp") val timestamp: String,
    @SerializedName("resultsCount") val resultsCount: Int
)
