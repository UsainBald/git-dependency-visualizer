import java.io.File
import java.io.FileInputStream
import java.nio.file.Files
import java.nio.file.Paths
import java.util.zip.Inflater
import java.util.zip.InflaterInputStream
import org.yaml.snakeyaml.Yaml
import java.text.SimpleDateFormat
import java.util.zip.ZipException

// Структура для представления узла графа зависимостей Git-объекта
data class GitObjectNode(
  var label: String,
  val children: MutableList<GitObjectNode> = mutableListOf()
)

// Глобальные переменные для конфигурации
var plantUmlPath: String = "/Users/gerkulesov35/IdeaProjects/mirea-config/git-dependency-visualizer/plantuml-1.2024.7.jar"
var repositoryPath: String = "/Users/gerkulesov35/IdeaProjects/mirea-config/kotlin-shell"
lateinit var startDate: String
lateinit var branch: String

fun main() {
  // Загружаем конфигурацию из YAML
  loadConfig("/Users/gerkulesov35/IdeaProjects/mirea-config/git-dependency-visualizer/src/main/resources/config.yaml")

  // Генерируем файл с PlantUML-графом зависимостей
  generatePlantUML("graph.puml")

  generateDiagramUsingJar("graph.puml", "/Users/gerkulesov35/IdeaProjects/mirea-config/git-dependency-visualizer/")
}

fun generateDiagramUsingJar(umlFilePath: String, outputFilePath: String) {
  val command = listOf("java", "-jar", plantUmlPath, umlFilePath, "-o", File(outputFilePath).parent)
  val process = ProcessBuilder(command)
    .redirectErrorStream(true)
    .start()

  process.inputStream.bufferedReader().use { reader ->
    println(reader.readText())
  }

  val exitCode = process.waitFor()
  if (exitCode != 0) {
    throw RuntimeException("Ошибка при выполнении команды PlantUML. Код ошибки: $exitCode")
  }

  println("UML-диаграмма успешно сгенерирована: $outputFilePath")
}

/**
 * Загрузка конфигурации из YAML-файла.
 */
fun loadConfig(filename: String) {
  val yaml = Yaml()
  FileInputStream(filename).use { input ->
    @Suppress("UNCHECKED_CAST")
    val config = yaml.load<Map<String, Any>>(input)
    plantUmlPath = config["plant_uml_path"] as String
    repositoryPath = config["repository_path"] as String

    // Проверяем, является ли start_date объектом Date
    val startDateValue = config["start_date"]
    startDate = when (startDateValue) {
      is String -> startDateValue
      is java.util.Date -> SimpleDateFormat("yyyy-MM-dd").format(startDateValue)
      else -> throw IllegalArgumentException("Invalid type for start_date: ${startDateValue?.javaClass?.name}")
    }

    branch = config["branch"] as String
  }
}

/**
 * Парсинг git-объекта по его хэшу.
 * Декодируем, разжимаем, анализируем заголовок и содержимое.
 */
fun parseObject(objectHash: String, description: String? = null): GitObjectNode {
  val objectPath = Paths.get(repositoryPath, ".git", "objects", objectHash.substring(0, 2), objectHash.substring(2)).toFile()
  val rawObjectContent = decompressObject(objectPath)

  val splitIndex = rawObjectContent.indexOf(0.toByte())
  val headerBytes = rawObjectContent.sliceArray(0 until splitIndex)
  val bodyBytes = rawObjectContent.sliceArray((splitIndex + 1) until rawObjectContent.size)

  val header = String(headerBytes, Charsets.UTF_8)
  val (objectType, _) = header.split(" ", limit = 2)

  val node = GitObjectNode(label = "[$objectType]\n${objectHash.take(6)}")

  when (objectType) {
    "commit" -> {
      node.children.addAll(parseCommit(bodyBytes))
    }
    "tree" -> {
      node.children.addAll(parseTree(bodyBytes))
    }
    "blob" -> {
      // blob не имеет детей
    }
  }

  if (description != null) {
    node.label += "\\n$description"
  }

  return node
}

/**
 * Парсим объект типа tree, извлекая список вложенных объектов.
 */
fun parseTree(rawContent: ByteArray): List<GitObjectNode> {
  val children = mutableListOf<GitObjectNode>()
  var rest = rawContent
  while (rest.isNotEmpty()) {
    val spaceIndex = rest.indexOf(' '.toByte())
    val modeBytes = rest.sliceArray(0 until spaceIndex)

    val afterMode = rest.sliceArray((spaceIndex + 1) until rest.size)
    val nullIndex = afterMode.indexOf(0.toByte())
    val nameBytes = afterMode.sliceArray(0 until nullIndex)

    val remaining = afterMode.sliceArray((nullIndex + 1) until afterMode.size)
    val sha1Bytes = remaining.sliceArray(0 until 20)
    val sha1Hex = sha1Bytes.joinToString("") { "%02x".format(it) }

    rest = remaining.sliceArray(20 until remaining.size)

    val name = String(nameBytes, Charsets.UTF_8)
    children.add(parseObject(sha1Hex, description = name))
  }
  return children
}

/**
 * Парсим объект коммита. Извлекаем tree, parent-ы, автора, коммитера и сообщение.
 */
fun parseCommit(rawContent: ByteArray): List<GitObjectNode> {
  val content = String(rawContent, Charsets.UTF_8)
  val lines = content.split("\n")

  val commitData = mutableMapOf<String, Any>()
  var index = 0

  // Первая строка - tree
  val treeLine = lines[index++].split(" ")
  commitData["tree"] = treeLine[1]

  // Родители
  val parents = mutableListOf<String>()
  while (index < lines.size && lines[index].startsWith("parent")) {
    val p = lines[index++].split(" ")
    parents.add(p[1])
  }
  commitData["parents"] = parents

  // Остальные метаданные до пустой строки
  while (index < lines.size && lines[index].isNotBlank()) {
    val parts = lines[index].split(" ", limit = 2)
    val key = parts[0]
    val value = if (parts.size > 1) parts[1] else ""
    commitData[key] = value
    index++
  }

  // Сообщение коммита
  val message = lines.drop(index + 1).joinToString("\n").trim()
  commitData["message"] = message

  val treeNode = parseObject(commitData["tree"] as String)
  val parentNodes = (commitData["parents"] as List<*>).map { parseObject(it as String) }

  return listOf(treeNode) + parentNodes
}

/**
 * Получить хэш последнего коммита в текущей ветке.
 */
fun getLastCommit(): String {
  val headPath = Paths.get(repositoryPath, ".git", "refs", "heads", branch)
  return Files.readAllLines(headPath)[0].trim()
}

/**
 * Создать PlantUML-файл для графа зависимостей.
 */
fun generatePlantUML(filename: String) {
  val lastCommit = getLastCommit()
  val tree = parseObject(lastCommit)

  val result = mutableListOf<String>()
  result.add("@startuml")
  result.add("skinparam defaultFontName Courier")

  fun formatLabel(label: String): String {
    // Заменяем переносы строк на '\\n' для корректного отображения
    return label.replace("\n", "\\n")
  }

  fun recursiveWrite(node: GitObjectNode) {
    val formattedLabel = formatLabel(node.label)
    for (child in node.children) {
      val childLabel = formatLabel(child.label)
      result.add("\"$formattedLabel\" --> \"$childLabel\"")
      recursiveWrite(child)
    }
  }

  recursiveWrite(tree)
  result.add("@enduml")

  File(filename).writeText(result.joinToString("\n"))
}

/**
 * Декомпрессия git-объекта из файла.
 */
fun decompressObject(file: File): ByteArray {
  if (!file.exists()) {
    throw IllegalArgumentException("Файл не найден: ${file.absolutePath}")
  }

  return try {
    FileInputStream(file).use { fis ->
      InflaterInputStream(fis, Inflater(false)).use { iis ->
        iis.readBytes()
      }
    }
  } catch (e: ZipException) {
    throw IllegalArgumentException("Ошибка декомпрессии файла: ${file.absolutePath}", e)
  }
}