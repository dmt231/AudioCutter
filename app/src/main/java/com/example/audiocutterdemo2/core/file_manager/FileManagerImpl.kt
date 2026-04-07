package com.example.audiocutterdemo.core.file_manager

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.database.ContentObserver
import android.net.Uri
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.os.ParcelFileDescriptor
import android.provider.MediaStore
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.example.audiocutterdemo2.core.file_manager.data.AudioFile
import com.example.audiocutterdemo.core.file_manager.data.AudioFileItem
import com.example.audiocutterdemo.core.file_manager.data.CacheAudioFile
import com.example.audiocutterdemo.core.file_manager.data.FileManagerState
import com.example.audiocutterdemo.core.ffmpeg.model.AudioFormat
import com.example.audiocutterdemo2.core.file_manager.AUDIO_EXTERNAL_URI
import com.example.audiocutterdemo2.core.file_manager.audioProjectionInfo
import com.example.audiocutterdemo2.core.file_manager.getAudioFileWithUri
import com.example.audiocutterdemo2.core.file_manager.getBitrateAndDuration
import com.example.audiocutterdemo2.core.file_manager.getDurationCacheAudioFile
import com.example.audiocutterdemo2.core.file_manager.getExtension
import com.example.audiocutterdemo2.core.file_manager.getRealUri
import com.example.audiocutterdemo2.core.file_manager.insertAudioMedia
import com.example.audiocutterdemo2.core.file_manager.queryAudioMediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.coroutines.resume

const val DEFAULT_EXTENSION_MP3 = ".mp3"

const val APP_FOLDER_NAME = "Audio Cutter"
const val CUT_FOLDER_NAME = "Cutter"
const val MERGE_FOLDER_NAME = "Merger"
const val MIX_FOLDER_NAME = "Mixer"
const val CONVERT_VIDEO_TO_AUDIO_FOLDER_NAME = "Convert To Audio"
const val AUDIO_EFFECT_FOLDER_NAME = "Audio Effect"
const val CHANGE_FORMAT_FOLDER_NAME = "Change Format"
const val REVERSE_FOLDER_NAME = "Reverse"
const val CHANGE_VOLUME_FOLDER_NAME = "Change Volume"
const val NOISE_REDUCE_FOLDER_NAME = "Noise Reduce"
const val RECORD_FOLDER_NAME = "Record"

const val AUDIO_MINE_TYPE = "audio/*"
const val VIDEO_MINE_TYPE = "video/*"

data class PickFile(
    val uri: String, val isError: Boolean = false
)

enum class TypeName(val prefix: String, val folderName: String) {
    CUTTER("cut_", CUT_FOLDER_NAME), MERGER("merge_", MERGE_FOLDER_NAME), MIXER(
        "mix_",
        MIX_FOLDER_NAME
    ),
    CONVERT_VIDEO_TO_AUDIO(
        "convert_",
        CONVERT_VIDEO_TO_AUDIO_FOLDER_NAME
    ),
    AUDIO_EFFECT("audio_effect_", AUDIO_EFFECT_FOLDER_NAME), CHANGE_FORMAT(
        "change_format_",
        CHANGE_FORMAT_FOLDER_NAME
    ),
    REVERSE("reverse_", REVERSE_FOLDER_NAME), CHANGE_VOLUME(
        "change_volume_",
        CHANGE_VOLUME_FOLDER_NAME
    ),
    NOISE_REDUCE("reduce_noise_", NOISE_REDUCE_FOLDER_NAME), RECORD("record_", RECORD_FOLDER_NAME)
}

val absAppFolderPath =
    Environment.getExternalStorageDirectory().absolutePath + File.separator + Environment.DIRECTORY_MUSIC + File.separator + APP_FOLDER_NAME + File.separator

class FileManagerImpl(private val context: Context) : IFileManager {

    private val mainScope = MainScope()

    private val queryAudioChannel = Channel<Unit>(Channel.CONFLATED)
    private val queryVideoChannel = Channel<Unit>(Channel.CONFLATED)

    override val audioItem: Flow<AudioFileItem> = flow {
        for (channel in queryAudioChannel) {
            emit(AudioFileItem(state = FileManagerState.Loading))

            val audioFiles = queryAudioMediaStore(
                context = context,
                uriExternal = AUDIO_EXTERNAL_URI,
                projection = audioProjectionInfo()
            )

            if (!audioFiles.isNullOrEmpty()) {
                emit(
                    AudioFileItem(
                        state = FileManagerState.Success,
                        list = audioFiles
                    )
                )
            } else if (audioFiles == null) {
                emit(AudioFileItem(state = FileManagerState.Failed, list = emptyList()))
            } else {
                emit(AudioFileItem(state = FileManagerState.Empty, list = emptyList()))
            }
        }
    }.onStart {
        context.registerAudioObserverMediaStore(audioOb)
        queryAudioChannel.trySend(Unit)
    }.onCompletion {
        context.unRegisterObserverMediaStore(audioOb)
    }.flowOn(Dispatchers.IO)
        .shareIn(scope = mainScope, started = SharingStarted.WhileSubscribed(), replay = 1)


    private val audioOb = object : ContentObserver(Handler(Looper.getMainLooper())) {
        override fun onChange(selfChange: Boolean, uri: Uri?) {
            queryAudioChannel.trySend(Unit)
            super.onChange(selfChange, uri)
        }
    }

    private val videoOb = object : ContentObserver(Handler(Looper.getMainLooper())) {
        override fun onChange(selfChange: Boolean, uri: Uri?) {
            queryVideoChannel.trySend(Unit)
            super.onChange(selfChange, uri)
        }
    }

    init {
        createNecessaryFolders()
    }

    override suspend fun getAudioFile(uriString: String): AudioFile? = withContext(Dispatchers.IO) {
        val item = context.getAudioFileWithUri(uri = uriString)

        val (bitRate, duration) = context.getBitrateAndDuration(uriString = uriString)
            ?: return@withContext item
        item?.copy(bitrate = bitRate, duration = duration)
    }

    override suspend fun reloadAudioFile() {
        queryAudioChannel.trySend(Unit)
    }


    override suspend fun getAudioBitrateAndDuration(uriString: String): Pair<Int, Long> =
        withContext(Dispatchers.IO) {
            return@withContext context.getBitrateAndDuration(
                uriString = uriString
            ) ?: (-1 to -1L)
        }

    override suspend fun insertAudioToMediaStore(cacheFile: CacheAudioFile): String {
        return context.insertAudioMedia(cacheFile)
    }

    override suspend fun pickFiles(
        activity: ComponentActivity, audioType: Boolean, isMultiple: Boolean
    ): List<PickFile> = suspendCancellableCoroutine { continuation ->

        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            putExtra(Intent.EXTRA_ALLOW_MULTIPLE, isMultiple)
            addCategory(Intent.CATEGORY_OPENABLE)

            type = if (audioType) AUDIO_MINE_TYPE else VIDEO_MINE_TYPE
        }

        val startForResult = activity.activityResultRegistry.register(
            System.currentTimeMillis().toString(), ActivityResultContracts.StartActivityForResult()
        ) { result: ActivityResult ->

            val files = ArrayList<PickFile>()
            val data = result.data

            Log.d("mtd", "pickFiles: data : $data")

            if (result.resultCode == Activity.RESULT_OK && data != null) {

                val clipData = data.clipData

                Log.d("mtd", "pickFiles: clipData : $clipData")

                if (clipData != null) {
                    repeat(clipData.itemCount) {
                        val uri = clipData.getItemAt(it).uri
                        val newUri = context.getRealUri(uri = uri, audioType = audioType)

                        if (newUri != null) {
                            files.add(
                                PickFile(
                                    uri = newUri.toString(),
                                    isError = if (audioType) context.getBitrateAndDuration(
                                        uriString = newUri.toString()
                                    ) == null else false
                                )
                            )
                        }
                    }
                } else {
                    val uri = data.data ?: return@register

                    val newUri = context.getRealUri(uri = uri, audioType = audioType)

                    if (newUri != null) {
                        files.add(
                            PickFile(
                                uri = newUri.toString(),
                                isError = if (audioType) context.getBitrateAndDuration(
                                    uriString = newUri.toString()
                                ) == null else false
                            )
                        )
                    }
                }
            }
            continuation.resume(files.toList())
        }

        startForResult.launch(intent)
    }

    override suspend fun deleteWithPath(paths: List<String>): Boolean = coroutineScope {
        return@coroutineScope withContext(Dispatchers.IO) {
            if (paths.isEmpty()) return@withContext true
            var result = true

            paths.forEach {
                if (!isActive) return@withContext false

                val isDelete =
                    com.example.audiocutterdemo2.core.file_manager.deleteWithPath(path = it)
                if (!isDelete) {
                    result = false
                }
            }

            return@withContext result
        }
    }

/*    override suspend fun deleteWithUri(activity: ComponentActivity, uris: List<String>): Boolean =
        withContext(Dispatchers.IO) {
            if (uris.isEmpty() || uris.all { it == "" }) return@withContext true

            var result = true

            uris.forEach {
                val uri = Uri.parse(it)
                val isDelete = activity.deleteUseMediaStore(uri)

                if (!isDelete) result = false
                else favoriteDao.delete(it)
            }

            return@withContext result
        }*/

 /*   override suspend fun rename(
        activity: ComponentActivity,
        path: String,
        newName: String
    ): String = coroutineScope {
        return@coroutineScope withContext(Dispatchers.IO) {
            try {
                val srcFile = File(path)
                val extension = path.getExtension()
                val newNameWithoutExtension = newName.substringBeforeLast(".")
                if (srcFile.exists()) {
                    val dir = srcFile.parentFile ?: return@withContext ""
                    if (dir.exists()) {
                        val to = File(dir, "$newNameWithoutExtension$extension")
                        if (to.exists()) return@withContext ""

                        val isRename = srcFile.renameTo(to)
                        if (isRename) {
                            activity.deleteAudioFromMedia(path)
                            return@withContext to.absolutePath
                        }

                        return@withContext ""
                    }
                }
                return@withContext ""
            } catch (e: Exception) {
                e.printStackTrace()
                return@withContext ""
            }
        }
    }*/

    override fun getRelativePath(folderName: String): String {
        return if (folderName.isEmpty()) {
            "Music/$APP_FOLDER_NAME/"
        } else {
            "Music/$APP_FOLDER_NAME/$folderName/"
        }
    }

    override fun autoGenerateFileName(
        type: TypeName, name: String, withoutMimeType: Boolean, format: AudioFormat?
    ): String {
        val extension = name.getExtension()
        val shortName = name.replace(extension, "")

        return if (withoutMimeType) {
            "${type.prefix}${shortName}_${System.currentTimeMillis()}"
        } else {
            val ext = format?.value ?: extension.ifEmpty { DEFAULT_EXTENSION_MP3 }
            "${type.prefix}${shortName}_${System.currentTimeMillis()}$ext"
        }
    }

    override fun autoGenerateOutputPath(
        context: Context,
        type: TypeName,
        fileName: String,
        format: AudioFormat?,
        isAutoGeneFileName: Boolean
    ): String {
        val outputDirectoryCache: File = context.filesDir

        val newFileName = if (isAutoGeneFileName) {
            autoGenerateFileName(type = type, name = fileName, format = format)
        } else {
            fileName
        }

        return "$outputDirectoryCache/$newFileName"
    }

    override suspend fun getCacheFile(type: TypeName, path: String): CacheAudioFile? {
        val file = File(path)

        return try {
            if (file.exists()) {
                CacheAudioFile(
                    type = type,
                    path = path,
                    name = file.name,
                    size = file.length(),
                    duration = context.getDurationCacheAudioFile(path = path),
                )
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    override fun isFileExist(uriString: String, isUri: Boolean): Boolean {
        if (uriString.isEmpty()) return false

        return try {
            if (isUri) {
                val fileDescriptor: ParcelFileDescriptor? =
                    context.contentResolver.openFileDescriptor(Uri.parse(uriString), "r")

                if (fileDescriptor != null) {
                    fileDescriptor.close()
                    true
                } else {
                    false
                }
            } else {
                File(uriString).exists()
            }
        } catch (e: Exception) {
            false
        }
    }

    private fun createNecessaryFolders() {
        if (createFolder(absAppFolderPath)) {
            createFolder("$absAppFolderPath$CUT_FOLDER_NAME")
            createFolder("$absAppFolderPath$MERGE_FOLDER_NAME")
            createFolder("$absAppFolderPath$MIX_FOLDER_NAME")
            createFolder("$absAppFolderPath$CONVERT_VIDEO_TO_AUDIO_FOLDER_NAME")
            createFolder("$absAppFolderPath$AUDIO_EFFECT_FOLDER_NAME")
            createFolder("$absAppFolderPath$CHANGE_FORMAT_FOLDER_NAME")
            createFolder("$absAppFolderPath$REVERSE_FOLDER_NAME")
            createFolder("$absAppFolderPath$CHANGE_VOLUME_FOLDER_NAME")
            createFolder("$absAppFolderPath$NOISE_REDUCE_FOLDER_NAME")
            createFolder("$absAppFolderPath$RECORD_FOLDER_NAME")
        }
    }

    private fun createFolder(folderPath: String): Boolean {
        val appFolder = File(folderPath)
        if (!appFolder.exists()) {
            return appFolder.mkdirs()
        }

        return if (appFolder.isDirectory) {
            true
        } else {
            appFolder.mkdirs()
        }
    }

    private fun Context.unRegisterObserverMediaStore(contentOb: ContentObserver) {
        try {
            contentResolver.unregisterContentObserver(contentOb)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun Context.registerAudioObserverMediaStore(
        audioOb: ContentObserver,
    ) {
        contentResolver.registerContentObserver(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, true, audioOb
        )
    }

    private fun Context.registerVideoObserverMediaStore(
        videoOb: ContentObserver,
    ) {
        contentResolver.registerContentObserver(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI, true, videoOb
        )
    }
}