package io.github.kazuki0529.mdspec2excel.loader

import io.github.kazuki0529.mdspec2excel.model.Spec
import io.github.kazuki0529.mdspec2excel.model.SpecCase

import com.vladsch.flexmark.ast.BulletList
import com.vladsch.flexmark.ast.FencedCodeBlock
import com.vladsch.flexmark.ast.Heading
import com.vladsch.flexmark.ast.OrderedList
import com.vladsch.flexmark.ext.yaml.front.matter.AbstractYamlFrontMatterVisitor
import com.vladsch.flexmark.ext.yaml.front.matter.YamlFrontMatterExtension
import com.vladsch.flexmark.parser.Parser
import com.vladsch.flexmark.util.ast.Node
import com.vladsch.flexmark.util.data.MutableDataSet
import java.io.File
import java.nio.charset.StandardCharsets

private data class ParseState(
    val title: String = "",
    val mainItem: String = "",
    val middleItem: String = "",
    val smallItem: String = "",
    val steps: List<String> = emptyList(),
    val expected: List<String> = emptyList(),
    val notes: List<String> = emptyList()
)

private fun ParseState.flushCaseTo(cases: MutableList<SpecCase>): ParseState {
    if (steps.isEmpty() && expected.isEmpty()) {
        return this
    }
    cases += SpecCase(
        mainItem = mainItem,
        middleItem = middleItem,
        smallItem = smallItem,
        steps = steps.joinToString("\n"),
        expected = expected.joinToString("\n"),
        notes = notes.joinToString("\n")
    )
    return copy(steps = emptyList(), expected = emptyList(), notes = emptyList())
}

private fun ParseState.withHeading(level: Int, text: String): ParseState = when (level) {
    1 -> copy(title = text, mainItem = "", middleItem = "", smallItem = "")
    2 -> copy(mainItem = text, middleItem = "", smallItem = "")
    3 -> copy(middleItem = text, smallItem = "")
    4 -> copy(smallItem = text)
    else -> this
}

private fun ParseState.withSteps(list: OrderedList): ParseState = copy(steps = steps + list.toNormalizedSteps(steps.size))

private fun ParseState.withExpected(list: BulletList): ParseState = copy(expected = expected + list.toExpectedLines())

private fun ParseState.withNotes(codeBlock: FencedCodeBlock): ParseState = copy(notes = notes + codeBlock.chars.toString().toNoteLines())

private val orderedListPrefixRegex = Regex("^\\s*\\d+\\.\\s*")
private val bulletListPrefixRegex = Regex("^\\s*[*+\\-]\\s+(?:\\[[ xX]\\]\\s+)?")

private fun Node.collectListItems(
    startIndex: Int = 0,
    transform: (text: String, index: Int) -> String?
): List<String> {
    var index = startIndex
    return generateSequence(firstChild) { it.next }
        .mapNotNull { item ->
            transform(item.chars.toString(), index)?.also { index += 1 }
        }
        .toList()
}

private fun String.toListItemSourceLines(): List<String> = lineSequence()
    .map(String::trimEnd)
    .filter(String::isNotBlank)
    .toList()

private fun List<String>.joinListItemLines(firstLine: String, continuationIndent: String): String {
    val continuationLines = drop(1).map { "$continuationIndent${it.trim()}" }
    return (listOf(firstLine) + continuationLines).joinToString("\n")
}

private fun OrderedList.toNormalizedSteps(startIndex: Int = 0): List<String> {
    return collectListItems(startIndex) { text, index -> text.toNormalizedStep(index) }
}

private fun String.toNormalizedStep(index: Int): String? {
    val lines = toListItemSourceLines()
    if (lines.isEmpty()) {
        return null
    }

    val firstLine = lines.first()
        .replace(orderedListPrefixRegex, "")
        .trim()
    if (firstLine.isEmpty()) {
        return null
    }

    return lines.joinListItemLines("${index + 1}. $firstLine", "   ")
}

private fun BulletList.toExpectedLines(): List<String> {
    return collectListItems { text, _ -> text.toExpectedLine() }
}

private fun String.toExpectedLine(): String? {
    val lines = toListItemSourceLines()
    if (lines.isEmpty()) {
        return null
    }

    val firstLine = lines.first()
        .replace(bulletListPrefixRegex, "")
        .trim()
    if (firstLine.isEmpty()) {
        return null
    }

    return lines.joinListItemLines("・$firstLine", "  ")
}

private fun String.toNoteLines(): List<String> = lineSequence()
    .filterNot { it.trimStart().startsWith("```") }
    .filter { it.isNotEmpty() }
    .toList()

/**
 * Markdown ファイル 1 つを解析して [Spec] を返す。
 *
 * flexmark-java を使用して Markdown の AST を先頭から末尾まで走査し、
 * 見出しレベルに応じてタイトル・大項目・中項目・小項目を更新しながら [SpecCase] を構築する。
 * 順序付きリストを手順、箇条書きリスト（チェックリスト）を想定動作、コードブロックを備考として扱う。
 *
 * @param file パース対象の Markdown ファイル
 * @return パース結果の [Spec]
 */
fun parseSpec(file: File): Spec {
    val options = MutableDataSet().set(Parser.EXTENSIONS, listOf(YamlFrontMatterExtension.create()))
    val parser = Parser.builder(options).build()
    val document = parser.parse(file.readLines(StandardCharsets.UTF_8).joinToString("\n"))

    // Read front matter variables
    val frontMatterVisitor = AbstractYamlFrontMatterVisitor()
    frontMatterVisitor.visit(document)
    val frontMatterVar = frontMatterVisitor.data["var"]?.firstOrNull()?.takeIf { it.isNotBlank() }

    var state = ParseState()
    val cases = mutableListOf<SpecCase>()

    var cursor: Node? = document.firstChild
    while (cursor != null) {
        state = when (val node = cursor) {
            is Heading -> {
                // 新しい見出しが来たら前のケースを確定する
                state.flushCaseTo(cases).withHeading(node.level, node.text.toString())
            }
            is OrderedList -> state.withSteps(node)
            is BulletList -> state.withExpected(node)
            is FencedCodeBlock -> state.withNotes(node)
            else -> state
        }
        cursor = cursor.next
    }
    // 末尾に残った未確定ケースを処理する
    state = state.flushCaseTo(cases)

    val fileName = file.nameWithoutExtension
    val varName = frontMatterVar ?: fileName
    return Spec(fileName, varName, state.title, cases)
}
