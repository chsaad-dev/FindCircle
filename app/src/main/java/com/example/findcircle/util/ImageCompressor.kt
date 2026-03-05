package com.example.findcircle.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.exifinterface.media.ExifInterface
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import kotlin.math.max
import kotlin.math.roundToInt

object ImageCompressor {

    private const val MAX_DIMENSION = 1200f // Max width or height

    fun compressImage(context: Context, uri: Uri): Uri {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return uri
            
            // 1. Decode bounds to check dimensions
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeStream(inputStream, null, options)
            inputStream.close()
            
            // 2. Calculate inSampleSize
            options.inSampleSize = calculateInSampleSize(options, MAX_DIMENSION.toInt(), MAX_DIMENSION.toInt())
            options.inJustDecodeBounds = false
            
            // 3. Decode actual bitmap
            val actualInputStream = context.contentResolver.openInputStream(uri) ?: return uri
            var bitmap = BitmapFactory.decodeStream(actualInputStream, null, options) ?: return uri
            actualInputStream.close()
            
            // 4. Handle EXIF rotation
            bitmap = rotateBitmapIfRequired(context, bitmap, uri)
            
            // 5. Scale down if still too large after inSampleSize
            val maxDim = max(bitmap.width, bitmap.height)
            if (maxDim > MAX_DIMENSION) {
                val scale = MAX_DIMENSION / maxDim
                val scaledWidth = (bitmap.width * scale).roundToInt()
                val scaledHeight = (bitmap.height * scale).roundToInt()
                bitmap = Bitmap.createScaledBitmap(bitmap, scaledWidth, scaledHeight, true)
            }

            // 6. Compress and save to temp file
            val tempFile = File(context.cacheDir, "compressed_${System.currentTimeMillis()}.jpg")
            val outputStream = FileOutputStream(tempFile)
            
            // Compress with 80% quality (good balance of size and visual quality)
            bitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
            outputStream.flush()
            outputStream.close()
            
            // Free up memory
            if (!bitmap.isRecycled) {
                bitmap.recycle()
            }
            
            // Return Uri of the compressed file
            Uri.fromFile(tempFile)
        } catch (e: Exception) {
            e.printStackTrace()
            // If anything fails, return original uri so upload still works at least
            uri
        }
    }

    private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
        val (height: Int, width: Int) = options.outHeight to options.outWidth
        var inSampleSize = 1

        if (height > reqHeight || width > reqWidth) {
            val halfHeight: Int = height / 2
            val halfWidth: Int = width / 2
            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }

    private fun rotateBitmapIfRequired(context: Context, bitmap: Bitmap, selectedImage: Uri): Bitmap {
        val input: InputStream? = context.contentResolver.openInputStream(selectedImage)
        val ei: ExifInterface
        try {
            if (input != null) {
                ei = ExifInterface(input)
                val orientation = ei.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
                return when (orientation) {
                    ExifInterface.ORIENTATION_ROTATE_90 -> rotateImage(bitmap, 90f)
                    ExifInterface.ORIENTATION_ROTATE_180 -> rotateImage(bitmap, 180f)
                    ExifInterface.ORIENTATION_ROTATE_270 -> rotateImage(bitmap, 270f)
                    else -> bitmap
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            input?.close()
        }
        return bitmap
    }

    private fun rotateImage(source: Bitmap, angle: Float): Bitmap {
        val matrix = Matrix()
        matrix.postRotate(angle)
        val rotatedImg = Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
        source.recycle()
        return rotatedImg
    }
}
