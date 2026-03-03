package com.hevycompanion.api.hevy.service

import com.hevycompanion.api.hevy.client.HevyClient
import com.hevycompanion.api.hevy.entity.ExerciseDictionary
import com.hevycompanion.api.hevy.repository.ExerciseDictionaryRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class HevySyncService(
    private val hevyClient: HevyClient,
    private val dictionaryRepository: ExerciseDictionaryRepository
) {
    private val logger = LoggerFactory.getLogger(HevySyncService::class.java)

    @Transactional
    fun syncExerciseDictionary(userId: UUID) {
        logger.info("Starting Exercise Dictionary Sync for user: {}", userId)
        
        var currentPage = 1
        var totalPages = 1
        val allTemplates = mutableListOf<ExerciseDictionary>()

        do {
            try {
                val response = hevyClient.getExerciseTemplates(userId, page = currentPage)
                totalPages = response.pageCount

                val entities = response.exerciseTemplates.map { template ->
                    ExerciseDictionary(
                        id = template.id,
                        title = template.title
                    )
                }
                allTemplates.addAll(entities)
                logger.info("Fetched page {}/{} ({} templates)", currentPage, totalPages, entities.size)

                currentPage++
            } catch (e: Exception) {
                logger.error("Failed to fetch exercise templates on page {}", currentPage, e)
                break // Stop syncing if an error occurs to prevent infinite loops
            }
        } while (currentPage <= totalPages)

        if (allTemplates.isNotEmpty()) {
            // Save all to database. This will insert new ones and update existing ones (thanks to @Id)
            dictionaryRepository.saveAll(allTemplates)
            logger.info("Successfully saved {} exercise templates to the dictionary.", allTemplates.size)
        }
    }
}