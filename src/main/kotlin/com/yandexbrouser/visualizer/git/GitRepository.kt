package com.yandexbrouser.visualizer.git

import java.io.File

class GitRepository(private val repoPath: String) {

  fun getCommitsAfterDate(startDate: String): List<String> {
    val result = runCommand("git -C $repoPath log --after=$startDate --pretty=format:%H")
    return result.lines().filter { it.isNotBlank() }
  }

  fun getFilesInCommit(commitHash: String): List<String> {
    val result = runCommand("git -C $repoPath diff-tree --no-commit-id --name-only -r $commitHash")
    return result.lines().filter { it.isNotBlank() }
  }

  fun getDependenciesInFile(filePath: String): List<String> {
    val file = File(repoPath, filePath)
    if (!file.exists()) return emptyList()

    val dependencies = mutableListOf<String>()
    file.forEachLine { line ->
      if (line.trim().startsWith("import ")) {
        val dependency = extractDependency(line)
        if (dependency != null) {
          dependencies.add(dependency)
        }
      }
    }
    return dependencies
  }

  private fun extractDependency(importLine: String): String? {
    val importStatement = importLine.removePrefix("import").trim()
    val parts = importStatement.split(".")
    return if (parts.isNotEmpty()) {
      parts.last()
    } else {
      null
    }
  }

  private fun runCommand(command: String): String {
    val process = Runtime.getRuntime().exec(command)
    return process.inputStream.bufferedReader().readText()
  }
}
