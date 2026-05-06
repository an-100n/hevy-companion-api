package com.hevycompanion.api

import io.github.cdimascio.dotenv.dotenv
import org.springframework.context.ApplicationContextInitializer
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.core.env.MapPropertySource

class DotenvInit : ApplicationContextInitializer<ConfigurableApplicationContext> {
    override fun initialize(context: ConfigurableApplicationContext) {
        val dotenv = dotenv {
            ignoreIfMissing = true
        }
        val props = dotenv.entries().associate { it.key to it.value }
        context.environment.propertySources
            .addFirst(MapPropertySource("dotenv", props))
    }
}