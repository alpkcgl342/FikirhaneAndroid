package com.alpkcgl.fikirhane.data.image

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.core.graphics.scale
import com.alpkcgl.fikirhane.data.repository.PreparedImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

class ImageException(message: String) : Exception(message)

/**
 * Seçilen görseli yüklemeye hazırlar. Sunucu (Vercel) 4 MB'tan büyük istekleri kabul
 * etmediği için büyük fotoğraflar en fazla 1600 px genişliğe indirilip JPEG'e çevrilir.
 * GIF'ler (animasyon bozulmasın diye) olduğu gibi gönderilir.
 */
class ImagePreparer(private val resolver: ContentResolver) {

    suspend fun prepare(uri: Uri): PreparedImage = withContext(Dispatchers.IO) {
        val mime = resolver.getType(uri)
        if (mime !in ACCEPTED) {
            throw ImageException("Yalnızca JPEG, PNG, WebP veya GIF görseller yüklenebilir")
        }
        if (mime == "image/gif") {
            val bytes = read(uri)
            if (bytes.size > MAX_BYTES) throw ImageException("GIF en fazla 4 MB olabilir")
            return@withContext PreparedImage(bytes, mime, "gorsel.gif")
        }

        // Önce yalnızca boyutları oku; büyük görseli belleğe tam çözmeden örnekleme oranını seç.
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        if (bounds.outWidth <= 0) throw ImageException("Görsel okunamadı")

        var sample = 1
        while (bounds.outWidth / (sample * 2) >= MAX_WIDTH) sample *= 2
        val decoded = resolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample })
        } ?: throw ImageException("Görsel okunamadı")

        val scaled = if (decoded.width > MAX_WIDTH) {
            decoded.scale(MAX_WIDTH, decoded.height * MAX_WIDTH / decoded.width).also { decoded.recycle() }
        } else {
            decoded
        }

        val out = ByteArrayOutputStream()
        scaled.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out)
        scaled.recycle()
        val bytes = out.toByteArray()
        if (bytes.size > MAX_BYTES) throw ImageException("Görsel çok büyük, daha küçük bir görsel seç")
        PreparedImage(bytes, "image/jpeg", "gorsel.jpg")
    }

    private fun read(uri: Uri): ByteArray =
        resolver.openInputStream(uri)?.use { it.readBytes() } ?: throw ImageException("Görsel okunamadı")

    private companion object {
        val ACCEPTED = setOf("image/jpeg", "image/png", "image/webp", "image/gif")
        const val MAX_WIDTH = 1600
        const val MAX_BYTES = 4 * 1024 * 1024
        const val JPEG_QUALITY = 85
    }
}
