package io.github.kazuki0529.mdspec2excel.converter

import io.github.kazuki0529.mdspec2excel.excel.AutoRowHeightCommand
import io.github.kazuki0529.mdspec2excel.loader.parseSpec
import org.jxls.builder.xls.XlsCommentAreaBuilder
import org.jxls.common.Context
import org.jxls.util.JxlsHelper
import org.slf4j.LoggerFactory
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.file.Paths
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.name

private val logger = LoggerFactory.getLogger("Converter")

/**
 * 指定ディレクトリ内の Markdown ファイルをすべて読み込み、JXls 形式の Excel テンプレートへ出力する。
 *
 * ディレクトリ内の `.md` ファイルを列挙し、[parseSpec] でパースした結果を
 * JXls の [Context] に登録したうえでテンプレートを処理する。
 * 各ファイルの変数名にはファイル名（拡張子なし）が使用される。
 *
 * @param mdSpecDir Markdown ファイルが格納されたディレクトリのパス
 * @param template JXls 形式の Excel テンプレートファイルのパス
 * @param out 出力先 Excel ファイルのパス
 */
fun convertMdToExcel(mdSpecDir: String, template: String, out: String) {
    logger.info("Start")

    val mdDir = Paths.get(mdSpecDir)
    val specList = mdDir.listDirectoryEntries()
        .filter { it.name.lowercase().endsWith(".md") }
        .map { path ->
            logger.info("Processing: ${path.name}")
            val spec = parseSpec(path.toFile())
            logger.debug("{}", spec)
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

    logger.info("Finished")
}
