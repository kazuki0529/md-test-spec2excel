package io.github.kazuki0529.mdspec2excel.loader

import io.github.kazuki0529.mdspec2excel.model.Spec
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Path

class SpecLoaderTest {

    private lateinit var spec: Spec

    @BeforeEach
    fun setUp() {
        val file = File(SpecLoaderTest::class.java.classLoader.getResource("specs/sample.md")!!.toURI())
        spec = parseSpec(file)
    }

    @Test
    fun `タイトルが正しく読み込まれる`() {
        assertEquals("テストケース名", spec.title)
    }

    @Test
    fun `テストケース数が正しい`() {
        assertEquals(3, spec.cases.size)
    }

    @Test
    fun `最初のケースの大項目が正しい`() {
        assertEquals("大項目１", spec.cases[0].mainItem)
    }

    @Test
    fun `最初のケースの中項目が正しい`() {
        assertEquals("中項目１", spec.cases[0].middleItem)
    }

    @Test
    fun `最初のケースの小項目が正しい`() {
        assertEquals("小項目１", spec.cases[0].smallItem)
    }

    @Test
    fun `手順が連番に正規化される`() {
        val steps = spec.cases[0].steps
        assertTrue(steps.startsWith("1. "), "steps should start with '1. ' but was: $steps")
        assertTrue(steps.contains("2. "), "steps should contain '2. ' but was: $steps")
    }

    @Test
    fun `想定動作の先頭に中点が付く`() {
        val expected = spec.cases[0].expected
        assertTrue(expected.startsWith("・"), "expected should start with '・' but was: $expected")
    }

    @Test
    fun `備考のコードフェンスが除去される`() {
        val notes = spec.cases[0].notes
        assertTrue(!notes.contains("```"), "notes should not contain backticks but was: $notes")
    }

    @Test
    fun `大項目２のケースが正しく取得できる`() {
        assertEquals("大項目２", spec.cases[2].mainItem)
    }

    @Test
    fun `同じ大項目配下の次ケースでも大項目が引き継がれる`() {
        assertEquals("大項目１", spec.cases[1].mainItem)
    }

    @Test
    fun `大項目が切り替わった後は中項目がリセットされる`(@TempDir tempDir: Path) {
        val markdown = """
            # タイトル

            ## 大項目1
            ### 中項目1
            #### 小項目1
            1. 手順1
            - [ ] 想定1

            ## 大項目2
            #### 小項目2
            1. 手順2
            - [ ] 想定2
        """.trimIndent()
        val file = tempDir.resolve("spec.md").toFile().apply { writeText(markdown) }

        val parsed = parseSpec(file)

        assertEquals(2, parsed.cases.size)
        assertEquals("大項目2", parsed.cases[1].mainItem)
        assertEquals("", parsed.cases[1].middleItem)
    }
}
