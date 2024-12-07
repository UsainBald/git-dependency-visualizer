package com.yandexbrouser.visualizer.config

import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import org.junit.jupiter.api.Test
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ConfigLoaderTest {

  @Test
  fun `test valid config loads correctly`() {
    val configLoader = spyk<ConfigLoader>()

    every { configLoader.loadConfig() } returns Config(
      plantUmlPath = "/path/to/plantuml.jar",
      repositoryPath = "/path/to/repository",
      startDate = LocalDate.of(2023, 1, 1)
    )

    val config = configLoader.loadConfig()

    assertEquals("/path/to/plantuml.jar", config.plantUmlPath)
    assertEquals("/path/to/repository", config.repositoryPath)
    assertEquals("2023-01-01", config.startDate.toString())
  }

  @Test
  fun `test invalid config throws exception`() {
    val configLoader = mockk<ConfigLoader>()

    every { configLoader.loadConfig() } throws IllegalArgumentException("Invalid config")

    assertFailsWith<IllegalArgumentException> {
      configLoader.loadConfig()
    }
  }
}
