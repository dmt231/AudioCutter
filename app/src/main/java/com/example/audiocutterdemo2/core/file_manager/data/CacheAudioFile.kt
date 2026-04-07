package com.example.audiocutterdemo.core.file_manager.data

import com.example.audiocutterdemo.core.file_manager.TypeName

data class CacheAudioFile(
    val type: TypeName,
    val path: String,
    val name: String,
    val duration: Long,
    val size: Long,
)