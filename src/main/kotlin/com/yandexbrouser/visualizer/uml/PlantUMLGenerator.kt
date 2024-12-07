package com.yandexbrouser.visualizer.uml

import java.io.File

class PlantUMLGenerator(private val plantUmlPath: String, private val outputDir: String) {

  fun generatePlantUml(commitHash: String, files: List<String>, fileDependencies: Map<String, List<String>>): String {
    val sb = StringBuilder()
    sb.append("@startuml\n")
    sb.append("title Commit: $commitHash\n")

    files.forEach { file ->
      sb.append("class \"$file\"\n")
    }

    fileDependencies.forEach { (file, dependencies) ->
      dependencies.forEach { dependency ->
        sb.append("\"$file\" --> \"$dependency\"\n")
      }
    }

    sb.append("@enduml\n")
    return sb.toString()
  }

  fun savePlantUmlFile(commitHash: String, plantUmlContent: String) {
    val plantUmlFile = File("$outputDir/$commitHash.puml")
    plantUmlFile.writeText(plantUmlContent)
  }

  fun generateGraphImage(commitHash: String) {
    val plantUmlFile = File("$outputDir/$commitHash.puml")
    val result = runCommand("java -jar $plantUmlPath ${plantUmlFile.absolutePath}")
    if (result.isNotBlank()) {
      println("Error generating image for commit $commitHash: $result")
    }
  }

  private fun runCommand(command: String): String {
    val process = Runtime.getRuntime().exec(command)
    return process.inputStream.bufferedReader().readText()
  }
}
