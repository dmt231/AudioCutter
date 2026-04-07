package com.example.audiocutterdemo.core.file_manager

import android.content.Context
import androidx.activity.ComponentActivity
import com.example.audiocutterdemo2.core.file_manager.data.AudioFile
import com.example.audiocutterdemo.core.file_manager.data.AudioFileItem
import com.example.audiocutterdemo.core.file_manager.data.CacheAudioFile
import com.example.audiocutterdemo.core.ffmpeg.model.AudioFormat
import kotlinx.coroutines.flow.Flow

interface IFileManager {

    val audioItem: Flow<AudioFileItem>

    suspend fun getAudioFile(uriString: String): AudioFile?

    suspend fun getAudioBitrateAndDuration(uriString: String): Pair<Int, Long>

    suspend fun insertAudioToMediaStore(cacheFile: CacheAudioFile): String

    suspend fun reloadAudioFile()

    suspend fun pickFiles(
        activity: ComponentActivity,
        audioType: Boolean = true,
        isMultiple: Boolean = true
    ): List<PickFile>

    suspend fun deleteWithPath(paths: List<String>): Boolean

//    suspend fun deleteWithUri(activity: ComponentActivity, uris: List<String>): Boolean

//    suspend fun rename(activity: ComponentActivity, path: String, newName: String): String

    fun getRelativePath(folderName: String): String

    fun autoGenerateFileName(
        type: TypeName,
        name: String,
        withoutMimeType: Boolean = false,
        format: AudioFormat? = null
    ): String

    fun autoGenerateOutputPath(
        context: Context,
        type: TypeName,
        fileName: String,
        format: AudioFormat? = null,
        isAutoGeneFileName: Boolean = true
    ): String

    suspend fun getCacheFile(type: TypeName, path: String): CacheAudioFile?

    fun isFileExist(uriString: String, isUri: Boolean = true): Boolean
}