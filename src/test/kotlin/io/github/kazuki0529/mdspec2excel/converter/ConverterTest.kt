package io.github.kazuki0529.mdspec2excel.converter

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Path
import java.nio.file.Paths

class ConverterTest {

    private fun resourcePath(path: String): Path {
        val resource = requireNotNull(ConverterTest::class.java.classLoader.getResource(path)) {
            "Test resource not found: $path"
        }
        return Paths.get(resource.toURI())
    }

    @Test
    fun `Markdownディレクトリを変換してExcelが出力される`(@TempDir tempDir: Path) {
        val specsDir = resourcePath("specs")
        val templateFile = resourcePath("template.xlsx")
        val outFile = tempDir.resolve("out.xlsx")

        convertMdToExcel(specsDir, templateFile, outFile)

        assertTrue(outFile.toFile().exists(), "出力Excelファイルが生成されること")
        assertTrue(outFile.toFile().length() > 0, "出力Excelファイルが空でないこと")
    }

    @Test
    fun `空ディレクトリを変換してもエラーにならない`(@TempDir tempDir: Path) {
        val emptyDir = tempDir.resolve("empty").toFile().also { it.mkdirs() }.toPath()
        val templateFile = resourcePath("template.xlsx")
        val outFile = tempDir.resolve("out_empty.xlsx")

        convertMdToExcel(emptyDir, templateFile, outFile)

        assertTrue(outFile.toFile().exists(), "空の場合でも出力Excelファイルが生成されること")
    }
}
