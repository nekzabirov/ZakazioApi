package com.zakaion.api.service

import org.springframework.core.io.Resource
import org.springframework.core.io.UrlResource
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.MalformedURLException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.streams.toList

@Service
class StorageService {

    //private val root: Path = Paths.get("/Users/mykyta/Documents/ZakazioApi/test")

    private val root: Path = Paths.get("/tmp")

    fun init() {
         try {
             Files.createDirectory(root)
         } catch (e: IOException) {
             println(e)
         }
    }

    fun list(): List<String> {
        return Files.list(root).map { it.toString() }.toList()
    }

    fun store(file: MultipartFile): String {
        try {
            val fileName = System.currentTimeMillis().toString() + "." + file.originalFilename?.substringAfterLast('.', "")
            Files.copy(file.inputStream, this.root.resolve(fileName))
            return fileName
        } catch (e: Exception) {
            throw RuntimeException("Could not store the file. Error: " + e.message)
        }
    }

    fun store(bytes: ByteArray, format: String): String {
        val fileName = System.currentTimeMillis().toString() + "." + format

        FileOutputStream(fileName).use { stream -> stream.write(bytes) }

        Files.copy(File(fileName).inputStream(), this.root.resolve(fileName))

        File(fileName).delete()

        return fileName
    }

    fun delete(filename: String): Boolean {
        return try {
            Files.delete(this.root.resolve(filename))
            true
        } catch (e: Exception) {
            false
        }
    }

    fun loadAsResource(filename: String): Resource {
        return try {
            val file = root.resolve(filename)
            val resource: Resource = UrlResource(file.toUri())
            if (resource.exists() || resource.isReadable) {
                resource
            } else {
                throw RuntimeException("Could not read the file!")
            }
        } catch (e: MalformedURLException) {
            throw RuntimeException("Error: " + e.message)
        }
    }

    fun loadAsFile(filename: String): File {
        return try {
            val file = root.resolve(filename)
            file.toFile()
        } catch (e: MalformedURLException) {
            throw RuntimeException("Error: " + e.message)
        }
    }

}