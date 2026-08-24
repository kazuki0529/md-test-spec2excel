# テンプレートのカスタマイズ

本ツールは [JXls](https://jxls.sourceforge.net/) を使用して Excel への出力を行います。  
`template/template.xlsx` を編集することで、出力する Excel のレイアウトや書式を自由にカスタマイズできます。

## テンプレート内で使用できる変数

変数名は front matter の `var` キーで指定します。未指定の場合はファイル名（拡張子なし）が変数名になります。  
例えば front matter に `var: loginSpec` と記載した場合、テンプレート内では `${loginSpec.title}` のように参照できます。  
front matter がない場合、`mdSpec.md` というファイルであれば `${mdSpec.title}` のように参照できます。

| 変数 | 型 | 説明 |
|---|---|---|
| `${varName.title}` | `String` | Markdown の `# 見出し1` から取得したタイトル |
| `${varName.cases}` | `List<SpecCase>` | テストケースの一覧 |
| `${case.mainItem}` | `String` | 大項目（`## 見出し2`） |
| `${case.middleItem}` | `String` | 中項目（`### 見出し3`） |
| `${case.smallItem}` | `String` | 小項目（`#### 見出し4`） |
| `${case.steps}` | `String` | 確認手順（改行区切り） |
| `${case.expected}` | `String` | 想定動作（改行区切り） |
| `${case.notes}` | `String` | 備考（改行区切り） |
| `${case.customFields}` | `Map<String, String>` | カスタム変数テーブルから取得した任意フィールド |

> `varName` の部分は front matter の `var` 値（または Markdown ファイル名・拡張子なし）に置き換えてください。

## テンプレートの書き方

テンプレート Excel は JXls 形式で記述します。  
セルに式を記入し、セルコメントで JXls の制御命令を付与します。

テンプレートのイメージ（1行 = 1テストケース）:

```
| タイトル | jx:each(items="mdSpec.cases" var="case" lastCell="G2") |
|----------|---------|---------|---------|--------|---------|------|
| No  | 大項目            | 中項目             | 小項目            | 確認手順         | 想定動作          | 備考          |
|     | ${case.mainItem}  | ${case.middleItem} | ${case.smallItem} | ${case.steps}    | ${case.expected}  | ${case.notes} |
```

- **`jx:each`** でテストケースの数だけ行を繰り返します
- **`var="case"`** でループ変数名を指定します
- **`${case.mainItem}`** 等の式がセルの値として展開されます
- ファイル名（`mdSpec`）の部分は Markdown ファイル名（拡張子なし）に合わせてください

カスタム変数の参照例:

```
${case.customFields['priority']}   // High
${case.customFields['tester_id']}  // user123
```

## 参考

- [JXls 公式ドキュメント](https://jxls.sourceforge.net/)
- リポジトリ同梱のサンプルテンプレート: `template/template.xlsx`
