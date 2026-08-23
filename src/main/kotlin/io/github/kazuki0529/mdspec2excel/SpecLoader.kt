package io.github.kazuki0529.mdspec2excel

import com.vladsch.flexmark.ast.Heading
import com.vladsch.flexmark.parser.Parser
import com.vladsch.flexmark.util.ast.Document
import com.vladsch.flexmark.util.ast.Node
import com.vladsch.flexmark.util.data.MutableDataSet
import java.io.File
import java.nio.charset.StandardCharsets

/**
 * Markdown形式のテスト仕様書のローダ
 */
class SpecLoader(specFile: File) {
    var cursor: Node? = null
    private var document: Document

    var title = ""
        private set
    var mainItem = ""
        private set
    var middleItem = ""
        private set
    var smallItem = ""
        private set
    var steps = listOf<String>()
        private set
    var expected = listOf<String>()
        private set
    var notes = listOf<String>()
        private set

    var cases = mutableListOf<SpecCase>()
        private set

    init {
        val parser = Parser.builder(MutableDataSet()).build()
        this.document = parser.parse(
            specFile.readLines(StandardCharsets.UTF_8).joinToString("\n")
        )
        this.cursor = this.document.firstChild
    }

    fun hasNext(): Boolean = this.cursor?.next != null

    fun next(): Boolean {
        this.cursor = this.cursor?.next
        if (this.isNewCase() || this.cursor == null) {
            this.cases.add(
                SpecCase(
                    this.mainItem,
                    this.middleItem,
                    this.smallItem,
                    this.steps.joinToString("\n"),
                    this.expected.joinToString("\n"),
                    this.notes.joinToString("\n")
                )
            )
            this.notifyNewCase()
        }
        return this.cursor != null
    }

    private fun isNewCase(): Boolean =
        this.cursor is Heading && this.steps.isNotEmpty() && this.expected.isNotEmpty()

    private fun notifyNewCase() {
        this.mainItem = ""
        this.middleItem = ""
        this.smallItem = ""
        this.steps = listOf()
        this.expected = listOf()
        this.notes = listOf()
    }

    fun notifyTitle(value: String) { this.title = value }
    fun notifyMainItem(value: String) { this.mainItem = value }
    fun notifyMiddleItem(value: String) { this.middleItem = value }
    fun notifySmallItem(value: String) { this.smallItem = value }

    fun notifySteps(value: String) {
        var num = 1
        this.steps = value.trim().split("\n").map {
            "${num++}. " + it.replace("""^\d+\. """.toRegex(), "")
        }
    }

    fun notifyExpected(value: String) {
        this.expected = value.trim().split("\n").map {
            "・" + it.replace("""^[\*\+\-] \[ \] """.toRegex(), "")
        }
    }

    fun notifyNotes(value: String) {
        this.notes = value.split("\n").filter { !it.startsWith("```") }
    }
}
