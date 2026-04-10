package com.example.audiocutterdemo2.ui.screen.main

import android.app.Application
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.net.toUri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import com.example.audiocutterdemo.core.ffmpeg.FFMpegController
import com.example.audiocutterdemo.core.ffmpeg.model.AudioCut
import com.example.audiocutterdemo.core.file_manager.IFileManager
import com.example.audiocutterdemo2.core.file_manager.data.AudioFile
import com.example.audiocutterdemo2.core.ffmpeg.DecoderManager
import com.example.audiocutterdemo2.core.file_manager.data.TrimMode
import com.example.audiocutterdemo2.permission.PermissionChecker
import com.example.audiocutterdemo2.ui.screen.main.data.ZoomLevel
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class MainViewModel(
    application: Application,
    private val fileManager: IFileManager,
    private val decoderManager: DecoderManager,
    private val permissionChecker: PermissionChecker,
    private val ffMpegController: FFMpegController,
) : AndroidViewModel(application) {

    private val _selectedFile = MutableStateFlow<AudioFile?>(null)
    val selectedFile = _selectedFile.asStateFlow()

    private val _amplitudes = MutableStateFlow<List<Int>>(emptyList())
    val amplitudes = _amplitudes.asStateFlow()

    var currentPlaybackMs by mutableLongStateOf(0L)
    var isPlaying by mutableStateOf(false)

    private var playbackJob: Job? = null

    private val exoPlayer: ExoPlayer = ExoPlayer.Builder(application).build()

    var isDecoding by mutableStateOf(false)
    var handleLMs by mutableLongStateOf(0L)
    var handleRMs by mutableLongStateOf(0L)
    var trimMode by mutableStateOf(TrimMode.TRIM_SIDE)

    private val _navigationEvent = Channel<Boolean>(Channel.BUFFERED)
    val navigationEvent = _navigationEvent.receiveAsFlow()

    private val _permissionRequestEvent = Channel<String>(Channel.BUFFERED)
    val permissionRequestEvent = _permissionRequestEvent.receiveAsFlow()

    //Zoom state
    private val _zoomState = MutableStateFlow<ZoomLevel>(ZoomLevel.Small)
    val zoomState = _zoomState.asStateFlow()

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

    fun togglePlayPause() {
        if (isPlaying) pauseAudio() else playAudio()
    }

    fun pauseAudio() {
        exoPlayer.pause()
        isPlaying = false
        playbackJob?.cancel()
    }

    fun seekTo(positionMs: Long) {
        exoPlayer.seekTo(positionMs)
        currentPlaybackMs = positionMs
    }

    fun onPickFileClick() {
        viewModelScope.launch {
            val requiredPermission = permissionChecker.getRequiredAudioPermission()
            if (permissionChecker.hasAudioPermission()) {
                _navigationEvent.send(true)
            } else {
                _permissionRequestEvent.send(requiredPermission)
            }
        }
    }

    fun onPermissionResult(isGranted: Boolean) {
        if (isGranted) {
            viewModelScope.launch { _navigationEvent.send(true) }
        } else {
            Log.d("MainViewModel", "Permission denied by user")
        }
    }

    fun pickFiles(activity: ComponentActivity) {
        viewModelScope.launch {
            val resultFiles = fileManager.pickFiles(activity, isMultiple = false)
            if (resultFiles.isEmpty()) return@launch

            val uri = resultFiles.first().uri
            val audioFile = fileManager.getAudioFile(uri) ?: return@launch

            _selectedFile.value = audioFile
            handleLMs = 0L
            handleRMs = audioFile.duration
            isDecoding = true
            _amplitudes.value = emptyList()

            exoPlayer.apply {
                setMediaItem(MediaItem.fromUri(uri))
                prepare()
                seekTo(0)
            }

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

    fun confirmCut(){
        val file = _selectedFile.value ?: return
        val splits = when(trimMode){
            TrimMode.TRIM_SIDE -> listOf(
                AudioCut(handleLMs, handleRMs)
            )
            TrimMode.TRIM_MIDDLE -> listOf(
                AudioCut(0, handleLMs),
                AudioCut(handleRMs, file.duration)
            )
        }
        //ffmpeg cut here
    }

    fun zoomIn(){
        var currentValue = _zoomState.value
        if(currentValue == ZoomLevel.Big) return
        when(currentValue){
            ZoomLevel.Small -> currentValue = ZoomLevel.Medium
            ZoomLevel.Medium -> currentValue = ZoomLevel.Big
            else -> {}
        }
        updateState(currentValue)
    }

    fun zoomOut(){
        var currentValue = _zoomState.value
        if(currentValue == ZoomLevel.Small) return
        when(currentValue){
            ZoomLevel.Big -> currentValue = ZoomLevel.Medium
            ZoomLevel.Medium -> currentValue = ZoomLevel.Small
            else -> {}
        }
        updateState(currentValue)
    }

    private fun playAudio() {
        if (exoPlayer.playbackState == ExoPlayer.STATE_ENDED) {
            exoPlayer.seekTo(0)
        }
        exoPlayer.play()
        isPlaying = true
        startPositionTracker()
    }

    private fun startPositionTracker() {
        playbackJob?.cancel()
        playbackJob = viewModelScope.launch {
            while (isActive && isPlaying) {
                currentPlaybackMs = exoPlayer.currentPosition
                delay(16)
            }
        }
    }

    private fun updateState(zoomLevel: ZoomLevel){
        _zoomState.value = zoomLevel
    }

    override fun onCleared() {
        exoPlayer.release()
        super.onCleared()
    }
}