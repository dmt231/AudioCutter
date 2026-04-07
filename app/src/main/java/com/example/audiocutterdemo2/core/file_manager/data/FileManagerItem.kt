package com.example.audiocutterdemo.core.file_manager.data

import com.example.audiocutterdemo2.core.file_manager.data.AudioFile

enum class FileManagerState {
    Loading, Success, Empty, Failed
}

data class AudioFileItem(val state: FileManagerState, val list: List<AudioFile> = emptyList())