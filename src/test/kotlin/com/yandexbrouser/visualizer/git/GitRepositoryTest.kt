package com.yandexbrouser.visualizer.git

import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class GitRepositoryTest {

  @Test
  fun `test get commits after date`() {
    val gitRepo = mockk<GitRepository>()

    every { gitRepo.getCommitsAfterDate("2023-01-01") } returns listOf("commit1", "commit2", "commit3")

    val commits = gitRepo.getCommitsAfterDate("2023-01-01")
    assertEquals(listOf("commit1", "commit2", "commit3"), commits)
  }

  @Test
  fun `test get files in commit`() {
    val gitRepo = mockk<GitRepository>()

    every { gitRepo.getFilesInCommit("commit1") } returns listOf("file1.kt", "file2.kt", "file3.kt")

    val files = gitRepo.getFilesInCommit("commit1")
    assertEquals(listOf("file1.kt", "file2.kt", "file3.kt"), files)
  }

  @Test
  fun `test get dependencies in file`() {
    val gitRepo = mockk<GitRepository>()

    every { gitRepo.getDependenciesInFile("file1.kt") } returns listOf("file2.kt")

    val dependencies = gitRepo.getDependenciesInFile("file1.kt")
    assertEquals(listOf("file2.kt"), dependencies)
  }
}
