package io.github.kazuki0529.mdspec2excel.model

/**
 * テスト仕様エンティティ
 */
data class Spec(
    val fileName: String,
    val title: String,
    val cases: List<SpecCase>
)

/**
 * テストケースエンティティ
 */
data class SpecCase(
    val mainItem: String,
    val middleItem: String,
    val smallItem: String,
    val steps: String,
    val expected: String,
    val notes: String
)
