package com.example.audiocutterdemo2.permission

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

class PermissionCheckerImpl(private val context: Context) : PermissionChecker {
    override fun getRequiredAudioPermission(): String {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            android.Manifest.permission.READ_MEDIA_AUDIO
        } else {
            android.Manifest.permission.READ_EXTERNAL_STORAGE
        }
    }

    override fun hasAudioPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context, getRequiredAudioPermission()
        ) == PackageManager.PERMISSION_GRANTED
    }
}