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
