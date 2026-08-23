package io.github.kazuki0529.mdspec2excel.converter

import io.github.kazuki0529.mdspec2excel.excel.AutoRowHeightCommand
import io.github.kazuki0529.mdspec2excel.loader.SpecLoader
import org.jxls.builder.xls.XlsCommentAreaBuilder
import org.jxls.common.Context
import org.jxls.util.JxlsHelper
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.file.Paths

/**
 * 指定ディレクトリ内の Markdown ファイルをすべて読み込み、JXls 形式の Excel テンプレートへ出力する。
 *
 * ディレクトリ内の `.md` ファイルを列挙し、[SpecLoader.parse] でパースした結果を
 * JXls の [Context] に登録したうえでテンプレートを処理する。
 * 各ファイルの変数名にはファイル名（拡張子なし）が使用される。
 *
 * @param mdSpecDir Markdown ファイルが格納されたディレクトリのパス
 * @param template JXls 形式の Excel テンプレートファイルのパス
 * @param out 出力先 Excel ファイルのパス
 */
fun convertMdToExcel(mdSpecDir: String, template: String, out: String) {
    println("######### Start #########")

    val mdDir = Paths.get(mdSpecDir)
    val specList = (File(mdSpecDir).list()?.filter { it.lowercase().endsWith(".md") } ?: emptyList())
        .map { fileName ->
            println("---------- $fileName ----------")
            val spec = SpecLoader.parse(mdDir.resolve(fileName).toFile())
            println(spec)
            spec
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
