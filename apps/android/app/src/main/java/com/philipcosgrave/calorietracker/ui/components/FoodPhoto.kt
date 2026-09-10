package com.philipcosgrave.calorietracker.ui.components

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.philipcosgrave.calorietracker.ui.screens.MemoryPhotoCapture
import java.io.File
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch

suspend fun saveFoodPhoto(context: android.content.Context, bitmap: Bitmap): String = withContext(Dispatchers.IO) {
    val ratio = minOf(1f, 1200f / maxOf(bitmap.width, bitmap.height))
    val resized = if (ratio < 1f) Bitmap.createScaledBitmap(bitmap, (bitmap.width * ratio).toInt(), (bitmap.height * ratio).toInt(), true) else bitmap
    try {
        val directory = File(context.filesDir, "food-photos").apply { mkdirs() }
        val file = File(directory, "${UUID.randomUUID()}.jpg")
        file.outputStream().use { check(resized.compress(Bitmap.CompressFormat.JPEG, 85, it)) }
        file.absolutePath
    } finally { if (resized !== bitmap) resized.recycle() }
}

suspend fun readSelectedPhoto(context: android.content.Context, uri: android.net.Uri): Bitmap = withContext(Dispatchers.IO) {
    if (android.os.Build.VERSION.SDK_INT >= 28) ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, uri)) { decoder, info, _ ->
        val ratio = minOf(1f, 1800f / maxOf(info.size.width, info.size.height))
        decoder.setTargetSize((info.size.width * ratio).toInt(), (info.size.height * ratio).toInt())
        decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
    } else {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri).use { BitmapFactory.decodeStream(it, null, bounds) }
        val options = BitmapFactory.Options().apply { inSampleSize = 1; while (maxOf(bounds.outWidth, bounds.outHeight) / inSampleSize > 1800) inSampleSize *= 2 }
        val decoded = checkNotNull(context.contentResolver.openInputStream(uri).use { BitmapFactory.decodeStream(it, null, options) })
        val orientation = context.contentResolver.openInputStream(uri).use { stream -> stream?.let { android.media.ExifInterface(it).getAttributeInt(android.media.ExifInterface.TAG_ORIENTATION, 1) } ?: 1 }
        val matrix = android.graphics.Matrix().apply {
            when (orientation) {
                2 -> setScale(-1f, 1f)
                3 -> setRotate(180f)
                4 -> setScale(1f, -1f)
                5 -> { setRotate(90f); postScale(-1f, 1f) }
                6 -> setRotate(90f)
                7 -> { setRotate(270f); postScale(-1f, 1f) }
                8 -> setRotate(270f)
            }
        }
        val upright = Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, matrix, true)
        if (upright !== decoded) decoded.recycle()
        upright
    }
}

@Composable fun FoodPhoto(path: String?, modifier: Modifier = Modifier.size(48.dp)) {
    val bitmap by produceState<Bitmap?>(null, path) {
        value = withContext(Dispatchers.IO) { path?.let { runCatching { BitmapFactory.decodeFile(it) }.getOrNull() } }
    }
    val current = bitmap
    if (current != null) Image(current.asImageBitmap(), "Food photo", modifier.clip(MaterialTheme.shapes.medium), contentScale = ContentScale.Crop)
    else DefaultFoodImage(modifier)
}

/** Photos here are explicitly attached to a saved food; meal-analysis captures stay in memory. */
@Composable fun FoodPhotoPicker(path: String?, onChange: (String?) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var options by remember { mutableStateOf(false) }
    var camera by remember { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    fun save(bitmap: Bitmap) { saving = true; scope.launch {
        try { onChange(saveFoodPhoto(context, bitmap)); camera = false }
        catch (e: kotlinx.coroutines.CancellationException) { throw e }
        catch (_: Exception) { error = "Could not attach photo. Please retry." }
        finally { bitmap.recycle(); saving = false }
    } }
    val gallery = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri -> if (uri != null) scope.launch {
        try { save(readSelectedPhoto(context, uri)) } catch (_: Exception) { error = "Could not open this photo." }
    } }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { if (it) camera = true else error = "Allow camera access to take a photo." }
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            FoodPhoto(path, Modifier.size(104.dp))
            TextButton(enabled = !saving, onClick = { options = true }) { Text(if (saving) "Saving photo…" else if (path == null) "Add Photo" else "Change Photo") }
            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        }
    }
    if (options) AlertDialog(onDismissRequest = { options = false }, title = { Text("Food photo") }, text = {
        Column {
            TextButton(onClick = { options = false; if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) camera = true else permission.launch(Manifest.permission.CAMERA) }) { Text("Take photo") }
            TextButton(onClick = { options = false; gallery.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }) { Text("Choose photo") }
            if (path != null) TextButton(onClick = { onChange(null); options = false }) { Text("Remove photo") }
        }
    }, confirmButton = { TextButton(onClick = { options = false }) { Text("Cancel") } })
    if (camera) androidx.compose.ui.window.Dialog(onDismissRequest = { if (!saving) camera = false }, properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)) {
        Surface { Column(Modifier.padding(16.dp)) {
            Text("Take food photo", style = MaterialTheme.typography.titleLarge)
            if (!saving) MemoryPhotoCapture(::save, { error = it; camera = false }) else LinearProgressIndicator(Modifier.fillMaxWidth())
            TextButton(enabled = !saving, onClick = { camera = false }) { Text("Cancel") }
        } }
    }
}
