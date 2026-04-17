package com.hamzaasad.gymmanagerpro.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.security.MessageDigest

object FileUtils {

    fun saveImageToInternalStorage(context: Context, uri: Uri, fileName: String): String? {
        return try {
            val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
            val outputDir = File(context.filesDir, "images")
            if (!outputDir.exists()) outputDir.mkdirs()
            
            val outputFile = File(outputDir, "$fileName.jpg")
            val outputStream = FileOutputStream(outputFile)
            
            inputStream?.use { input ->
                outputStream.use { output ->
                    input.copyTo(output)
                }
            }
            outputFile.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun saveBitmapToInternalStorage(context: Context, bitmap: Bitmap, fileName: String): String? {
        return try {
            val outputDir = File(context.filesDir, "images")
            if (!outputDir.exists()) outputDir.mkdirs()
            
            val outputFile = File(outputDir, "$fileName.jpg")
            val outputStream = FileOutputStream(outputFile)
            
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream)
            outputStream.flush()
            outputStream.close()
            
            outputFile.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun getImageHash(filePath: String): String? {
        return try {
            val file = File(filePath)
            if (!file.exists()) return null
            val bytes = file.readBytes()
            val md = MessageDigest.getInstance("MD5")
            val digest = md.digest(bytes)
            digest.fold("") { str, it -> str + "%02x".format(it) }
        } catch (e: Exception) {
            null
        }
    }

    fun zipDirectory(sourceDir: File, dbFile: File, zipFile: File) {
        java.util.zip.ZipOutputStream(FileOutputStream(zipFile)).use { zos ->
            // 1. Add Database
            if (dbFile.exists()) {
                val entry = java.util.zip.ZipEntry(dbFile.name)
                zos.putNextEntry(entry)
                dbFile.inputStream().use { it.copyTo(zos) }
                zos.closeEntry()
            }

            // 2. Add Images Directory
            if (sourceDir.exists() && sourceDir.isDirectory) {
                sourceDir.listFiles()?.forEach { file ->
                    val entry = java.util.zip.ZipEntry("images/${file.name}")
                    zos.putNextEntry(entry)
                    file.inputStream().use { it.copyTo(zos) }
                    zos.closeEntry()
                }
            }
        }
    }

    fun unzipToDirectory(zipUri: Uri, context: Context, targetDbFile: File, targetImagesDir: File) {
        context.contentResolver.openInputStream(zipUri)?.use { input ->
            java.util.zip.ZipInputStream(input).use { zis ->
                var entry = zis.nextEntry
                while (entry != null) {
                    when {
                        entry.name == targetDbFile.name -> {
                            FileOutputStream(targetDbFile).use { zis.copyTo(it) }
                        }
                        entry.name.startsWith("images/") -> {
                            if (!targetImagesDir.exists()) targetImagesDir.mkdirs()
                            val fileName = entry.name.substringAfter("images/")
                            if (fileName.isNotEmpty()) {
                                FileOutputStream(File(targetImagesDir, fileName)).use { zis.copyTo(it) }
                            }
                        }
                    }
                    zis.closeEntry()
                    entry = zis.nextEntry
                }
            }
        }
    }
}
