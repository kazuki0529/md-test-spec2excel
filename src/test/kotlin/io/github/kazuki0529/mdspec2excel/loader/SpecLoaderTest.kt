package io.github.kazuki0529.mdspec2excel.loader

import io.github.kazuki0529.mdspec2excel.model.Spec
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Path
import java.nio.file.Paths

class SpecLoaderTest {

    private lateinit var spec: Spec

    private fun resourceFile(path: String): File {
        val resource = requireNotNull(SpecLoaderTest::class.java.classLoader.getResource(path)) {
            "Test resource not found: $path"
        }
        return Paths.get(resource.toURI()).toFile()
    }

    private fun parseMarkdown(tempDir: Path, markdown: String): Spec {
        val file = tempDir.resolve("spec.md").toFile().apply { writeText(markdown) }
        return parseSpec(file)
    }

    @BeforeEach
    fun setUp() {
        spec = parseSpec(resourceFile("specs/sample.md"))
    }

    @Nested
    inner class `見出し継承と階層リセット` {
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
        fun `同じ大項目配下の次ケースでも大項目が引き継がれる`() {
            assertEquals("大項目１", spec.cases[1].mainItem)
        }

        @Test
        fun `大項目２のケースが正しく取得できる`() {
            assertEquals("大項目２", spec.cases[2].mainItem)
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
            val parsed = parseMarkdown(tempDir, markdown)

            assertEquals(2, parsed.cases.size)
            assertEquals("大項目2", parsed.cases[1].mainItem)
            assertEquals("", parsed.cases[1].middleItem)
        }
    }

    @Nested
    inner class `リストと備考の正規化` {
        @Test
        fun `手順が連番に正規化される`() {
            val lines = spec.cases[0].steps.split("\n")
            assertEquals(listOf("1. 手順 1", "2. 手順 2"), lines)
        }

        @Test
        fun `想定動作の先頭に中点が付く`() {
            val expected = spec.cases[0].expected
            assertTrue(expected.startsWith("・"), "expected should start with '・' but was: $expected")
        }

        @Test
        fun `備考のコードフェンスが除去される`() {
            val notes = spec.cases[0].notes
            assertTrue(!notes.contains("```"), "notes should not contain code fence markers but was: $notes")
        }

        @Test
        fun `複数の箇条書き記法が想定動作として正規化される`(@TempDir tempDir: Path) {
            val markdown = """
                # タイトル

                ## 大項目
                ### 中項目
                #### 小項目
                1. 手順1
                3. 手順3

                - [ ] 想定A
                * 想定B
                + [x] 想定C
            """.trimIndent()
            val parsed = parseMarkdown(tempDir, markdown)

            assertEquals(
                listOf("1. 手順1", "2. 手順3"),
                parsed.cases[0].steps.split("\n"),
                "actual steps=${parsed.cases[0].steps}"
            )
            assertEquals(
                listOf("・想定A", "・想定B", "・想定C"),
                parsed.cases[0].expected.split("\n"),
                "actual expected=${parsed.cases[0].expected}"
            )
        }
    }

    @Nested
    inner class `カスタム変数テーブル` {
        @Test
        fun `3列テーブルから customFields を取り込む`(@TempDir tempDir: Path) {
            val markdown = """
                # タイトル
                ## 大項目
                ### 中項目
                #### 小項目
                1. 手順1
                - [ ] 想定1

                | 論理名 | 変数名 | 値 |
                |---|---|---|
                | 優先度 | priority | High |
                | テスター | tester_id | user123 |
            """.trimIndent()
            val parsed = parseMarkdown(tempDir, markdown)

            assertEquals(
                mapOf("priority" to "High", "tester_id" to "user123"),
                parsed.cases[0].customFields
            )
        }

        @Test
        fun `複数テーブルは後勝ちでマージされる`(@TempDir tempDir: Path) {
            val markdown = """
                # タイトル
                ## 大項目
                ### 中項目
                #### 小項目
                1. 手順1
                - [ ] 想定1

                | 論理名 | 変数名 | 値 |
                |---|---|---|
                | 優先度 | priority | Low |
                | テスター | tester_id | user111 |

                | 論理名 | 変数名 | 値 |
                |---|---|---|
                | 優先度 | priority | High |
                | バージョン | min_version | 1.0 |
            """.trimIndent()
            val parsed = parseMarkdown(tempDir, markdown)

            assertEquals(
                mapOf("priority" to "High", "tester_id" to "user111", "min_version" to "1.0"),
                parsed.cases[0].customFields
            )
        }
    }

    @Test
    fun `手順と想定動作がない場合はケースを生成しない`(@TempDir tempDir: Path) {
        val markdown = """
            # タイトル
            ## 大項目
            ### 中項目
            #### 小項目
        """.trimIndent()
        val parsed = parseMarkdown(tempDir, markdown)

        assertEquals("タイトル", parsed.title)
        assertTrue(parsed.cases.isEmpty(), "steps/expected がない場合はケース0件")
    }

    @Test
    fun `テーブルがないケースでは customFields は空`(@TempDir tempDir: Path) {
        val markdown = """
            # タイトル
            ## 大項目
            ### 中項目
            #### 小項目
            1. 手順1
            - [ ] 想定1
        """.trimIndent()
        val parsed = parseMarkdown(tempDir, markdown)

        assertEquals(emptyMap<String, String>(), parsed.cases[0].customFields)
    }

    @Nested
    inner class `front matter` {
        @Test
        fun `var が指定されている場合はそれが varName になる`(@TempDir tempDir: Path) {
            val markdown = """
                ---
                var: loginSpec
                ---
                # ログイン仕様書
            """.trimIndent()
            val parsed = parseMarkdown(tempDir, markdown)

            assertEquals("loginSpec", parsed.varName)
        }

        @Test
        fun `var が未指定のときはファイル名が varName になる`(@TempDir tempDir: Path) {
            val markdown = """
                # タイトル
            """.trimIndent()
            val parsed = parseMarkdown(tempDir, markdown)

            assertEquals("spec", parsed.varName)
        }

        @Test
        fun `var が空文字のときはファイル名が varName になる`(@TempDir tempDir: Path) {
            val markdown = """
                ---
                var:
                ---
                # タイトル
            """.trimIndent()
            val parsed = parseMarkdown(tempDir, markdown)

            assertEquals("spec", parsed.varName)
        }

        @Test
        fun `front matter の title は見出し1として扱われない`(@TempDir tempDir: Path) {
            val markdown = """
                ---
                var: mySpec
                title: フロントマタータイトル
                ---
                # 見出し1タイトル
            """.trimIndent()
            val parsed = parseMarkdown(tempDir, markdown)

            assertEquals("mySpec", parsed.varName)
            assertEquals("見出し1タイトル", parsed.title)
        }
    }
}
