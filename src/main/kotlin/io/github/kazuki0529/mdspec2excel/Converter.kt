package io.github.kazuki0529.mdspec2excel

import com.vladsch.flexmark.ast.BulletList
import com.vladsch.flexmark.ast.FencedCodeBlock
import com.vladsch.flexmark.ast.Heading
import com.vladsch.flexmark.ast.OrderedList
import org.jxls.builder.xls.XlsCommentAreaBuilder
import org.jxls.common.Context
import org.jxls.util.JxlsHelper
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.file.Paths

/**
 * 指定ディレクトリの Markdown ファイルを読み込み、Excel テンプレートに出力する
 *
 * @param mdSpecDir Markdown ファイルが格納されたディレクトリのパス
 * @param template JXls 形式の Excel テンプレートファイルのパス
 * @param out 出力先 Excel ファイルのパス
 */
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
