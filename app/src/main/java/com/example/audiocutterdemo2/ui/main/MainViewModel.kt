package com.example.audiocutterdemo2.ui.main

import android.net.Uri
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.audiocutterdemo.core.file_manager.IFileManager
import com.example.audiocutterdemo2.core.file_manager.data.AudioFile
import com.example.audiocutterdemo2.core.ffmpeg.DecoderManager
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import androidx.core.net.toUri
import com.example.audiocutterdemo2.core.decoder.PcmDecoder
import com.example.audiocutterdemo2.core.file_manager.data.TrimMode

class MainViewModel(
    private val fileManager: IFileManager,
    private val decoderManager: PcmDecoder
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

    fun onPickFileClick() {
        viewModelScope.launch { _navigationEvent.send(true) }
    }

    fun pickFiles(activity: ComponentActivity) {
        viewModelScope.launch {
            val resultFiles = fileManager.pickFiles(activity, isMultiple = false)
            if (resultFiles.isEmpty()) return@launch

            val uri    = resultFiles.first().uri
            val audioFile = fileManager.getAudioFile(uri) ?: return@launch

            _selectedFile.value = audioFile
            handleLMs  = 0L
            handleRMs  = audioFile.duration
            isDecoding = true
            _amplitudes.value = emptyList()

            _amplitudes.value = decoderManager.decode(
                context    = activity,
                uri        = uri.toUri(),
                targetBars = 1000
            )
            isDecoding = false
        }
    }
}