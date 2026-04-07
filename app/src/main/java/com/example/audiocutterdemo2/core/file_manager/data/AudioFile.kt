package com.example.audiocutterdemo2.core.file_manager.data

data class AudioFile(
    val id: Long,
    val uriString: String,
    val parentName: String,
    val relativePath: String,
    val name: String,
    val size: Long,
    val bitrate: Int,
    val duration: Long,
    val mimeType: String,
    val lastModified: Long,
)
