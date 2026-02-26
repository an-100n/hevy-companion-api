package com.hevycompanion.api.configuration

import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.stereotype.Service
import java.util.Date
import javax.crypto.SecretKey

@Service
class JwtService(
    @Value($$"${SECURITY_jwt_SECRET_KEY}")
    private val secretString: String
) {

    private val secretKey: SecretKey by lazy {
        Keys.hmacShaKeyFor(secretString.toByteArray())
    }

    fun generateToken(userDetails: UserDetails): String {
        return Jwts.builder()
            .subject(userDetails.username) // The "Who" (Subject)
            .issuedAt(Date(System.currentTimeMillis())) // The "When"
            .expiration(Date(System.currentTimeMillis() + 1000 * 60 * 60 * 24)) // Expires in 24 hours
            .signWith(secretKey, Jwts.SIG.HS256) // The Signature (HMAC-SHA256)
            .compact() // Compresses it into the "ey..." string
    }

    // 3. Extract Username
    fun extractUsername(token: String): String {
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
    fun isTokenValid(token: String, userDetails: UserDetails): Boolean {
        val username = extractUsername(token)
        return (username == userDetails.username) && !isTokenExpired(token)
    }

    private fun isTokenExpired(token: String): Boolean {
        val payload = Jwts.parser()
            .verifyWith(secretKey)
            .build()
            .parseSignedClaims(token)
            .payload

        return payload.expiration.before(Date())
    }
}