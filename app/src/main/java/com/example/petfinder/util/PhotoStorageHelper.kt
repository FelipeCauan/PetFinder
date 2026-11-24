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

/**
 * Helper offline-first para salvar fotos da galeria/câmera no armazenamento interno do app
 * e carregá-las de forma segura em Compose.
 */
object PhotoStorageHelper {

    private const val PET_PHOTOS_DIR = "pet_photos"
    private const val CAMERA_CACHE_DIR = "camera_temp"

    fun createTempCameraUri(context: Context): Pair<Uri, File> {
        val tempDir = File(context.cacheDir, CAMERA_CACHE_DIR).apply { mkdirs() }
        val tempFile = File(tempDir, "cam_${UUID.randomUUID()}.jpg")
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            tempFile
        )
        return uri to tempFile
    }

    suspend fun copyUriToInternalStorage(context: Context, sourceUri: Uri): String? =
        withContext(Dispatchers.IO) {
            runCatching {
                val destDir = File(context.filesDir, PET_PHOTOS_DIR).apply { mkdirs() }
                val destFile = File(destDir, "pet_${UUID.randomUUID()}.jpg")
                context.contentResolver.openInputStream(sourceUri)?.use { input ->
                    FileOutputStream(destFile).use { output ->
                        input.copyTo(output)
                    }
                } ?: return@withContext null
                destFile.absolutePath
            }.getOrNull()
        }

    suspend fun moveFileToInternalStorage(context: Context, tempFile: File): String? =
        withContext(Dispatchers.IO) {
            if (!tempFile.exists() || tempFile.length() == 0L) return@withContext null
            runCatching {
                val destDir = File(context.filesDir, PET_PHOTOS_DIR).apply { mkdirs() }
                val destFile = File(destDir, "pet_${UUID.randomUUID()}.jpg")
                tempFile.copyTo(destFile, overwrite = true)
                tempFile.delete()
                destFile.absolutePath
            }.getOrNull()
        }

    suspend fun loadImageBitmap(path: String, maxDimensionPx: Int = 720): ImageBitmap? =
        withContext(Dispatchers.IO) {
            if (path.isBlank()) return@withContext null
            val file = File(path)
            if (!file.exists()) return@withContext null

            runCatching {
                val boundsOptions = BitmapFactory.Options().apply {
                    inJustDecodeBounds = true
                }
                BitmapFactory.decodeFile(path, boundsOptions)

                val sampleSize = calculateInSampleSize(
                    width = boundsOptions.outWidth,
                    height = boundsOptions.outHeight,
                    reqSize = maxDimensionPx
                )

                val decodeOptions = BitmapFactory.Options().apply {
                    inSampleSize = sampleSize
                }
                val bitmap = BitmapFactory.decodeFile(path, decodeOptions) ?: return@withContext null
                applyExifOrientation(path, bitmap).asImageBitmap()
            }.getOrNull()
        }

    private fun calculateInSampleSize(width: Int, height: Int, reqSize: Int): Int {
        var inSampleSize = 1
        if (height > reqSize || width > reqSize) {
            val halfHeight = height / 2
            val halfWidth = width / 2
            while ((halfHeight / inSampleSize) >= reqSize && (halfWidth / inSampleSize) >= reqSize) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }

    private fun applyExifOrientation(path: String, bitmap: Bitmap): Bitmap {
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
            else -> 0f
        }
        if (degrees == 0f) return bitmap

        val matrix = Matrix().apply { postRotate(degrees) }
        val rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        if (rotated != bitmap) {
            bitmap.recycle()
        }
        return rotated
    }
}
