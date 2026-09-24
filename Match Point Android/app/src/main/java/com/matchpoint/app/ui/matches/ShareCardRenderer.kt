package com.matchpoint.app.ui.matches

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import androidx.compose.ui.graphics.toArgb
import androidx.core.content.FileProvider
import com.matchpoint.app.ui.theme.CourtTheme
import java.io.File
import java.io.FileOutputStream

/**
 * Rasterizes [ShareCardContent] (captured at its natural height by the caller) onto a fixed
 * 1080x1920 Instagram Story canvas — centered if it's shorter, scaled down (letterboxed) if a
 * mabar had enough matches/players to run taller than the story frame. Ported 1:1 from iOS's
 * ShareCardRenderer (SwiftUI's ImageRenderer + UIGraphicsImageRenderer).
 */
object ShareCardRenderer {
    private const val CANVAS_WIDTH = 1080
    private const val CANVAS_HEIGHT = 1920

    fun composeToStoryCanvas(source: Bitmap, theme: CourtTheme): Bitmap {
        val output = Bitmap.createBitmap(CANVAS_WIDTH, CANVAS_HEIGHT, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        canvas.drawColor(theme.backgroundBottom.toArgb())

        var drawWidth = source.width
        var drawHeight = source.height
        if (drawHeight > CANVAS_HEIGHT) {
            val scale = CANVAS_HEIGHT.toFloat() / drawHeight
            drawWidth = (drawWidth * scale).toInt()
            drawHeight = CANVAS_HEIGHT
        }
        val left = (CANVAS_WIDTH - drawWidth) / 2
        val top = maxOf(0, (CANVAS_HEIGHT - drawHeight) / 2)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        canvas.drawBitmap(source, null, Rect(left, top, left + drawWidth, top + drawHeight), paint)
        return output
    }

    /** Writes the recap to a cache-dir PNG and returns a content:// URI ready for a share sheet. */
    fun saveToCache(context: Context, image: Bitmap, sessionName: String): android.net.Uri {
        val dir = File(context.cacheDir, "share_cards").apply { mkdirs() }
        val safeName = sessionName.replace("/", "-")
        val file = File(dir, "MatchPoint-$safeName-Recap.png")
        FileOutputStream(file).use { out -> image.compress(Bitmap.CompressFormat.PNG, 100, out) }
        return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }

    fun shareIntent(uri: android.net.Uri): Intent {
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        return Intent.createChooser(send, "Share Recap")
    }
}
