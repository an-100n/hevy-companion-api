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
    private val dictionaryRepository: ExerciseDictionaryRepository,
    private val exerciseCache: ExerciseDictionaryCache
) {
    private val logger = LoggerFactory.getLogger(HevySyncService::class.java)

    @Transactional
    fun syncExerciseDictionary(userId: UUID) {
        logger.info("Starting exercise dictionary sync for user: {}", userId)

        var page = 1
        val allTemplates = mutableListOf<ExerciseDictionary>()

        while (true) {
            val response = try {
                hevyClient.getExerciseTemplates(userId, page = page)
            } catch (e: Exception) {
                logger.error("Failed to fetch exercise templates on page {}", page, e)
                break
            }

            response.exerciseTemplates.mapTo(allTemplates) { ExerciseDictionary(id = it.id, title = it.title) }
            logger.info("Fetched page {}/{} ({} templates)", page, response.pageCount, response.exerciseTemplates.size)

            if (page >= response.pageCount) break
            page++
        }

        if (allTemplates.isNotEmpty()) {
            dictionaryRepository.saveAll(allTemplates)
            exerciseCache.refresh()
            logger.info("Saved {} exercise templates and refreshed cache.", allTemplates.size)
        }
    }
}
