package com.hevycompanion.api.hevy.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table

@Entity
@Table(name = "exercise_dictionary")
class ExerciseDictionary(
    @Id
    @Column(name = "id", updatable = false, nullable = false)
    val id: String, // This is the Hevy exercise_template_id (e.g., "1B2B1E7C")

    @Column(nullable = false)
    val title: String
)