package com.yandexbrouser.visualizer

import GitObjectNode
import decompressObject
import generateDiagramUsingJar
import generatePlantUML
import getLastCommit
import io.mockk.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import parseCommit
import parseObject
import parseTree
import java.io.ByteArrayInputStream
import java.io.File

class ApplicationTest {

    @BeforeEach
    fun setUp() {
        // Перед каждым тестом сбрасываем глобальные переменные
        clearAllMocks()
    }

    @Test
    fun `test decompressObject file not found`() {
        val testFile = mockk<File>()
        every { testFile.exists() } returns false
        every { testFile.absolutePath } returns "/path/not/found"

        val exception = assertThrows<IllegalArgumentException> {
            decompressObject(testFile)
        }
        assertTrue(exception.message!!.contains("Файл не найден"))
    }

    @Test
    fun `test parseObject with commit`() {
        // Мокаем decompressObject, чтобы вернуть фейковый commit-объект
        mockkStatic("ApplicationKt") // чтобы замокать функции верхнего уровня
        every { decompressObject(any()) } returns ("commit 123\\0tree abcd\n\nmessage").toByteArray(Charsets.UTF_8)

        // Мокаем parseCommit так как он вызывается внутри parseObject
        every { parseCommit(any()) } returns emptyList()

        val node = parseObject("c7b3d8")
        assertEquals("[]\nc7b3d8", node.label)
    }

    @Test
    fun `test parseCommit`() {
        mockkStatic("ApplicationKt")
        val commitContent = """
            tree abcdef1234567890
            parent 1122334455667788
            parent 99aabbccddeeff00
            author Someone <someone@example.com> 1234567890 +0000
            committer Another <another@example.com> 1234567890 +0000

            This is a commit message
        """.trimIndent()

        every { parseObject("abcdef1234567890") } returns GitObjectNode("treeNode")
        every { parseObject("1122334455667788") } returns GitObjectNode("parent1")
        every { parseObject("99aabbccddeeff00") } returns GitObjectNode("parent2")

        val nodes = parseCommit(commitContent.toByteArray())
        assertEquals(3, nodes.size)
        assertEquals("treeNode", nodes[0].label)
        assertEquals("parent1", nodes[1].label)
        assertEquals("parent2", nodes[2].label)
    }

    @Test
    fun `test parseTree`() {
        mockkStatic("ApplicationKt")
        // Формируем псевдоданные, имитирующие дерево:
        // mode name\0sha
        // Предположим имя = "file.txt", sha = 20 байт нулей
        val mode = "100644".toByteArray()
        val name = "file.txt".toByteArray()
        val sha = ByteArray(20) { 0x11 }
        val rawContent = mode + " ".toByteArray() + name + 0.toByte() + sha

        every { parseObject(any(), any()) } returns GitObjectNode("[blob]\n111111")

        val children = parseTree(rawContent)
        assertEquals(1, children.size)
        assertTrue(children[0].label.contains("[blob]"))
    }

    @Test
    fun `test generatePlantUML`() {
        mockkStatic("ApplicationKt")
        val mockRootNode = GitObjectNode(
            "[commit]\nabc123", mutableListOf(
                GitObjectNode("[tree]\nabc234")
            )
        )

        every { getLastCommit() } returns "abc123def456"
        every { parseObject("abc123def456") } returns mockRootNode

        val testFile = File("graph.puml")
        if (testFile.exists()) testFile.delete()

        generatePlantUML("graph.puml")

        assertTrue(testFile.exists())
        val content = testFile.readText()
        assertTrue(content.contains("\"[commit]\\nabc123\" --> \"[tree]\\nabc234\""))
    }

    @Test
    fun `test generateDiagramUsingJar`() {
        // Мокаем процесс и проверяем его поведение
        mockkConstructor(ProcessBuilder::class)
        val mockProcess = mockkClass(Process::class)

        every { anyConstructed<ProcessBuilder>().start() } returns mockProcess
        every { mockProcess.inputStream } returns ByteArrayInputStream("PlantUML output".toByteArray())
        every { mockProcess.waitFor() } returns 0

        generateDiagramUsingJar("graph.puml", "/output/path")
    }

    @Test
    fun `test generateDiagramUsingJar error`() {
        mockkConstructor(ProcessBuilder::class)
        val mockProcess = mockkClass(Process::class)

        every { anyConstructed<ProcessBuilder>().start() } returns mockProcess
        every { mockProcess.inputStream } returns ByteArrayInputStream("Error".toByteArray())
        every { mockProcess.waitFor() } returns 200

        val ex = assertThrows<RuntimeException> {
            generateDiagramUsingJar("graph.puml", "/output/path")
        }
        assertTrue(ex.message!!.contains("Код ошибки: 200"))
    }

}