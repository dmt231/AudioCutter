package com.example.audiocutterdemo2.ui.main

import android.util.Log
import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.flowWithLifecycle
import com.example.audiocutterdemo2.core.file_manager.data.TrimMode
import com.example.audiocutterdemo2.ui.screen.WaveformRangeSelector
import com.example.audiocutterdemo2.ui.theme.Purple40
import org.koin.androidx.compose.koinViewModel

@Composable
fun MainScreen(viewModel: MainViewModel = koinViewModel()) {
    val context = LocalContext.current
    val activity = context as? ComponentActivity
    val lifecycleOwner = LocalLifecycleOwner.current
    val selectedFile by viewModel.selectedFile.collectAsStateWithLifecycle()
    val amplitudes by viewModel.amplitudes.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel.navigationEvent, lifecycleOwner) {
        viewModel.navigationEvent.flowWithLifecycle(
            lifecycleOwner.lifecycle,
            Lifecycle.State.STARTED
        )
            .collect { event ->
                activity?.let {
                    viewModel.pickFiles(it)
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
                style = MaterialTheme.typography.titleMedium
            )

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
                    onHandleLChange = { viewModel.handleLMs = it },
                    onHandleRChange = { viewModel.handleRMs = it }
                )
            }

            Spacer(Modifier.height(16.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = viewModel.trimMode == TrimMode.TRIM_SIDE,
                    onClick = { viewModel.trimMode = TrimMode.TRIM_SIDE },
                    label = { Text("Trim side") }
                )
                FilterChip(
                    selected = viewModel.trimMode == TrimMode.TRIM_MIDDLE,
                    onClick = { viewModel.trimMode = TrimMode.TRIM_MIDDLE },
                    label = { Text("Trim middle") }
                )
            }

            Spacer(Modifier.height(16.dp))

            Button(onClick = {
                //TODO
                Log.d("mtd", "MainScreen: leftStart : ${viewModel.handleLMs} endTime : ${viewModel.handleRMs}")
            }) {
                Text("Confirm")
            }
        }
    }
}