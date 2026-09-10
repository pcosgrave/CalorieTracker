package com.philipcosgrave.calorietracker.ui.screens
import android.Manifest
import android.content.ClipboardManager
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.philipcosgrave.calorietracker.ui.components.*
import com.philipcosgrave.calorietracker.data.local.readRecipeText
import kotlinx.coroutines.launch

@Composable fun RecipeScanScreen(onBack: () -> Unit, onExtracted: (String, String?) -> Unit) {
    androidx.activity.compose.BackHandler(onBack = onBack)
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var camera by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var text by rememberSaveable { mutableStateOf("") }
    var photo by rememberSaveable { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    fun read(bitmap: Bitmap) { camera = false; busy = true; error = null; scope.launch {
        try {
            text = readRecipeText(bitmap)
            if (text.isBlank()) error = "No recipe text found. Try a clearer photo or paste the text."
            else photo = saveFoodPhoto(context, bitmap)
        } catch (e: kotlinx.coroutines.CancellationException) { throw e }
        catch (_: Exception) { error = "Could not read this recipe. Retake the photo or paste the text." }
        finally { bitmap.recycle(); busy = false }
    } }
    val gallery = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri -> if (uri != null) scope.launch {
        try { read(readSelectedPhoto(context, uri)) } catch (_: Exception) { error = "Could not open the photo." }
    } }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { if (it) camera = true else error = "Camera permission is needed to take a recipe photo." }
    Page {
        PageHeader(if (busy) "Extracting recipe…" else "Add Recipe", onBack = onBack)
        Text("Scan a recipe from a book, photo or screenshot.", color = AppMuted)
        if (busy) {
            CircularProgressIndicator()
            Text("Reading the recipe on your phone…")
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                OutlinedButton(onClick = { if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) camera = true else permission.launch(Manifest.permission.CAMERA) }, modifier = Modifier.weight(1f)) { Text("Camera") }
                OutlinedButton(onClick = { gallery.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }, modifier = Modifier.weight(1f)) { Text("Photos") }
                OutlinedButton(onClick = {
                    val clip = (context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).primaryClip
                    val item = clip?.takeIf { it.itemCount > 0 }?.getItemAt(0)
                    if (item?.uri != null) scope.launch { try { read(readSelectedPhoto(context, item.uri)) } catch (_: Exception) { error = "Could not read the pasted image." } }
                    else { text = item?.text?.toString().orEmpty(); if (text.isBlank()) error = "Copy recipe text or an image first." }
                }, modifier = Modifier.weight(1f)) { Text("Paste") }
            }
            if (camera) MemoryPhotoCapture(::read, { error = it })
            else {
                photo?.let { FoodPhoto(it, Modifier.fillMaxWidth().height(160.dp)) }
                OutlinedTextField(text, { text = it }, modifier = Modifier.fillMaxWidth().heightIn(min = 200.dp), label = { Text("Recipe text") }, placeholder = { Text("Recipe name\nServings\nIngredients\nInstructions (optional)") })
                Text("Review the extracted text. Nutrition will come from your known ingredients.", color = AppMuted)
                AppPrimaryButton("Review Recipe", { onExtracted(text, photo) }, Modifier.fillMaxWidth(), enabled = text.isNotBlank())
            }
            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        }
    }
}
