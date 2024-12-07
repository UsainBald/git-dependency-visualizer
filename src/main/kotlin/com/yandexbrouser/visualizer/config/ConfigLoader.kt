package com.yandexbrouser.visualizer.config

import java.io.InputStream
import java.time.LocalDate
import java.time.format.DateTimeFormatter

data class Config(
  val plantUmlPath: String,
  val repositoryPath: String,
  val startDate: LocalDate
)

class ConfigLoader {

  fun loadConfig(): Config {
    val configFileStream: InputStream = this::class.java.classLoader.getResourceAsStream("config.yaml")
      ?: throw IllegalArgumentException("config.yaml not found in resources folder")

    val yaml = configFileStream.bufferedReader().use { it.readText() }
    val configProps = yaml.split("\n").associate {
      val (key, value) = it.split(": ")
      key.trim() to value.trim()
    }

    val plantUmlPath = configProps["plant_uml_path"] ?: throw IllegalArgumentException("Plant UML path not specified.")
    val repositoryPath = configProps["repository_path"] ?: throw IllegalArgumentException("Repository path not specified.")
    val dateStr = configProps["start_date"] ?: throw IllegalArgumentException("Start date not specified.")

    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    val startDate = LocalDate.parse(dateStr, formatter)

    return Config(plantUmlPath, repositoryPath, startDate)
  }
}
