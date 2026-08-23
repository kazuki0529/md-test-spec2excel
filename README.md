# MdTestSpec2Excel

![CI](https://github.com/kazuki0529/md-test-spec2excel/actions/workflows/ci.yml/badge.svg)

## 概要

Markdown形式で記載したテスト仕様書を、Excelに変換するツール。  
MarkdownファイルごとにExcelを出力するのではなく、  
[JXls](https://jxls.sourceforge.net/) 形式のExcelテンプレートに埋め込む形式。

## 実行方法

### Fat jar

```bash
# ビルド（Java 8 以上が必要）
./gradlew shadowJar

# 実行
java -jar build/libs/md-test-spec2excel-1.0.0.jar \
  <path-to-markdown-dir> \
  <path-to-template-excel-file> \
  <path-to-output-excel-file>

# 例
java -jar build/libs/md-test-spec2excel-1.0.0.jar \
  example template/template.xlsx dist/out.xlsx
```

### Docker

```bash
# イメージのビルド
docker build -f docker/Dockerfile -t md-test-spec2excel .

# 実行
docker run --rm \
  -v $(pwd):/workspaces md-test-spec2excel \
    /workspaces/example /workspaces/template/template.xlsx /workspaces/dist/out.xlsx
```

## Markdown ファイルの仕様

本ツールが読み込む Markdown ファイルの構造は以下の通りです。

### 全体構造

| Markdown 要素 | 対応するフィールド | 説明 |
|---|---|---|
| `# 見出し 1` | タイトル (`title`) | テスト仕様書全体のタイトル |
| `## 見出し 2` | 大項目 (`mainItem`) | テストケースの大分類 |
| `### 見出し 3` | 中項目 (`middleItem`) | テストケースの中分類 |
| `#### 見出し 4` | 小項目 (`smallItem`) | テストケースの小分類 |
| 順序付きリスト | 確認手順 (`steps`) | 実行する操作手順 |
| チェックリスト（箇条書き） | 想定動作 (`expected`) | 期待される動作・結果 |
| コードブロック | 備考 (`notes`) | 補足情報・注意事項 |

### テストケースの区切り

1つのテストケースは **確認手順（順序付きリスト）** と **想定動作（チェックリスト）** を
1セットとして構成されます。次の見出し（`##`〜`####`）が現れた時点で、それまでの手順・想定動作・備考をまとめた
テストケースが確定します。

### ファイルの記載例

```markdown
# テストケース名

## 大項目１

### 中項目１

#### 小項目１

1. 確認手順 1
1. 確認手順 2

- [ ] 想定動作 1
- [ ] 想定動作 2

\`\`\`text
備考１
備考２
\`\`\`

### 中項目２

#### 小項目１

1. 確認手順 A
1. 確認手順 B

- [ ] 想定動作 A
- [ ] 想定動作 B
```

### 注意事項

- ファイル名（拡張子なし）が JXls テンプレート内の変数名として使用されます。
  テンプレートと一致するファイル名を使用してください。
- 確認手順は **順序付きリスト**（`1. テキスト` 形式）で記載してください。
- 想定動作は **チェックリスト形式の箇条書き**（`- [ ] テキスト` 形式）で記載してください。
- 備考は **コードブロック**（`` ``` `` で囲まれた範囲）で記載してください。言語指定は任意です。
- 備考が不要なテストケースはコードブロックを省略できます。

