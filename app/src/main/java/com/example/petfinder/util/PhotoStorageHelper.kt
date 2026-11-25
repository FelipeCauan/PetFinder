package com.example.petfinder.util

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

object PhotoStorageHelper {

    const val PET_PHOTOS_DIR = "pet_photos"
    const val USER_PHOTOS_DIR = "user_photos"
    private const val CAMERA_CACHE_DIR = "camera_temp"

    fun createTempCameraUri(context: Context): Pair<Uri, File> {
        val tempDir = File(context.cacheDir, CAMERA_CACHE_DIR).apply { mkdirs() }
        val tempFile = File(tempDir, "cam_${UUID.randomUUID()}.jpg")
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", tempFile)
        return uri to tempFile
    }

    suspend fun copyUriToInternalStorage(
        context: Context,
        sourceUri: Uri,
        dirName: String = PET_PHOTOS_DIR,
        filePrefix: String = "pet"
    ): String? = withContext(Dispatchers.IO) {
        runCatching {
            val destFile = newInternalPhotoFile(context, dirName, filePrefix)
            context.contentResolver.openInputStream(sourceUri)?.use { input ->
                FileOutputStream(destFile).use { output -> input.copyTo(output) }
            } ?: return@withContext null
            destFile.absolutePath
        }.getOrNull()
    }

    suspend fun moveFileToInternalStorage(
        context: Context,
        tempFile: File,
        dirName: String = PET_PHOTOS_DIR,
        filePrefix: String = "pet"
    ): String? = withContext(Dispatchers.IO) {
        if (!tempFile.exists() || tempFile.length() == 0L) return@withContext null
        runCatching {
            val destFile = newInternalPhotoFile(context, dirName, filePrefix)
            tempFile.copyTo(destFile, overwrite = true)
            tempFile.delete()
            destFile.absolutePath
        }.getOrNull()
    }

    suspend fun deleteLocalPhotos(context: Context, paths: List<String?>) {
        withContext(Dispatchers.IO) {
            val baseDir = runCatching { context.filesDir.canonicalPath }.getOrNull() ?: return@withContext
            paths.filterNotNull()
                .filter { it.isNotBlank() }
                .forEach { path ->
                    runCatching {
                        val file = File(path)
                        if (file.canonicalPath.startsWith(baseDir) && file.exists()) file.delete()
                    }
                }
        }
    }

    private fun newInternalPhotoFile(context: Context, dirName: String, prefix: String): File {
        val dir = File(context.filesDir, dirName).apply { mkdirs() }
        return File(dir, "${prefix}_${UUID.randomUUID()}.jpg")
    }
}
