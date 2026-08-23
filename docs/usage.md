# 使い方

## 引数

```
java -jar md-test-spec2excel-1.0.0.jar <mdSpecDir> <template> <output>
```

| 引数 | 説明 | 例 |
|---|---|---|
| `mdSpecDir` | Markdown ファイル（`.md`）が格納されたディレクトリ | `example` |
| `template` | JXls 形式の Excel テンプレートファイル（`.xlsx`） | `template/template.xlsx` |
| `output` | 出力先 Excel ファイルパス（`.xlsx`） | `dist/out.xlsx` |

> **注意:** 出力先のディレクトリ（例: `dist/`）は事前に作成しておく必要があります。

---

## Fat Jar で実行する

Java 8 以上が必要です。

```bash
# ビルド
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

---

## Docker で実行する

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
