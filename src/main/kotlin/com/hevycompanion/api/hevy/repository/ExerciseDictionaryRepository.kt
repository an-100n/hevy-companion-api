package com.hevycompanion.api.hevy.repository

import com.hevycompanion.api.hevy.entity.ExerciseDictionary
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface ExerciseDictionaryRepository : JpaRepository<ExerciseDictionary, String>