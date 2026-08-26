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
import com.vladsch.flexmark.ext.yaml.front.matter.YamlFrontMatterExtension
import com.vladsch.flexmark.parser.Parser
import com.vladsch.flexmark.util.ast.Node
import com.vladsch.flexmark.util.data.MutableDataSet
import java.io.File
import java.nio.charset.StandardCharsets

private const val HEADING_LEVEL_TITLE = 1
private const val HEADING_LEVEL_MAIN_ITEM = 2
private const val HEADING_LEVEL_MIDDLE_ITEM = 3
private const val HEADING_LEVEL_SMALL_ITEM = 4

private const val CUSTOM_FIELD_MIN_COLUMNS = 3
private const val CUSTOM_FIELD_KEY_COLUMN_INDEX = 1
private const val CUSTOM_FIELD_VALUE_COLUMN_INDEX = 2

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
    HEADING_LEVEL_TITLE -> copy(title = text, mainItem = "", middleItem = "", smallItem = "", customFields = emptyMap())
    HEADING_LEVEL_MAIN_ITEM -> copy(mainItem = text, middleItem = "", smallItem = "", customFields = emptyMap())
    HEADING_LEVEL_MIDDLE_ITEM -> copy(middleItem = text, smallItem = "", customFields = emptyMap())
    HEADING_LEVEL_SMALL_ITEM -> copy(smallItem = text, customFields = emptyMap())
    else -> this
}

private fun ParseState.withSteps(list: OrderedList): ParseState = copy(steps = steps + list.toNormalizedSteps(steps.size))

private fun ParseState.withExpected(list: BulletList): ParseState = copy(expected = expected + list.toExpectedLines())

private fun ParseState.withNotes(codeBlock: FencedCodeBlock): ParseState = copy(notes = notes + codeBlock.chars.toString().toNoteLines())
private fun ParseState.withCustomFields(table: TableBlock): ParseState = copy(customFields = customFields + table.toCustomFieldMap())

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

private val validVarNamePattern = Regex("""^[A-Za-z_$][A-Za-z0-9_$]*$""")

/**
 * TableBlock から customFields を抽出する。
 * 3列（論理名 / 変数名 / 値）を前提とし、同一キーは後勝ちで上書きする。
 */
private fun TableBlock.toCustomFieldMap(): Map<String, String> = firstChild
    .selfAndFollowingSiblings()
    .filterIsInstance<TableBody>()
    .flatMap { it.firstChild.selfAndFollowingSiblings().filterIsInstance<TableRow>() }
    .mapNotNull(TableRow::toCustomFieldEntryOrNull)
    .fold(linkedMapOf()) { acc, (key, value) ->
        acc[key] = value
        acc
    }

private fun TableRow.toCustomFieldEntryOrNull(): Pair<String, String>? {
    val values = firstChild
        .selfAndFollowingSiblings()
        .filterIsInstance<TableCell>()
        .map { it.text.toString().trim() }
        .toList()

    if (values.size < CUSTOM_FIELD_MIN_COLUMNS) {
        return null
    }

    val key = values[CUSTOM_FIELD_KEY_COLUMN_INDEX]
    if (!validVarNamePattern.matches(key)) {
        return null
    }
    return key to values[CUSTOM_FIELD_VALUE_COLUMN_INDEX]
}

private fun Node?.selfAndFollowingSiblings(): Sequence<Node> = sequence {
    var cursor = this@selfAndFollowingSiblings
    while (cursor != null) {
        yield(cursor)
        cursor = cursor.next
    }
}

/**
 * Markdown ファイルの先頭にある YAML front matter を解析し、
 * トップレベルキーとその値（スカラー文字列 または Map<String, String>）を返す。
 *
 * flexmark の AbstractYamlFrontMatterVisitor はネスト構造を保持しないため、
 * ファイルテキストから直接解析する。2 レベルまでの YAML に対応する。
 *
 * 例:
 * ```
 * spec:
 *   var: mdSpec
 * vars:
 *   feature: ログイン
 * ```
 * → `mapOf("spec" to mapOf("var" to "mdSpec"), "vars" to mapOf("feature" to "ログイン"))`
 *
 * @param lines Markdown ファイルの全行
 * @return トップレベルキーと値のマップ。front matter がない場合は空マップ。
 */
private fun parseFrontMatter(lines: List<String>): Map<String, Any> {
    if (lines.isEmpty() || lines[0].trim() != "---") return emptyMap()

    // 2 番目の "---" を探してフロントマター終端を特定する
    val endIdx = lines.drop(1).indexOfFirst { it.trim() == "---" }
    if (endIdx < 0) return emptyMap()

    val result = mutableMapOf<String, Any>()
    var currentBlockKey: String? = null
    var currentBlock: MutableMap<String, String>? = null

    for (line in lines.drop(1).take(endIdx)) {
        if (line.isBlank()) continue

        // インデントされた行はネストされたキーとして扱う
        val isIndented = line.isNotEmpty() && line[0].isWhitespace()
        if (isIndented) {
            val colonIdx = line.indexOf(':')
            if (colonIdx < 0 || currentBlock == null) continue
            val key = line.substring(0, colonIdx).trim()
            val value = line.substring(colonIdx + 1).trim()
            if (key.isNotBlank()) currentBlock[key] = value
        } else {
            // 前のブロックを確定する
            currentBlockKey?.let { k -> currentBlock?.let { result[k] = it } }
            currentBlock = null
            currentBlockKey = null

            val colonIdx = line.indexOf(':')
            if (colonIdx < 0) continue
            val key = line.substring(0, colonIdx).trim()
            val value = line.substring(colonIdx + 1).trim()
            if (value.isNotBlank()) {
                // スカラー値（例: `title: foo`）
                result[key] = value
            } else {
                // マップブロック（例: `spec:` や `vars:`）
                currentBlockKey = key
                currentBlock = mutableMapOf()
            }
        }
    }
    // 最後のブロックを確定する
    currentBlockKey?.let { k -> currentBlock?.let { result[k] = it } }

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
    // ファイルを一度だけ読み込み、AST パースとフロントマター解析の両方に使う
    val fileLines = file.readLines(StandardCharsets.UTF_8)
    val document = parser.parse(fileLines.joinToString("\n"))

    // フロントマターを直接ファイルテキストから解析する
    val frontMatter = parseFrontMatter(fileLines)

    @Suppress("UNCHECKED_CAST")
    val specBlock = frontMatter["spec"] as? Map<String, String> ?: emptyMap()
    val frontMatterVar = specBlock["var"]?.takeIf { it.isNotBlank() }

    @Suppress("UNCHECKED_CAST")
    val frontMatterVars: Map<String, String> = frontMatter["vars"] as? Map<String, String> ?: emptyMap()

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
    return Spec(fileName, varName, state.title, cases, frontMatterVars)
}
