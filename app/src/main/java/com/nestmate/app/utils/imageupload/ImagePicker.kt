package com.nestmate.app.utils.imageupload

import android.Manifest
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState

class ImagePickerHandle internal constructor(val launch: () -> Unit)

@Composable
fun rememberImagePicker(
    maxCount: Int,
    onPicked: (List<Uri>) -> Unit,
): ImagePickerHandle {
    require(maxCount >= 1) { "maxCount must be at least 1" }
    val context = LocalContext.current
    val photoPickerAvailable = remember { PickVisualMedia.isPhotoPickerAvailable(context) }

    return if (photoPickerAvailable) {
        rememberPhotoPicker(maxCount, onPicked)
    } else {
        rememberGetContentPicker(onPicked)
    }
}

@Composable
private fun rememberPhotoPicker(
    maxCount: Int,
    onPicked: (List<Uri>) -> Unit,
): ImagePickerHandle {
    return if (maxCount == 1) {
        val launcher = rememberLauncherForActivityResult(
            contract = PickVisualMedia(),
            onResult = { uri -> onPicked(listOfNotNull(uri)) },
        )
        ImagePickerHandle {
            launcher.launch(PickVisualMediaRequest(PickVisualMedia.ImageOnly))
        }
    } else {
        val launcher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.PickMultipleVisualMedia(maxCount),
            onResult = { uris -> onPicked(uris) },
        )
        ImagePickerHandle {
            launcher.launch(PickVisualMediaRequest(PickVisualMedia.ImageOnly))
        }
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
private fun rememberGetContentPicker(
    onPicked: (List<Uri>) -> Unit,
): ImagePickerHandle {
    val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_IMAGES
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }
    val permissionState = rememberPermissionState(permission)

    val contentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri -> onPicked(listOfNotNull(uri)) },
    )

    var pendingLaunch by remember { mutableStateOf(false) }
    LaunchedEffect(pendingLaunch, permissionState.status) {
        if (pendingLaunch && permissionState.status.isGranted) {
            contentLauncher.launch("image/*")
            pendingLaunch = false
        }
    }

    return ImagePickerHandle {
        if (permissionState.status.isGranted) {
            contentLauncher.launch("image/*")
        } else {
            pendingLaunch = true
            permissionState.launchPermissionRequest()
        }
    }
}
