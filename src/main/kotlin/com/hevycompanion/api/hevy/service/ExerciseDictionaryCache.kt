package com.hevycompanion.api.hevy.service

import com.hevycompanion.api.hevy.repository.ExerciseDictionaryRepository
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component

@Component
class ExerciseDictionaryCache(private val repository: ExerciseDictionaryRepository) {

    @Volatile
    private var titleById: Map<String, String> = emptyMap()

    @EventListener(ApplicationReadyEvent::class)
    fun load() {
        titleById = repository.findAll().associate { it.id to it.title }
    }

    // Called after a dictionary sync so new exercise names are immediately available
    fun refresh() = load()

    fun getTitle(exerciseTemplateId: String): String =
        titleById[exerciseTemplateId] ?: "Unknown"
}
