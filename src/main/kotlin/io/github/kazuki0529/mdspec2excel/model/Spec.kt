package io.github.kazuki0529.mdspec2excel.model

/**
 * Markdown ファイル 1 つ分のテスト仕様を表すエンティティ。
 *
 * @property fileName ファイル名（拡張子なし）。
 * @property varName JXls テンプレート内での変数名。front matter の `spec.var` キーが優先され、未指定の場合は [fileName] が使用される。
 * @property title Markdown の `# 見出し 1` から取得したテスト仕様のタイトル。
 * @property cases このファイルに含まれるテストケースの一覧。
 * @property vars front matter の `vars` で定義されたユーザ定義変数。
 */
data class Spec(
    val fileName: String,
    val varName: String,
    val title: String,
    val cases: List<SpecCase>,
    val vars: Map<String, String> = emptyMap()
)

/**
 * テストケース 1 件分の情報を表すエンティティ。
 *
 * @property mainItem `## 見出し 2` から取得した大項目名。
 * @property middleItem `### 見出し 3` から取得した中項目名。
 * @property smallItem `#### 見出し 4` から取得した小項目名。
 * @property steps 順序付きリストから取得した確認手順（改行区切りの文字列）。
 * @property expected 箇条書きリストから取得した想定動作（改行区切りの文字列）。
 * @property notes コードブロックから取得した備考（改行区切りの文字列）。
 * @property customFields カスタム変数テーブル（`| 論理名 | 変数名 | 値 |`）から取得した任意フィールド。
 */
data class SpecCase(
    val mainItem: String,
    val middleItem: String,
    val smallItem: String,
    val steps: String,
    val expected: String,
    val notes: String,
    val customFields: Map<String, String> = emptyMap()
)
