package com.hevycompanion.api.aichat.controller

import org.springframework.ai.chat.client.ChatClient
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/chat")
class ChatController(
    builder: ChatClient.Builder
) {
    private val chatClient = builder.defaultSystem("You are an elite strength coach. Give concise, data-driven advice.")
        .build()
    @GetMapping()
   suspend fun chat(@RequestParam(value = "message", defaultValue = "Hello. Is bicep curl with dumbbells a good exercise?") message: String): String {
        return chatClient.prompt()
            .user(message)
            .call()
            .content() ?: "No response from AI"
    }

}