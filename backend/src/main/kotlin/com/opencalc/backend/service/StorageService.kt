package com.opencalc.backend.service

import io.minio.BucketExistsArgs
import io.minio.MakeBucketArgs
import io.minio.MinioClient
import io.minio.PutObjectArgs
import io.minio.RemoveObjectArgs
import io.minio.http.Method
import java.io.ByteArrayInputStream
import java.util.UUID
import java.util.concurrent.TimeUnit

class StorageService {

    private val minioClient: MinioClient by lazy {
        val endpoint = System.getenv("MINIO_URL") ?: "http://localhost:9000"
        val accessKey = System.getenv("MINIO_ROOT_USER") ?: "opencalc"
        val secretKey = System.getenv("MINIO_ROOT_PASSWORD") ?: "opencalc_secret"

        MinioClient.builder()
            .endpoint(endpoint)
            .credentials(accessKey, secretKey)
            .build()
    }

    private val bucketName = "chat-media"

    init {
        // Ensure bucket exists
        try {
            val exists = minioClient.bucketExists(
                BucketExistsArgs.builder().bucket(bucketName).build()
            )
            if (!exists) {
                minioClient.makeBucket(
                    MakeBucketArgs.builder().bucket(bucketName).build()
                )
            }
        } catch (e: Exception) {
            println("MinIO initialization warning: ${e.message}")
        }
    }

    fun uploadFile(fileName: String, contentType: String, data: ByteArray): String {
        val objectName = "${UUID.randomUUID()}_$fileName"

        minioClient.putObject(
            PutObjectArgs.builder()
                .bucket(bucketName)
                .`object`(objectName)
                .stream(ByteArrayInputStream(data), data.size.toLong(), -1)
                .contentType(contentType)
                .build()
        )

        // Generate presigned URL (valid for 7 days)
        return minioClient.getPresignedObjectUrl(
            io.minio.GetPresignedObjectUrlArgs.builder()
                .method(Method.GET)
                .bucket(bucketName)
                .`object`(objectName)
                .expiry(7, TimeUnit.DAYS)
                .build()
        )
    }

    fun deleteFile(objectName: String) {
        minioClient.removeObject(
            RemoveObjectArgs.builder()
                .bucket(bucketName)
                .`object`(objectName)
                .build()
        )
    }
}
