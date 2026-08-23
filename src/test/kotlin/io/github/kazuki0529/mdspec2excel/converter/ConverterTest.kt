package io.github.kazuki0529.mdspec2excel.converter

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Path

class ConverterTest {

    @Test
    fun `Markdownディレクトリを変換してExcelが出力される`(@TempDir tempDir: Path) {
        val specsDir = File(ConverterTest::class.java.classLoader.getResource("specs")!!.toURI())
        val templateFile = ConverterTest::class.java.classLoader.getResource("template.xlsx")!!.path
        val outFile = tempDir.resolve("out.xlsx").toFile().absolutePath

        convertMdToExcel(specsDir.absolutePath, templateFile, outFile)

        assertTrue(File(outFile).exists(), "出力Excelファイルが生成されること")
        assertTrue(File(outFile).length() > 0, "出力Excelファイルが空でないこと")
    }

    @Test
    fun `空ディレクトリを変換してもエラーにならない`(@TempDir tempDir: Path) {
        val emptyDir = tempDir.resolve("empty").toFile().also { it.mkdirs() }
        val templateFile = ConverterTest::class.java.classLoader.getResource("template.xlsx")!!.path
        val outFile = tempDir.resolve("out_empty.xlsx").toFile().absolutePath

        convertMdToExcel(emptyDir.absolutePath, templateFile, outFile)

        assertTrue(File(outFile).exists(), "空の場合でも出力Excelファイルが生成されること")
    }
}
