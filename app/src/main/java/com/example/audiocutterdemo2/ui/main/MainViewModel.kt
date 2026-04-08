package com.example.audiocutterdemo2.ui.main

import android.util.Log
import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.audiocutterdemo.core.file_manager.IFileManager
import com.example.audiocutterdemo2.core.file_manager.data.AudioFile
import com.example.audiocutterdemo2.core.ffmpeg.DecoderManager
import com.example.audiocutterdemo2.core.file_manager.data.TrimMode
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

class MainViewModel(
    private val fileManager: IFileManager,
    private val decoderManager: DecoderManager
) : ViewModel() {
    private val _selectedFile = MutableStateFlow<AudioFile?>(null)
    val selectedFile = _selectedFile.asStateFlow()

    private val _amplitudes = MutableStateFlow<List<Int>>(emptyList())
    val amplitudes = _amplitudes.asStateFlow()

    var isDecoding  by mutableStateOf(false)
    var handleLMs   by mutableLongStateOf(0L)
    var handleRMs   by mutableLongStateOf(0L)
    var trimMode    by mutableStateOf(TrimMode.TRIM_SIDE)

    private val _navigationEvent = Channel<Boolean>(Channel.BUFFERED)
    val navigationEvent = _navigationEvent.receiveAsFlow()

    init {
        viewModelScope.launch {
            decoderManager.frames.collect { framesFloat ->
                if (framesFloat != null) {
                    val validIntAmps = framesFloat
                        .filter { it >= 0f }
                        .map { it.toInt() }

                    _amplitudes.value = validIntAmps
                }
            }
        }
    }

    fun onPickFileClick() {
        viewModelScope.launch { _navigationEvent.send(true) }
    }

    fun pickFiles(activity: ComponentActivity) {
        viewModelScope.launch {
            val resultFiles = fileManager.pickFiles(activity, isMultiple = false)
            if (resultFiles.isEmpty()) return@launch

            val uri  = resultFiles.first().uri
            val audioFile = fileManager.getAudioFile(uri) ?: return@launch

            _selectedFile.value = audioFile
            handleLMs  = 0L
            handleRMs  = audioFile.duration
            isDecoding = true
            _amplitudes.value = emptyList()

            val inputStream = activity.contentResolver.openInputStream(uri.toUri())

            if (inputStream != null) {
                try {
                    decoderManager.loadFramesWithInputStream(
                        uriString = uri,
                        inputStream = inputStream
                    )
                } catch (e: Exception) {
                    Log.e("MainViewModel", "${e.message}")
                } finally {
                    inputStream.close()
                }
            }
            isDecoding = false
        }
    }
}