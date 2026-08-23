package io.github.kazuki0529.mdspec2excel.converter

import io.github.kazuki0529.mdspec2excel.excel.AutoRowHeightCommand
import io.github.kazuki0529.mdspec2excel.loader.parseSpec
import org.jxls.builder.xls.XlsCommentAreaBuilder
import org.jxls.common.Context
import org.jxls.util.JxlsHelper
import org.slf4j.LoggerFactory
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.stream.Collectors

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
    convertMdToExcel(Paths.get(mdSpecDir), Paths.get(template), Paths.get(out))
}

/**
 * 指定ディレクトリ内の Markdown ファイルを読み込み、JXls 形式の Excel テンプレートへ出力する。
 *
 * @param mdSpecDir Markdown ファイルが格納されたディレクトリ
 * @param template JXls 形式の Excel テンプレートファイル
 * @param out 出力先 Excel ファイル
 */
fun convertMdToExcel(mdSpecDir: Path, template: Path, out: Path) {
    logger.info("Start")

    val context = Context()
    Files.list(mdSpecDir).use { entries ->
        entries
            .filter { Files.isRegularFile(it) && it.fileName.toString().lowercase().endsWith(".md") }
            .sorted()
            .collect(Collectors.toList())
            .asSequence()
            .map { path ->
                logger.info("Processing: ${path.fileName}")
                parseSpec(path.toFile()).also { logger.debug("{}", it) }
            }
            .forEach { context.putVar(it.fileName, it) }
    }

    XlsCommentAreaBuilder.addCommandMapping("autoRowHeight", AutoRowHeightCommand::class.java)
    Files.newInputStream(template).use { input ->
        Files.newOutputStream(out).use { output ->
            JxlsHelper.getInstance().processTemplate(input, output, context)
        }
    }

    logger.info("Finished")
}
