package com.willitfit.util

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.view.PixelCopy
import android.view.View
import com.google.ar.sceneform.ArSceneView
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

object ScreenshotHelper {

    /**
     * Capture a screenshot of the AR view
     */
    suspend fun captureArView(arSceneView: ArSceneView): Bitmap? {
        return suspendCancellableCoroutine { continuation ->
            val bitmap = Bitmap.createBitmap(
                arSceneView.width,
                arSceneView.height,
                Bitmap.Config.ARGB_8888
            )

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                PixelCopy.request(
                    arSceneView,
                    bitmap,
                    { result ->
                        if (result == PixelCopy.SUCCESS) {
                            continuation.resume(bitmap)
                        } else {
                            continuation.resume(null)
                        }
                    },
                    android.os.Handler(arSceneView.context.mainLooper)
                )
            } else {
                // Fallback for older devices
                try {
                    arSceneView.isDrawingCacheEnabled = true
                    arSceneView.buildDrawingCache()
                    val drawingCache = arSceneView.drawingCache
                    if (drawingCache != null) {
                        continuation.resume(drawingCache.copy(Bitmap.Config.ARGB_8888, false))
                    } else {
                        continuation.resume(null)
                    }
                    arSceneView.isDrawingCacheEnabled = false
                } catch (e: Exception) {
                    continuation.resume(null)
                }
            }
        }
    }

    /**
     * Save bitmap to gallery
     */
    fun saveToGallery(context: Context, bitmap: Bitmap): Boolean {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val filename = "WillItFit_$timestamp.jpg"

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Use MediaStore for Android 10+
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/WillItFit")
            }

            val uri = context.contentResolver.insert(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                contentValues
            )

            uri?.let {
                context.contentResolver.openOutputStream(it)?.use { outputStream ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 95, outputStream)
                }
                true
            } ?: false
        } else {
            // Legacy approach for older Android versions
            val picturesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
            val willItFitDir = File(picturesDir, "WillItFit")

            if (!willItFitDir.exists()) {
                willItFitDir.mkdirs()
            }

            val file = File(willItFitDir, filename)

            try {
                FileOutputStream(file).use { outputStream ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 95, outputStream)
                }
                true
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }
    }

    /**
     * Capture a regular view (non-AR)
     */
    fun captureView(view: View): Bitmap? {
        return try {
            view.isDrawingCacheEnabled = true
            view.buildDrawingCache()
            val bitmap = view.drawingCache?.copy(Bitmap.Config.ARGB_8888, false)
            view.isDrawingCacheEnabled = false
            bitmap
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
