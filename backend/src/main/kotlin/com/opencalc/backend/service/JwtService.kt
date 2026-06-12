package com.opencalc.backend.service

import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import java.util.Base64
import java.util.Date

object JwtService {
    private const val SECRET = "opencalc-stealth-secret-key-change-in-production"
    private const val EXPIRATION_MS = 86400000 // 24 hours

    private fun hmacSha256(data: String): String {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(SECRET.toByteArray(), "HmacSHA256"))
        return Base64.getUrlEncoder().withoutPadding().encodeToString(mac.doFinal(data.toByteArray()))
    }

    fun generateToken(roomId: String): String {
        val header = Base64.getUrlEncoder().withoutPadding()
            .encodeToString("""{"alg":"HS256","typ":"JWT"}""".toByteArray())
        val now = System.currentTimeMillis()
        val payload = Base64.getUrlEncoder().withoutPadding()
            .encodeToString("""{"roomId":"$roomId","exp":${now + EXPIRATION_MS},"iat":$now}""".toByteArray())
        val signature = hmacSha256("$header.$payload")
        return "$header.$payload.$signature"
    }

    fun verifyToken(token: String): String? {
        return try {
            val parts = token.split(".")
            if (parts.size != 3) return null

            val expectedSig = hmacSha256("${parts[0]}.${parts[1]}")
            if (expectedSig != parts[2]) return null

            val payload = String(Base64.getUrlDecoder().decode(parts[1]))
            // Simple extraction of roomId
            val roomIdMatch = """"roomId":"([^"]+)"""".toRegex().find(payload)
            val expMatch = """"exp":([0-9]+)""".toRegex().find(payload)

            val exp = expMatch?.groupValues?.get(1)?.toLongOrNull() ?: return null
            if (System.currentTimeMillis() > exp) return null

            roomIdMatch?.groupValues?.get(1)
        } catch (e: Exception) {
            null
        }
    }
}
