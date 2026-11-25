package com.example.petfinder.ui.components

import android.Manifest
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.example.petfinder.util.LocationHelper
import com.example.petfinder.util.PhotoStorageHelper
import kotlinx.coroutines.launch
import java.io.File

data class PhotoPickerActions(
    val pickFromGallery: () -> Unit,
    val takePhoto: () -> Unit
)

@Composable
fun rememberPhotoPickerActions(
    dirName: String = PhotoStorageHelper.PET_PHOTOS_DIR,
    filePrefix: String = "pet",
    onPhotoSaved: (String) -> Unit,
    onError: (String) -> Unit
): PhotoPickerActions {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var pendingCameraPath by rememberSaveable { mutableStateOf<String?>(null) }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val savedPath = PhotoStorageHelper.copyUriToInternalStorage(context, uri, dirName, filePrefix)
            if (savedPath != null) onPhotoSaved(savedPath)
            else onError("Não foi possível carregar a imagem selecionada.")
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        val path = pendingCameraPath
        pendingCameraPath = null
        if (!success || path == null) return@rememberLauncherForActivityResult
        scope.launch {
            val savedPath = PhotoStorageHelper.moveFileToInternalStorage(context, File(path), dirName, filePrefix)
            if (savedPath != null) onPhotoSaved(savedPath)
            else onError("Não foi possível salvar a foto capturada.")
        }
    }

    return remember(context, dirName, filePrefix) {
        PhotoPickerActions(
            pickFromGallery = {
                galleryLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            },
            takePhoto = {
                val (uri, tempFile) = PhotoStorageHelper.createTempCameraUri(context)
                pendingCameraPath = tempFile.absolutePath
                cameraLauncher.launch(uri)
            }
        )
    }
}

@Composable
fun rememberGpsRequester(
    locationHelper: LocationHelper,
    onPermissionGranted: () -> Unit,
    onPermissionDenied: () -> Unit
): () -> Unit {
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) onPermissionGranted() else onPermissionDenied()
    }

    return remember(locationHelper) {
        {
            if (locationHelper.hasLocationPermission()) {
                onPermissionGranted()
            } else {
                permissionLauncher.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                )
            }
        }
    }
}
