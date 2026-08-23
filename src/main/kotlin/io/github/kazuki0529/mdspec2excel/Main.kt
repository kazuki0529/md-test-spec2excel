package io.github.kazuki0529.mdspec2excel

import io.github.kazuki0529.mdspec2excel.converter.convertMdToExcel

/**
 * アプリケーションのエントリーポイント。
 *
 * コマンドライン引数として以下の3つを受け取り、Markdown → Excel 変換を実行する。
 *
 * @param args コマンドライン引数
 *   - `args[0]`: Markdown ファイルが格納されたディレクトリのパス
 *   - `args[1]`: JXls 形式の Excel テンプレートファイルのパス
 *   - `args[2]`: 出力先 Excel ファイルのパス
 */
fun main(args: Array<String>) {
    if (args.size != 3) {
        System.err.println("Usage: md-test-spec2excel <path-to-markdown-dir> <path-to-template-excel-file> <path-to-output-excel-file>")
        System.exit(1)
    }
    convertMdToExcel(args[0], args[1], args[2])
}
