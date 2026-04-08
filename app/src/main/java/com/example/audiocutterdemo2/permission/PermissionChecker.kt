package com.example.audiocutterdemo2.permission

interface PermissionChecker {
    fun hasAudioPermission(): Boolean
    fun getRequiredAudioPermission(): String
}