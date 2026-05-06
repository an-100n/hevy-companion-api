package com.hevycompanion.api.configuration

import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

// A single place for all error handling. Every controller can now throw exceptions
// freely without wrapping calls in try-catch — Spring routes them here automatically.
@RestControllerAdvice
class GlobalExceptionHandler {

    private val logger = LoggerFactory.getLogger(GlobalExceptionHandler::class.java)

    data class ErrorResponse(val message: String)

    // 404 — resource doesn't exist (e.g. user not found, routine not found)
    @ExceptionHandler(IllegalArgumentException::class)
    fun handleNotFound(e: IllegalArgumentException): ResponseEntity<ErrorResponse> {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ErrorResponse(e.message ?: "Resource not found"))
    }

    // 400 — bad state (e.g. Hevy API key not configured, encryption failure)
    @ExceptionHandler(IllegalStateException::class)
    fun handleBadRequest(e: IllegalStateException): ResponseEntity<ErrorResponse> {
        return ResponseEntity.badRequest().body(ErrorResponse(e.message ?: "Bad request"))
    }

    // 500 — anything unexpected
    @ExceptionHandler(Exception::class)
    fun handleGeneric(e: Exception): ResponseEntity<ErrorResponse> {
        logger.error("Unhandled exception", e)
        return ResponseEntity.internalServerError().body(ErrorResponse("An unexpected error occurred"))
    }
}
