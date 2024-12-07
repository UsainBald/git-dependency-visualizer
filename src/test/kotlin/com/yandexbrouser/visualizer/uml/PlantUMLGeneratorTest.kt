package com.yandexbrouser.visualizer.uml

import io.mockk.*
import org.junit.jupiter.api.Test
import kotlin.test.assertTrue
import java.io.File

class PlantUMLGeneratorTest {

  @Test
  fun `test generate PlantUML`() {
    val umlGenerator = PlantUMLGenerator("/path/to/plantuml.jar", "output")
    val files = listOf("file1.kt", "file2.kt", "file3.kt")
    val fileDependencies = mapOf(
      "file1.kt" to listOf("file2.kt"),
      "file2.kt" to listOf("file3.kt")
    )

    val plantUmlContent = umlGenerator.generatePlantUml("commit1", files, fileDependencies)

    assertTrue(plantUmlContent.contains("file1.kt"))
    assertTrue(plantUmlContent.contains("\"file1.kt\" --> \"file2.kt\""))
  }

  @Test
  fun `test save PlantUML file and delete after`() {
    val umlGenerator = spyk(PlantUMLGenerator("/path/to/plantuml.jar", "output"))
    val plantUmlContent = "@startuml\n@enduml"

    every { umlGenerator.savePlantUmlFile(any(), any()) } answers {
      val file = File("output/commit1.puml")
      file.writeText(arg(1)) // Writes the content to a file
      file
    }

    // Execute the save operation
    umlGenerator.savePlantUmlFile("commit1", plantUmlContent)

    // Check if the file was created
    val plantUmlFile = File("output/commit1.puml")
    assertTrue(plantUmlFile.exists())

    // Clean up by deleting the file
    plantUmlFile.delete()
    assertTrue(!plantUmlFile.exists())
  }
}
