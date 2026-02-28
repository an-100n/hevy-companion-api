package com.hevycompanion.api.configuration

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

@Component
class JwtAuthenticationFilter(
    private val jwtService: JwtService,
) : OncePerRequestFilter() {

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        val authHeader = request.getHeader("Authorization")

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response)
            return
        }

        val jwt = authHeader.substring(7)

if(jwtService.isTokenValid(jwt)){
    val userId = jwtService.extractUserId(jwt)
    if (SecurityContextHolder.getContext().authentication == null) {

        // Step 3: Create the authentication token MANUALLY
        val authorities = listOf(SimpleGrantedAuthority("ROLE_USER")) // Grant a default role

        val authToken = UsernamePasswordAuthenticationToken(
            userId, // The "principal" is now just the User's ID string
            null,
            authorities
        )

        authToken.details = WebAuthenticationDetailsSource().buildDetails(request)

        // Step 4: Tell Spring Security the user is authenticated
        SecurityContextHolder.getContext().authentication = authToken
    }
}

//        try {
//            // In a real app, handle generic exceptions here to avoid 500s on malformed tokens
//            val username = jwtService.extractUsername(jwt)
//
//            if (username != null && SecurityContextHolder.getContext().authentication == null) {
//                val userDetails = userDetailsService.loadUserByUsername(username)
//
//                if (jwtService.isTokenValid(jwt, userDetails)) {
//                    val authToken = UsernamePasswordAuthenticationToken(
//                        userDetails,
//                        null,
//                        userDetails.authorities
//                    )
//                    authToken.details = WebAuthenticationDetailsSource().buildDetails(request)
//                    SecurityContextHolder.getContext().authentication = authToken
//                }
//            }
//        } catch (e: Exception) {
//            // Log the error so we know WHY auth failed, but don't crash the request.
//            println("JWT Authentication Failed: ${e.message}")
//        }

        filterChain.doFilter(request, response)
    }
}