package com.example.audiocutterdemo2.core.file_manager

import android.content.ContentResolver
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.storage.StorageManager
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.util.Log
import android.webkit.MimeTypeMap
import androidx.annotation.RequiresApi
import com.arthenica.ffmpegkit.FFmpegKitConfig
import com.arthenica.ffmpegkit.FFprobeKit
import com.example.audiocutterdemo.core.file_manager.APP_FOLDER_NAME
import com.example.audiocutterdemo.core.file_manager.TypeName
import com.example.audiocutterdemo.core.file_manager.absAppFolderPath
import com.example.audiocutterdemo2.core.file_manager.data.AudioFile
import com.example.audiocutterdemo.core.file_manager.data.CacheAudioFile

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

val AUDIO_EXTERNAL_URI: Uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
val VIDEO_EXTERNAL_URI: Uri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI
val FILE_EXTERNAL_URI: Uri = MediaStore.Files.getContentUri("external")

private const val MEDIA_STORE_DATA: String = MediaStore.MediaColumns.DATA

@RequiresApi(Build.VERSION_CODES.Q)
private const val AUDIO_COLUMN_RELATIVE: String = MediaStore.Audio.Media.RELATIVE_PATH
private const val AUDIO_COLUMN_DATA: String = MediaStore.Audio.Media.DATA

@RequiresApi(Build.VERSION_CODES.R)
private const val AUDIO_COLUMN_BITRATE: String = MediaStore.Audio.Media.BITRATE
private const val AUDIO_COLUMN_ID: String = MediaStore.Audio.Media._ID
private const val AUDIO_COLUMN_DISPLAY_NAME: String = MediaStore.Audio.Media.DISPLAY_NAME
private const val AUDIO_COLUMN_MIME_TYPE: String = MediaStore.Audio.Media.MIME_TYPE
private const val AUDIO_COLUMN_DURATION: String = MediaStore.Audio.Media.DURATION
private const val AUDIO_COLUMN_DATE_MODIFIED: String = MediaStore.Audio.Media.DATE_MODIFIED
private const val AUDIO_COLUMN_SIZE: String = MediaStore.Audio.Media.SIZE

@RequiresApi(Build.VERSION_CODES.Q)
private const val VIDEO_COLUMN_RELATIVE: String = MediaStore.Video.Media.RELATIVE_PATH
private const val VIDEO_COLUMN_DATA: String = MediaStore.Video.Media.DATA

@RequiresApi(Build.VERSION_CODES.R)
private const val VIDEO_COLUMN_BITRATE: String = MediaStore.Video.Media.BITRATE
private const val VIDEO_COLUMN_ID: String = MediaStore.Video.Media._ID
private const val VIDEO_COLUMN_DISPLAY_NAME: String = MediaStore.Video.Media.DISPLAY_NAME
private const val VIDEO_COLUMN_MIME_TYPE: String = MediaStore.Video.Media.MIME_TYPE
private const val VIDEO_COLUMN_DURATION: String = MediaStore.Video.Media.DURATION
private const val VIDEO_COLUMN_DATE_MODIFIED: String = MediaStore.Video.Media.DATE_MODIFIED
private const val VIDEO_COLUMN_SIZE: String = MediaStore.Video.Media.SIZE

private val cmdInfoAudio =
    "-show_streams -show_entries format=:stream=sample_rate -of json -v quiet -i \'%s\'"

suspend fun queryAudioMediaStore(
    context: Context, projection: Array<String>, uriExternal: Uri
): List<AudioFile>? = withContext(Dispatchers.IO) {
    val rootPath = Environment.getExternalStorageDirectory().path

    val audioFiles = mutableListOf<AudioFile>()

    val cursor = context.contentResolver.query(
        uriExternal, projection, null, null, null
    )

    try {
        cursor?.use {
            while (it.moveToNext() && isActive) {
                val audioFile = parseAudioFileInfo(rootPath = rootPath, cursor = it)

                if (audioFile != null && audioFile.duration > 0L) {
                    audioFiles.add(audioFile)
                }
            }
        }

        audioFiles
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

fun audioProjectionInfo(): Array<String> {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        arrayOf(
            AUDIO_COLUMN_ID,
            AUDIO_COLUMN_DISPLAY_NAME,
            AUDIO_COLUMN_MIME_TYPE,
            AUDIO_COLUMN_DURATION,
            AUDIO_COLUMN_DATE_MODIFIED,
            AUDIO_COLUMN_SIZE,
            AUDIO_COLUMN_BITRATE,
            AUDIO_COLUMN_RELATIVE
        )
    } else {
        arrayOf(
            VIDEO_COLUMN_ID,
            VIDEO_COLUMN_DISPLAY_NAME,
            VIDEO_COLUMN_MIME_TYPE,
            VIDEO_COLUMN_DURATION,
            VIDEO_COLUMN_DATE_MODIFIED,
            VIDEO_COLUMN_SIZE,
            MEDIA_STORE_DATA,
        )
    }
}

fun videoProjectionInfo(): Array<String> {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        arrayOf(
            VIDEO_COLUMN_ID,
            VIDEO_COLUMN_DISPLAY_NAME,
            VIDEO_COLUMN_MIME_TYPE,
            VIDEO_COLUMN_DURATION,
            VIDEO_COLUMN_DATE_MODIFIED,
            VIDEO_COLUMN_SIZE,
            VIDEO_COLUMN_BITRATE,
            VIDEO_COLUMN_RELATIVE
        )
    } else {
        arrayOf(
            VIDEO_COLUMN_ID,
            VIDEO_COLUMN_DISPLAY_NAME,
            VIDEO_COLUMN_MIME_TYPE,
            VIDEO_COLUMN_DURATION,
            VIDEO_COLUMN_DATE_MODIFIED,
            VIDEO_COLUMN_SIZE,
            MEDIA_STORE_DATA
        )
    }
}

fun deleteWithPath(path: String): Boolean {
    try {
        val file = File(path)
        if (file.exists()) {
            return file.delete()
        }
        return false
    } catch (e: Exception) {
        e.printStackTrace()
        return false
    }
}

suspend fun Context.getAudioUri(uri: String): String? = withContext(Dispatchers.IO) {
    return@withContext try {
        val cursor = getAudioCursor(uri = uri)

        cursor?.use {
            if (cursor.moveToFirst() && isActive) {
                parseAudioUriInfo(cursor = cursor)
            } else {
                null
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

suspend fun Context.getAudioFileWithUri(uri: String): AudioFile? = withContext(Dispatchers.IO) {
    return@withContext try {
        val rootPath = Environment.getExternalStorageDirectory().path

        val cursor = getAudioCursor(uri = uri)

        cursor?.use {
            if (cursor.moveToFirst() && isActive) {
                parseAudioFileInfo(rootPath = rootPath, cursor = cursor)
            } else {
                null
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}


//suspend fun Activity.deleteAudioFromMedia(path: String): Boolean = withContext(Dispatchers.IO) {
//    val uriFile = getUriFromPath(path) ?: return@withContext false
//    return@withContext deleteUseMediaStore(uriFile)
//}

fun Context.getUriFromPath(path: String?): Uri? {
    path ?: return null

    val resolver = contentResolver
    val query = resolver.query(
        FILE_EXTERNAL_URI,
        arrayOf(MediaStore.Files.FileColumns._ID),
        MediaStore.MediaColumns.DATA + " = ?",
        arrayOf(path),
        null
    )

    try {
        query?.use { cursor ->
            val idColumns = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID)
            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumns)
                return ContentUris.withAppendedId(
                    FILE_EXTERNAL_URI, id
                )
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
    } finally {
        query?.close()
    }
    return null
}

fun String.getMimeType(): String {
    val mime = MimeTypeMap.getSingleton()
    val extension = getExtension()
    return mime.getMimeTypeFromExtension(extension.replace(".", "")) ?: ""
}

fun String.getExtension(): String {
    if (this.isEmpty())
        return ""

    var extension = ""

    val lastIndexOf = lastIndexOf(".")
    if (lastIndexOf != -1) {
        extension = substring(lastIndexOf)
    }

    return extension
}

fun Context.getSampleRate(uriString: String): Float? {

    return try {
        val path = FFmpegKitConfig.getSafParameterForRead(
            this, Uri.parse(uriString)
        )

        getSampleRateFromMediaMetadata(uriString) ?: getSampleRateFromFFMpeg(path)
    } catch (e: Exception) {
        null
    }
}


fun Context.getBitrateAndDuration(
    uriString: String,
): Pair<Int, Long>? {
    val metadataInfo = getAudioInfoFromMediaMetadata(uriString = uriString)

    var bitrate = metadataInfo.first ?: 0
    var duration = metadataInfo.second ?: 0L

    if (bitrate <= 0 || duration <= 0L) {
        val ffmpegInfo = getAudioInfoFromFFMpeg(
            uriString = uriString, isUri = true
        )

        if (bitrate <= 0) bitrate = ffmpegInfo.first ?: 0

        if (duration <= 0L) duration = ffmpegInfo.second ?: 0L
    }

    return if (duration <= 0L) null
    else bitrate to duration
}

fun Context.getDurationCacheAudioFile(path: String): Long {
    var duration = getDurationMediaMetadata(path) ?: 0L

    if (duration <= 0L) {
        val ffmpegInfo = getAudioInfoFromFFMpeg(uriString = path, isUri = false)

        duration = ffmpegInfo.second ?: 0L
    }

    return duration
}

fun getDurationMediaMetadata(path: String): Long? {
    var duration: Long? = null

    return try {
        val mmr = MediaMetadataRetriever()

        mmr.setDataSource(path)
        duration = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull()

        mmr.release()

        duration
    } catch (e: Exception) {
        duration
    }
}

suspend fun Context.insertAudioMedia(audio: CacheAudioFile): String = withContext(Dispatchers.IO) {
    val valuesAudio = ContentValues()
    valuesAudio.updateContentValue(audio)

    return@withContext if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {

        val contentUri = getAudioContentUri()
        val relativePath = audio.path.toRelativePath(audio.type, "Music")
        valuesAudio.updateRelativePath(relativePath)

        writeToStorage(
            contentUri = contentUri,
            value = valuesAudio,
            path = audio.path
        )?.toString() ?: ""
    } else {
        val path = writeFile(
            inputPath = audio.path,
            outputPath = "${absAppFolderPath}${audio.type.folderName}/${audio.name}"
        ) ?: return@withContext ""

        valuesAudio.put(MediaStore.MediaColumns.DATA, path)
        contentResolver.insert(AUDIO_EXTERNAL_URI, valuesAudio)?.toString() ?: ""
    }
}

//suspend fun Activity.deleteUseMediaStore(uri: Uri): Boolean {
//    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
//        requestDeletePermission(uri)
//    } else
//        deleteContent(uri)
//}

private fun Context.getAudioCursor(uri: String): Cursor? {
    val projection = audioProjectionInfo()

    val selection = "${AUDIO_COLUMN_ID}=?"
    val selectionArgs = arrayOf(ContentUris.parseId(Uri.parse(uri)).toString())

    return contentResolver.query(
        AUDIO_EXTERNAL_URI, projection, selection, selectionArgs, null
    )
}


fun Context.deleteContent(uri: Uri): Boolean {
    return try {
        contentResolver.delete(uri, null, null)
        true
    } catch (e: Exception) {
        e.printStackTrace()
        false
    }
}

fun Context.getRealUri(uri: Uri, audioType: Boolean): Uri? {
    return try {
        val docId = DocumentsContract.getDocumentId(uri)
        val split = docId.split(":").toTypedArray()

        Log.d("MTHAI", "getRealUri: $uri")

        if (isExternalStorageDocument(uri)) {
            val isSdCard = getStoragePath(this, true).contains(split[0])

            getUriFromPath(
                path = "${
                    getStoragePath(
                        mContext = this, isExternal = isSdCard
                    )
                }/${split[1]}"
            )
        } else if (isMediaDocument(uri)) {
            val contentUri = getUriExternal(split[0])
            val selection = MediaStore.Files.FileColumns._ID + "=?"
            val selectionArgs = arrayOf(split[1])

            getMediaUri(
                contentUri = contentUri,
                selection = selection,
                selectionArgs = selectionArgs,
                audioType = audioType
            )
        } else if (isDownloadsDocument(uri)) {
            if (split[0] == "raw") {
                return getUriFromPath(path = split[1])
            } else {
                contentResolver.query(
                    uri, arrayOf(MediaStore.Files.FileColumns.DISPLAY_NAME), null, null, null
                )?.use {
                    if (it.moveToFirst()) {
                        val fileName = it.getString(0)
                        return getUriFromPath(
                            path = "${
                                Environment.getExternalStoragePublicDirectory(
                                    Environment.DIRECTORY_DOWNLOADS
                                ).absolutePath
                            }/$fileName"
                        )
                    }
                }
                null
            }
        } else {
            null
        }
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

private fun Context.getMediaUri(
    audioType: Boolean, contentUri: Uri?, selection: String?, selectionArgs: Array<String>?
): Uri? {
    contentUri ?: return null

    var cursor: Cursor? = null

    val uriExternal = if (audioType) AUDIO_EXTERNAL_URI else VIDEO_EXTERNAL_URI
    val column = if (audioType) AUDIO_COLUMN_ID else VIDEO_COLUMN_ID
    val projection = arrayOf(column)

    try {
        cursor = contentResolver.query(
            contentUri, projection, selection, selectionArgs, null
        )

        Log.d("MTHAI", "getMediaUri: $contentUri")

        if (cursor != null && cursor.moveToFirst()) {
            val id = cursor.getLong(cursor.getColumnIndexOrThrow(AUDIO_COLUMN_ID))
            return ContentUris.withAppendedId(uriExternal, id)
        }
    } catch (e: Exception) {
        e.printStackTrace()
    } finally {
        cursor?.close()
    }

    return null
}

private fun getUriExternal(type: String): Uri {
    return when (type) {
        "video" -> {
            VIDEO_EXTERNAL_URI
        }

        "audio" -> {
            AUDIO_EXTERNAL_URI
        }

        else -> {
            FILE_EXTERNAL_URI
        }
    }
}

private fun getStoragePath(mContext: Context, isExternal: Boolean): String {
    var path = ""
    val mStorageManager = mContext.getSystemService(Context.STORAGE_SERVICE) as StorageManager
    try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            mStorageManager.storageVolumes.forEach {
                if (it.isRemovable == isExternal) {
                    path = it.directory?.absolutePath ?: ""
                }
            }
        } else {
            val storageVolumeClazz = Class.forName("android.os.storage.StorageVolume")
            val getVolumeList = mStorageManager.javaClass.getMethod("getVolumeList")
            val getPath = storageVolumeClazz.getMethod("getPath")
            val isRemovable = storageVolumeClazz.getMethod("isRemovable")
            val result = getVolumeList.invoke(mStorageManager)
            result?.let {
                val length: Int = java.lang.reflect.Array.getLength(result)
                for (i in 0 until length) {
                    val storageVolumeElement: Any = java.lang.reflect.Array.get(result, i)!!
                    val paths = getPath.invoke(storageVolumeElement) as String
                    val removable = isRemovable.invoke(storageVolumeElement) as Boolean
                    if (removable == isExternal) {
                        path = paths
                    }
                }
            }
        }

        if (path.isEmpty() && !isExternal) {
            path = Environment.getExternalStorageDirectory().absolutePath
        }
    } catch (e: java.lang.Exception) {
        e.printStackTrace()
    }
    return path
}

private fun isExternalStorageDocument(uri: Uri): Boolean {
    return "com.android.externalstorage.documents" == uri.authority
}

private fun isMediaDocument(uri: Uri): Boolean {
    return "com.android.providers.media.documents" == uri.authority
}

private fun isDownloadsDocument(uri: Uri): Boolean {
    return "com.android.providers.downloads.documents" == uri.authority
}

private fun parseAudioUriInfo(cursor: Cursor): String? {
    return try {
        val id = cursor.getLong(cursor.getColumnIndexOrThrow(AUDIO_COLUMN_ID))
        ContentUris.withAppendedId(AUDIO_EXTERNAL_URI, id).toString()
    } catch (e: Exception) {
        Log.d("MTHAI", "parseAudioUriInfo Exception: $e")
        null
    }
}

private fun parseAudioFileInfo(rootPath: String, cursor: Cursor): AudioFile? {
    return try {
        val id = cursor.getLong(cursor.getColumnIndexOrThrow(AUDIO_COLUMN_ID))
        val uri = ContentUris.withAppendedId(AUDIO_EXTERNAL_URI, id)
        val displayName = cursor.getString(cursor.getColumnIndexOrThrow(AUDIO_COLUMN_DISPLAY_NAME))
        val mimeType = cursor.getString(cursor.getColumnIndexOrThrow(AUDIO_COLUMN_MIME_TYPE))
        val duration = cursor.getLong(cursor.getColumnIndexOrThrow(AUDIO_COLUMN_DURATION))
        val dateModified = cursor.getLong(cursor.getColumnIndexOrThrow(AUDIO_COLUMN_DATE_MODIFIED))
        val size = cursor.getLong(cursor.getColumnIndexOrThrow(AUDIO_COLUMN_SIZE))

        var relativePath = ""

        val parentName = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val hasRelativePathColumn = cursor.getColumnIndex(AUDIO_COLUMN_RELATIVE) != -1

            relativePath = if (hasRelativePathColumn)
                cursor.getString(cursor.getColumnIndexOrThrow(AUDIO_COLUMN_RELATIVE))
            else
                cursor.getString(cursor.getColumnIndexOrThrow(AUDIO_COLUMN_DATA))

            File(relativePath).name ?: ""
        } else {
            val absolutePath = cursor.getString(cursor.getColumnIndexOrThrow(MEDIA_STORE_DATA))

            relativePath = absolutePath.substring(rootPath.length)

            File(absolutePath).parentFile?.name ?: ""
        }

        AudioFile(
            id = id,
            uriString = uri.toString(),
            parentName = parentName,
            relativePath = relativePath,
            name = displayName,
            size = size,
            mimeType = mimeType,
            bitrate = 0,
            duration = duration,
            lastModified = dateModified * 1000,
        )
    } catch (e: Exception) {
        Log.d("MTHAI", "parseAudioFileInfo Exception: $e")
        null
    }
}


private fun Context.getSampleRateFromMediaMetadata(path: String): Float? {
    var sampleRate: Float? = null

    return try {
        val mmr = MediaMetadataRetriever()
        mmr.setDataSource(this, Uri.parse(path))

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            sampleRate =
                mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_SAMPLERATE)?.toFloatOrNull()

            mmr.release()

            sampleRate
        } else {
            getSampleRateFromFFMpeg(path)
        }
    } catch (e: NumberFormatException) {
        sampleRate
    }
}

private fun getSampleRateFromFFMpeg(path: String): Float? {
    val cmdFFProbe = String.format(cmdInfoAudio, path)
    val ffProbeSession = FFprobeKit.execute(cmdFFProbe)
    val jsonObj = JSONObject(ffProbeSession.output)
    val streams = jsonObj.optJSONArray("streams")

    return streams?.optJSONObject(0)?.optString("sample_rate")?.toFloatOrNull()
}

private fun Context.getAudioInfoFromFFMpeg(
    uriString: String,
    isUri: Boolean,
): Pair<Int?, Long?> {
    val mediaInformation = FFprobeKit.executeWithArguments(
        defaultGetMediaInformationCommandArguments(
            isUri = isUri, uriStringOrPath = uriString
        )
    )

    return mediaInformation.output.getDurationAndBitrate()
}

private fun Context.defaultGetMediaInformationCommandArguments(
    isUri: Boolean, uriStringOrPath: String
): Array<String> {
    val path = if (isUri) FFmpegKitConfig.getSafParameterForRead(
        this, Uri.parse(uriStringOrPath)
    ) else uriStringOrPath

    return arrayOf(
        "-v",
        "error",
        "-hide_banner",
        "-print_format",
        "json",
        "-show_streams",
        "-show_format",
        "-i",
        path
    )
}

private fun String.getDurationAndBitrate(): Pair<Int?, Long?> {
    var bitrate: Int? = null
    var duration: Long? = null

    return try {
        val jsonObj = JSONObject(this)
        val dataObj = jsonObj.optJSONObject("format")
        bitrate = dataObj?.optInt("bit_rate")
        val strDuration = dataObj?.optDouble("duration")
        duration = ((strDuration ?: 0.0) * 1000L).toLong()

        bitrate to duration
    } catch (ex: Exception) {
        bitrate to duration
    }
}

private fun Context.getAudioInfoFromMediaMetadata(
    uriString: String?,
): Pair<Int?, Long?> {

    var bitrate: Int? = null
    var duration: Long? = null

    return try {
        val mmr = MediaMetadataRetriever()

        mmr.setDataSource(this, Uri.parse(uriString))
        bitrate = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE)?.toIntOrNull()
        duration = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull()

        mmr.release()

        bitrate to duration
    } catch (e: Exception) {
        bitrate to duration
    }
}

private fun ContentValues.updateContentValue(audio: CacheAudioFile) {
    val mimeType = audio.path.getMimeType()
    put(MediaStore.MediaColumns.DISPLAY_NAME, audio.name)
    put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
    put(MediaStore.MediaColumns.DURATION, audio.duration * 1000)
    put(MediaStore.MediaColumns.SIZE, audio.size)
    put(MediaStore.MediaColumns.DATE_MODIFIED, System.currentTimeMillis())
}

@RequiresApi(Build.VERSION_CODES.Q)
private fun String.toRelativePath(type: TypeName, folder: String): String {
    val parent = File(this).parentFile
    val folderName = parent?.name

    return if (folderName != null) "Music/${APP_FOLDER_NAME}/${type.folderName}"
    else folder
}

@RequiresApi(Build.VERSION_CODES.Q)
private fun getAudioContentUri(): Uri {
    return MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
}

@RequiresApi(Build.VERSION_CODES.Q)
private fun Context.writeToStorage(
    contentUri: Uri, value: ContentValues, path: String
): Uri? {
    val savedUri = try {
        contentResolver.insert(contentUri, value) ?: return null
    } catch (e: Exception) {
        e.printStackTrace()
        return null
    }

    val success = contentResolver.writeFile(savedUri, path)

    try {
        contentResolver.update(
            savedUri,
            ContentValues().apply { put(MediaStore.MediaColumns.IS_PENDING, 0) },
            null,
            null
        )
    } catch (e: Exception) {
        e.printStackTrace()
    }

    return if (success) {
        savedUri
    } else null
}

private fun writeFile(inputPath: String, outputPath: String): String? {
    return try {
        val outputStream = FileOutputStream(File(outputPath))
        val inputStream = FileInputStream(File(inputPath))

        inputStream.copyTo(outputStream)

        inputStream.close()
        outputStream.close()

        deleteWithPath(inputPath)

        outputPath
    } catch (e: Exception) {
        null
    }
}

private fun ContentResolver.writeFile(uri: Uri, path: String): Boolean {
    try {
        val pfd = openFileDescriptor(uri, "w") ?: return false

        val outputStream = FileOutputStream(pfd.fileDescriptor)
        val inputStream = FileInputStream(path)

        inputStream.copyTo(outputStream)

        pfd.close()
        inputStream.close()
        outputStream.close()

        deleteWithPath(path)

        return true
    } catch (e: Exception) {
        return false
    }
}

@RequiresApi(Build.VERSION_CODES.Q)
private fun ContentValues.updateRelativePath(path: String) {
    put(MediaStore.MediaColumns.RELATIVE_PATH, path)
    put(MediaStore.MediaColumns.DATE_ADDED, System.currentTimeMillis() / 1000)
    put(MediaStore.MediaColumns.DATE_TAKEN, System.currentTimeMillis())
    put(MediaStore.MediaColumns.IS_PENDING, 1)
}