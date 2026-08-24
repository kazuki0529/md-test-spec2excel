package io.github.kazuki0529.mdspec2excel.loader

import io.github.kazuki0529.mdspec2excel.model.Spec
import io.github.kazuki0529.mdspec2excel.model.SpecCase

import com.vladsch.flexmark.ast.BulletList
import com.vladsch.flexmark.ast.FencedCodeBlock
import com.vladsch.flexmark.ast.Heading
import com.vladsch.flexmark.ast.OrderedList
import com.vladsch.flexmark.ext.tables.TableBlock
import com.vladsch.flexmark.ext.tables.TableBody
import com.vladsch.flexmark.ext.tables.TableCell
import com.vladsch.flexmark.ext.tables.TableRow
import com.vladsch.flexmark.ext.tables.TablesExtension
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
    val notes: List<String> = emptyList(),
    val customFields: Map<String, String> = emptyMap()
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
        notes = notes.joinToString("\n"),
        customFields = customFields
    )
    return copy(steps = emptyList(), expected = emptyList(), notes = emptyList(), customFields = emptyMap())
}

private fun ParseState.withHeading(level: Int, text: String): ParseState = when (level) {
    1 -> copy(title = text, mainItem = "", middleItem = "", smallItem = "", customFields = emptyMap())
    2 -> copy(mainItem = text, middleItem = "", smallItem = "", customFields = emptyMap())
    3 -> copy(middleItem = text, smallItem = "", customFields = emptyMap())
    4 -> copy(smallItem = text, customFields = emptyMap())
    else -> this
}

private fun ParseState.withSteps(list: OrderedList): ParseState = copy(steps = steps + list.chars.toString().toNormalizedSteps(steps.size))

private fun ParseState.withExpected(list: BulletList): ParseState = copy(expected = expected + list.chars.toString().toExpectedLines())

private fun ParseState.withNotes(codeBlock: FencedCodeBlock): ParseState = copy(notes = notes + codeBlock.chars.toString().toNoteLines())

private fun ParseState.withCustomFields(table: TableBlock): ParseState = copy(customFields = customFields + table.toCustomFieldMap())

private fun String.toTrimmedNonBlankLines(): List<String> = trim()
    .lineSequence()
    .map(String::trim)
    .filter(String::isNotEmpty)
    .toList()

private fun String.toNormalizedSteps(startIndex: Int = 0): List<String> = toTrimmedNonBlankLines()
    .mapIndexed { index, line -> "${startIndex + index + 1}. ${line.replace(Regex("^\\d+\\.\\s*"), "")}" }

private fun String.toExpectedLines(): List<String> = toTrimmedNonBlankLines()
    .map { line -> "・${line.replace(Regex("^[*+\\-]\\s+(?:\\[[ xX]\\]\\s+)?"), "")}" }

private fun String.toNoteLines(): List<String> = lineSequence()
    .filterNot { it.trimStart().startsWith("```") }
    .filter { it.isNotEmpty() }
    .toList()

private val validVarNamePattern = Regex("^[A-Za-z_$][A-Za-z0-9_$]*$")

private fun TableBlock.toCustomFieldMap(): Map<String, String> {
    val result = linkedMapOf<String, String>()
    var section: Node? = firstChild
    while (section != null) {
        if (section is TableBody) {
            var row: Node? = section.firstChild
            while (row != null) {
                if (row is TableRow) {
                    val values = mutableListOf<String>()
                    var cell: Node? = row.firstChild
                    while (cell != null) {
                        if (cell is TableCell) {
                            values += cell.text.toString().trim()
                        }
                        cell = cell.next
                    }
                    if (values.size >= 3) {
                        val key = values[1]
                        if (validVarNamePattern.matches(key)) {
                            result[key] = values[2]
                        }
                    }
                }
                row = row.next
            }
        }
        section = section.next
    }
    return result
}

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
    val options = MutableDataSet().set(
        Parser.EXTENSIONS,
        listOf(
            YamlFrontMatterExtension.create(),
            TablesExtension.create()
        )
    )
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
            is TableBlock -> state.withCustomFields(node)
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
