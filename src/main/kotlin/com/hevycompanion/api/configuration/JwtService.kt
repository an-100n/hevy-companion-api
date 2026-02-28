package com.hevycompanion.api.configuration

import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import javax.crypto.SecretKey

@Service
class JwtService(
    @Value($$"${SUPABASE_CURRENT_KEY}")
    private val secretString: String
) {

    private val secretKey: SecretKey by lazy {
        Keys.hmacShaKeyFor(secretString.toByteArray())
    }


    // 3. Extract Username
    fun extractUserId(token: String): String {
        // This validates the signature AND parses the data in one step.
        // If the signature is wrong, this line throws a runtime exception!
        val payload = Jwts.parser()
            .verifyWith(secretKey)
            .build()
            .parseSignedClaims(token)
            .payload

        return payload.subject
    }
    // 4. VALIDATE TOKEN
    // Checks: Is the username correct? Is the token expired?
    fun isTokenValid(token: String): Boolean {
        return try{
            Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
            true
        }catch (e: Exception){
            println("Invalid JWT: ${e.message}")
            false
        }
    }


}