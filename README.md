# MdTestSpec2Excel

![CI](https://github.com/kazuki0529/md-test-spec2excel/actions/workflows/ci.yml/badge.svg)

Markdown 形式で記載したテスト仕様書を Excel に変換するツールです。  
Markdown ファイルごとに別々の Excel を出力するのではなく、  
[JXls](https://jxls.sourceforge.net/) 形式の Excel テンプレートへ埋め込む形式で出力します。

## 特徴

- Markdown でテスト仕様書を記述 → Excel に変換
- JXls テンプレートで出力レイアウトを自由にカスタマイズ可能
- Fat Jar または Docker で実行可能
- 複数の Markdown ファイルをまとめて1つの Excel に出力

## クイックスタート

```bash
# 1. ビルド（Java 8 以上が必要）
./gradlew shadowJar

# 2. 出力先ディレクトリを作成
mkdir -p dist

# 3. サンプルを実行
java -jar build/libs/md-test-spec2excel-1.0.0.jar \
  example template/template.xlsx dist/out.xlsx
```

`dist/out.xlsx` が生成されていれば成功です。

## ドキュメント

| ドキュメント | 内容 |
|---|---|
| [使い方](docs/usage.md) | 実行方法（Fat Jar / Docker）・引数の説明 |
| [Markdown ファイルの仕様](docs/markdown-spec.md) | 記述ルール・入出力の例 |
| [テンプレートのカスタマイズ](docs/template.md) | JXls テンプレートの書き方・使用できる変数一覧 |
| [コントリビューション](CONTRIBUTING.md) | 開発環境・ビルド・テスト・プロジェクト構成 |

## ライセンス

[MIT License](LICENSE)

