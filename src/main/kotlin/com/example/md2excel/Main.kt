package com.example.md2excel

import com.vladsch.flexmark.ast.BulletList
import com.vladsch.flexmark.ast.FencedCodeBlock
import com.vladsch.flexmark.ast.Heading
import com.vladsch.flexmark.ast.OrderedList
import com.vladsch.flexmark.parser.Parser
import com.vladsch.flexmark.util.ast.Document
import com.vladsch.flexmark.util.ast.Node
import com.vladsch.flexmark.util.data.MutableDataSet
import org.jxls.builder.xls.XlsCommentAreaBuilder
import org.jxls.command.AbstractCommand
import org.jxls.common.CellRef
import org.jxls.common.Context
import org.jxls.common.Size
import org.jxls.transform.poi.PoiTransformer
import org.jxls.util.JxlsHelper
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.charset.StandardCharsets
import java.nio.file.Paths

/**
 * 自動高さと自動幅の設定を行う Jxls コマンド
 */
class AutoRowHeightCommand : AbstractCommand() {
    override fun getName(): String = "autoRowHeight"

    override fun applyAt(cellRef: CellRef?, context: Context?): Size {
        val area = this.areaList[0]
        val size = area.applyAt(cellRef, context)
        if (cellRef != null) {
            val transformer = area.transformer as PoiTransformer
            transformer.workbook.getSheet(cellRef.sheetName).getRow(cellRef.row).apply {
                height = -1
            }
        }
        return size
    }
}

/**
 * テスト仕様エンティティ
 */
data class Spec(
    val fileName: String,
    val title: String,
    val cases: List<SpecCase>
)

/**
 * テストケースエンティティ
 */
data class SpecCase(
    val mainItem: String,
    val middleItem: String,
    val smallItem: String,
    val steps: String,
    val expected: String,
    val notes: String
)

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

fun convertMdToExcel(mdSpecDir: String, template: String, out: String) {
    println("######### Start #########")

    val list: List<String> = File(mdSpecDir).list()?.filter { it.lowercase().endsWith(".md") } ?: listOf()
    val specList = mutableListOf<Spec>()

    list.forEach {
        println("---------- $it ----------")
        val file = Paths.get(mdSpecDir).resolve(it).toFile()
        val loader = SpecLoader(file)

        do {
            when (val current = loader.cursor) {
                is Heading -> {
                    when (current.level) {
                        1 -> loader.notifyTitle(current.text.toString())
                        2 -> loader.notifyMainItem(current.text.toString())
                        3 -> loader.notifyMiddleItem(current.text.toString())
                        4 -> loader.notifySmallItem(current.text.toString())
                    }
                }
                is OrderedList -> loader.notifySteps(current.chars.toString())
                is BulletList -> loader.notifyExpected(current.chars.toString())
                is FencedCodeBlock -> loader.notifyNotes(current.chars.toString())
            }
        } while (loader.next())

        val spec = Spec(file.nameWithoutExtension, loader.title, loader.cases)
        specList.add(spec)
        println(spec)
    }

    val context = Context().apply {
        specList.forEach { putVar(it.fileName, it) }
    }
    XlsCommentAreaBuilder.addCommandMapping("autoRowHeight", AutoRowHeightCommand::class.java)
    FileInputStream(template).use { input ->
        FileOutputStream(out).use { output ->
            JxlsHelper.getInstance().processTemplate(input, output, context)
        }
    }

    println("######### Finished #########")
}

fun main(args: Array<String>) {
    if (args.size != 3) {
        System.err.println("Usage: md-test-spec2excel <path-to-markdown-dir> <path-to-template-excel-file> <path-to-output-excel-file>")
        System.exit(1)
    }
    convertMdToExcel(args[0], args[1], args[2])
}
