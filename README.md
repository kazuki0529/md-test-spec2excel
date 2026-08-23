# MdTestSpec2Excel

![CI](https://github.com/kazuki0529/md-test-spec2excel/actions/workflows/ci.yml/badge.svg)

## 概要

Markdown形式で記載したテスト仕様書を、Excelに変換するツール。  
MarkdownファイルごとにExcelを出力するのではなく、  
[JXls](https://jxls.sourceforge.net/) 形式のExcelテンプレートに埋め込む形式。

## クイックスタート

1. **Java 8 以上** がインストールされていることを確認する
2. リポジトリを clone する

   ```bash
   git clone https://github.com/kazuki0529/md-test-spec2excel.git
   cd md-test-spec2excel
   ```

3. Fat Jar をビルドする

   ```bash
   ./gradlew shadowJar
   ```

4. サンプルを実行して動作確認する

   ```bash
   mkdir -p dist
   java -jar build/libs/md-test-spec2excel-1.0.0.jar \
     example template/template.xlsx dist/out.xlsx
   ```

5. `dist/out.xlsx` が生成されていれば成功

## 実行方法

### 引数

```
java -jar md-test-spec2excel-1.0.0.jar <mdSpecDir> <template> <output>
```

| 引数 | 説明 | 例 |
|---|---|---|
| `mdSpecDir` | Markdown ファイル（`.md`）が格納されたディレクトリ | `example` |
| `template` | JXls 形式の Excel テンプレートファイル（`.xlsx`） | `template/template.xlsx` |
| `output` | 出力先 Excel ファイルパス（`.xlsx`） | `dist/out.xlsx` |

> **注意:** 出力先のディレクトリ（例: `dist/`）は事前に作成しておく必要があります。

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
mkdir -p dist
java -jar build/libs/md-test-spec2excel-1.0.0.jar \
  example template/template.xlsx dist/out.xlsx
```

### Docker

Java のインストールが不要です。Docker が使える環境であればすぐに実行できます。

```bash
# イメージのビルド
docker build -f docker/Dockerfile -t md-test-spec2excel .

# 実行（カレントディレクトリを /workspaces にマウント）
mkdir -p dist
docker run --rm \
  -v $(pwd):/workspaces md-test-spec2excel \
    /workspaces/example /workspaces/template/template.xlsx /workspaces/dist/out.xlsx
```

> **注意:** `-v $(pwd):/workspaces` でカレントディレクトリをコンテナ内の `/workspaces` にマウントしています。  
> 引数のパスはすべてコンテナ内のパス（`/workspaces/...`）で指定してください。

## テンプレートのカスタマイズ

本ツールは [JXls](https://jxls.sourceforge.net/) を使用してExcelへの出力を行います。  
`template/template.xlsx` を編集することで、出力するExcelのレイアウトや書式を自由にカスタマイズできます。

### テンプレート内で使用できる変数

Markdown ファイルのファイル名（拡張子なし）が変数名になります。  
例えば `mdSpec.md` というファイルを変換した場合、テンプレート内では `${mdSpec.title}` のように参照できます。

| 変数 | 型 | 説明 |
|---|---|---|
| `${fileName.title}` | `String` | Markdown の `# 見出し1` から取得したタイトル |
| `${fileName.cases}` | `List<SpecCase>` | テストケースの一覧 |
| `${case.mainItem}` | `String` | 大項目（`## 見出し2`） |
| `${case.middleItem}` | `String` | 中項目（`### 見出し3`） |
| `${case.smallItem}` | `String` | 小項目（`#### 見出し4`） |
| `${case.steps}` | `String` | 確認手順（改行区切り） |
| `${case.expected}` | `String` | 想定動作（改行区切り） |
| `${case.notes}` | `String` | 備考（改行区切り） |

> `fileName` の部分は実際の Markdown ファイル名（拡張子なし）に置き換えてください。  
> JXls テンプレートの構文については [JXls 公式ドキュメント](https://jxls.sourceforge.net/) を参照してください。

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

