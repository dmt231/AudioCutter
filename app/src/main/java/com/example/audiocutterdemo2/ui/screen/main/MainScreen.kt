package com.example.audiocutterdemo2.ui.screen.main

import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.audiocutterdemo2.R
import com.example.audiocutterdemo2.core.file_manager.data.TrimMode
import com.example.audiocutterdemo2.ui.custom.WaveformRangeSelector
import org.koin.androidx.compose.koinViewModel

@Composable
fun MainScreen(viewModel: MainViewModel = koinViewModel()) {
    val context = LocalContext.current
    val activity = context as? ComponentActivity
    val lifecycleOwner = LocalLifecycleOwner.current
    val selectedFile by viewModel.selectedFile.collectAsStateWithLifecycle()
    val amplitudes by viewModel.amplitudes.collectAsStateWithLifecycle()
    val zoomLevel by viewModel.zoomState.collectAsStateWithLifecycle()
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        viewModel.onPermissionResult(isGranted)
    }

    LaunchedEffect(Unit) {
        viewModel.permissionRequestEvent.collect { permission ->
            permissionLauncher.launch(permission)
        }
    }

    LaunchedEffect(viewModel.navigationEvent, lifecycleOwner) {
        viewModel.navigationEvent.flowWithLifecycle(
            lifecycleOwner.lifecycle, Lifecycle.State.STARTED
        ).collect { shouldPick ->
            if (shouldPick && activity != null) {
                viewModel.pickFiles(activity)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (selectedFile == null) {
            Button(onClick = { viewModel.onPickFileClick() }) {
                Text("Pick Audio")
            }
        } else {
            Text(
                text = selectedFile!!.name,
                style = MaterialTheme.typography.titleMedium,
                color = Color.White
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = viewModel.trimMode == TrimMode.TRIM_SIDE,
                    onClick = { viewModel.trimMode = TrimMode.TRIM_SIDE },
                    label = { Text("Trim side") })
                FilterChip(
                    selected = viewModel.trimMode == TrimMode.TRIM_MIDDLE,
                    onClick = { viewModel.trimMode = TrimMode.TRIM_MIDDLE },
                    label = { Text("Trim middle") })
            }

            Spacer(Modifier.height(24.dp))

            if (viewModel.isDecoding) {
                CircularProgressIndicator()
            } else if (amplitudes.isNotEmpty()) {
                WaveformRangeSelector(
                    amplitudes = amplitudes,
                    totalDurationMs = selectedFile!!.duration,
                    handleLMs = viewModel.handleLMs,
                    handleRMs = viewModel.handleRMs,
                    trimMode = viewModel.trimMode,
                    zoomLevel = zoomLevel,
                    currentPlaybackMs = viewModel.currentPlaybackMs,
                    onHandleLChange = { viewModel.handleLMs = it },
                    onHandleRChange = { viewModel.handleRMs = it },
                    onSeek = { viewModel.seekTo(it) }
                )
            }

            Spacer(Modifier.height(16.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(24.dp)
            ) {

                Image(
                    modifier = Modifier
                        .size(24.dp)
                        .clickable {
                            viewModel.zoomOut()
                        },
                    painter = painterResource(id = R.drawable.ic_zoom_out),
                    contentDescription = null
                )

                Image(
                    modifier = Modifier.clickable {
                        viewModel.togglePlayPause()
                    },
                    painter = painterResource(if (viewModel.isPlaying) R.drawable.ic_pause else R.drawable.ic_play),
                    contentDescription = null
                )

                Image(
                    modifier = Modifier
                        .size(24.dp)
                        .clickable {
                            viewModel.zoomIn()
                        },
                    painter = painterResource(id = R.drawable.ic_zoom_in),
                    contentDescription = null
                )

            }

            Spacer(Modifier.height(16.dp))

            Button(onClick = {
                //TODO
                viewModel.confirmCut()
            }) {
                Text("Confirm")
            }
        }
    }
}

