package com.example.petfinder.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.petfinder.util.PhotoStorageHelper

@Composable
fun PetLocalImage(
    photoPath: String?,
    contentDescription: String?,
    fallbackEmoji: String,
    modifier: Modifier = Modifier,
    fallbackEmojiSize: TextUnit = 48.sp
) {
    val bitmap by produceState<ImageBitmap?>(initialValue = null, key1 = photoPath) {
        value = photoPath?.takeIf { it.isNotBlank() }?.let { PhotoStorageHelper.loadImageBitmap(it) }
    }

    Box(
        modifier = modifier.background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center
    ) {
        val loaded = bitmap
        if (loaded != null) {
            Image(bitmap = loaded, contentDescription = contentDescription, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        } else {
            Text(text = fallbackEmoji, fontSize = fallbackEmojiSize)
        }
    }
}

@Composable
fun PhotoSourceDialog(
    title: String,
    onDismiss: () -> Unit,
    onTakePhoto: () -> Unit,
    onPickFromGallery: () -> Unit,
    onRemovePhoto: (() -> Unit)? = null
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                DialogActionRow(Icons.Outlined.PhotoCamera, "Tirar foto com a câmera", MaterialTheme.colorScheme.primary) {
                    onDismiss()
                    onTakePhoto()
                }
                DialogActionRow(Icons.Outlined.PhotoLibrary, "Escolher da galeria", MaterialTheme.colorScheme.primary) {
                    onDismiss()
                    onPickFromGallery()
                }
                if (onRemovePhoto != null) {
                    DialogActionRow(Icons.Outlined.DeleteOutline, "Remover foto atual", MaterialTheme.colorScheme.error) {
                        onDismiss()
                        onRemovePhoto()
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}

@Composable
private fun DialogActionRow(
    icon: ImageVector,
    label: String,
    tint: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = tint)
        Spacer(modifier = Modifier.width(12.dp))
        Text(text = label, color = if (tint == MaterialTheme.colorScheme.error) tint else Color.Unspecified)
    }
}
