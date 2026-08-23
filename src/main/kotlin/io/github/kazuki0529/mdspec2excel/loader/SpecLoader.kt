package io.github.kazuki0529.mdspec2excel.loader

import io.github.kazuki0529.mdspec2excel.model.Spec
import io.github.kazuki0529.mdspec2excel.model.SpecCase

import com.vladsch.flexmark.ast.BulletList
import com.vladsch.flexmark.ast.FencedCodeBlock
import com.vladsch.flexmark.ast.Heading
import com.vladsch.flexmark.ast.OrderedList
import com.vladsch.flexmark.parser.Parser
import com.vladsch.flexmark.util.ast.Node
import com.vladsch.flexmark.util.data.MutableDataSet
import java.io.File
import java.nio.charset.StandardCharsets

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
    val parser = Parser.builder(MutableDataSet()).build()
    val document = parser.parse(file.readLines(StandardCharsets.UTF_8).joinToString("\n"))

    var title = ""
    var mainItem = ""
    var middleItem = ""
    var smallItem = ""
    var steps = listOf<String>()
    var expected = listOf<String>()
    var notes = listOf<String>()
    val cases = mutableListOf<SpecCase>()

    /** 現在収集中のフィールドから [SpecCase] を生成して [cases] に追加し、フィールドをリセットする。 */
    fun flushCase() {
        if (steps.isNotEmpty() || expected.isNotEmpty()) {
            cases.add(
                SpecCase(
                    mainItem,
                    middleItem,
                    smallItem,
                    steps.joinToString("\n"),
                    expected.joinToString("\n"),
                    notes.joinToString("\n")
                )
            )
            steps = listOf()
            expected = listOf()
            notes = listOf()
        }
    }

    var cursor: Node? = document.firstChild
    while (cursor != null) {
        when (val node = cursor) {
            is Heading -> {
                // 新しい見出しが来たら前のケースを確定する
                if (steps.isNotEmpty() || expected.isNotEmpty()) flushCase()
                when (node.level) {
                    1 -> {
                        title = node.text.toString()
                        mainItem = ""
                        middleItem = ""
                        smallItem = ""
                    }
                    2 -> {
                        mainItem = node.text.toString()
                        middleItem = ""
                        smallItem = ""
                    }
                    3 -> {
                        middleItem = node.text.toString()
                        smallItem = ""
                    }
                    4 -> smallItem = node.text.toString()
                }
            }
            is OrderedList -> steps = node.chars.toString().trim().split("\n").mapIndexed { i, line ->
                "${i + 1}. " + line.replace("""^\d+\. """.toRegex(), "")
            }
            is BulletList -> expected = node.chars.toString().trim().split("\n").map { line ->
                "・" + line.replace("""^[*+\-] \[ ] """.toRegex(), "")
            }
            is FencedCodeBlock -> notes = node.chars.toString().split("\n")
                .filter { !it.startsWith("```") }
        }
        cursor = cursor.next
    }
    // 末尾に残った未確定ケースを処理する
    flushCase()

    return Spec(file.nameWithoutExtension, title, cases)
}
