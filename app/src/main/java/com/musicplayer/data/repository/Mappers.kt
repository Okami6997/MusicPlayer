package com.musicplayer.data.repository

import com.musicplayer.data.local.MediaSourceEntity
import com.musicplayer.data.local.PlaylistEntity
import com.musicplayer.data.local.TrackEntity
import com.musicplayer.domain.model.MediaSource
import com.musicplayer.domain.model.MediaSourceType
import com.musicplayer.domain.model.Playlist
import com.musicplayer.domain.model.Track

// ── Track ────────────────────────────────────────────────────────────────────

fun Track.toEntity(): TrackEntity = TrackEntity(
    id = id,
    title = title,
    artist = artist,
    albumArtist = albumArtist,
    album = album,
    albumId = albumId,
    duration = duration,
    trackNumber = trackNumber,
    discNumber = discNumber,
    year = year,
    genre = genre,
    uri = uri,
    artworkUri = artworkUri,
    sourceId = sourceId,
    sourceType = sourceType.name,
    isDownloaded = isDownloaded,
    downloadedUri = downloadedUri,
    bitrate = bitrate,
    sampleRate = sampleRate,
    fileSize = fileSize,
    codec = codec
)

fun TrackEntity.toDomain(): Track = Track(
    id = id,
    title = title,
    artist = artist,
    albumArtist = albumArtist,
    album = album,
    albumId = albumId,
    duration = duration,
    trackNumber = trackNumber,
    discNumber = discNumber,
    year = year,
    genre = genre,
    uri = uri,
    artworkUri = artworkUri,
    sourceId = sourceId,
    sourceType = MediaSourceType.valueOf(sourceType),
    isDownloaded = isDownloaded,
    downloadedUri = downloadedUri,
    bitrate = bitrate,
    sampleRate = sampleRate,
    fileSize = fileSize,
    codec = codec
)

// ── MediaSource ───────────────────────────────────────────────────────────────

fun MediaSource.toEntity(): MediaSourceEntity = MediaSourceEntity(
    id = id,
    name = name,
    type = type.name,
    baseUrl = baseUrl,
    username = username,
    password = password,
    token = token,
    localPath = localPath,
    isEnabled = isEnabled,
    lastSyncTime = lastSyncTime,
    artworkUri = artworkUri
)

fun MediaSourceEntity.toDomain(): MediaSource = MediaSource(
    id = id,
    name = name,
    type = MediaSourceType.valueOf(type),
    baseUrl = baseUrl,
    username = username,
    password = password,
    token = token,
    localPath = localPath,
    isEnabled = isEnabled,
    lastSyncTime = lastSyncTime,
    artworkUri = artworkUri
)

// ── Playlist ──────────────────────────────────────────────────────────────────

fun Playlist.toEntity(): PlaylistEntity = PlaylistEntity(
    id = id,
    name = name,
    artworkUri = artworkUri,
    sourceId = sourceId,
    sourceType = sourceType.name,
    isLocal = isLocal,
    description = description,
    trackCount = trackCount,
    duration = duration
)

fun PlaylistEntity.toDomain(tracks: List<Track> = emptyList()): Playlist = Playlist(
    id = id,
    name = name,
    artworkUri = artworkUri,
    sourceId = sourceId,
    sourceType = MediaSourceType.valueOf(sourceType),
    isLocal = isLocal,
    description = description,
    trackCount = trackCount,
    duration = duration,
    tracks = tracks
)
