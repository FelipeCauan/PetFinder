package com.example.petfinder.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
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

    suspend fun loadImageBitmap(path: String, maxDimensionPx: Int = 720): ImageBitmap? =
        withContext(Dispatchers.IO) {
            if (path.isBlank() || !File(path).exists()) return@withContext null
            runCatching {
                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeFile(path, bounds)
                var sampleSize = 1
                while (bounds.outWidth / sampleSize > maxDimensionPx ||
                    bounds.outHeight / sampleSize > maxDimensionPx
                ) {
                    sampleSize *= 2
                }
                val bitmap = BitmapFactory.decodeFile(
                    path,
                    BitmapFactory.Options().apply { inSampleSize = sampleSize }
                ) ?: return@withContext null
                rotateIfNeeded(path, bitmap).asImageBitmap()
            }.getOrNull()
        }

    private fun newInternalPhotoFile(context: Context, dirName: String, prefix: String): File {
        val dir = File(context.filesDir, dirName).apply { mkdirs() }
        return File(dir, "${prefix}_${UUID.randomUUID()}.jpg")
    }

    private fun rotateIfNeeded(path: String, bitmap: Bitmap): Bitmap {
        val orientation = runCatching {
            ExifInterface(path).getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL
            )
        }.getOrDefault(ExifInterface.ORIENTATION_NORMAL)

        val degrees = when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90f
            ExifInterface.ORIENTATION_ROTATE_180 -> 180f
            ExifInterface.ORIENTATION_ROTATE_270 -> 270f
            else -> return bitmap
        }
        val rotated = Bitmap.createBitmap(
            bitmap, 0, 0, bitmap.width, bitmap.height,
            Matrix().apply { postRotate(degrees) }, true
        )
        if (rotated != bitmap) bitmap.recycle()
        return rotated
    }
}
