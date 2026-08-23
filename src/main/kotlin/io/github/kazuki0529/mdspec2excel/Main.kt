package io.github.kazuki0529.mdspec2excel

import io.github.kazuki0529.mdspec2excel.converter.convertMdToExcel

fun main(args: Array<String>) {
    if (args.size != 3) {
        System.err.println("Usage: md-test-spec2excel <path-to-markdown-dir> <path-to-template-excel-file> <path-to-output-excel-file>")
        System.exit(1)
    }
    convertMdToExcel(args[0], args[1], args[2])
}
