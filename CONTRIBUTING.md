# コントリビューションガイド

バグ報告・機能提案・プルリクエストを歓迎します。

## 開発環境のセットアップ

**必要なもの:**

- Java 8 以上
- Kotlin 1.9.x（Gradle が自動ダウンロード）

```bash
git clone https://github.com/kazuki0529/md-test-spec2excel.git
cd md-test-spec2excel
./gradlew build
```

## ビルド

```bash
# 通常ビルド
./gradlew build

# Fat Jar のビルド
./gradlew shadowJar
```

ビルド成果物は `build/libs/md-test-spec2excel-1.0.0.jar` に生成されます。

## テストの実行

```bash
./gradlew test
```

## プロジェクト構成

```
.
├── src/
│   ├── main/kotlin/io/github/kazuki0529/mdspec2excel/
│   │   ├── Main.kt                        # エントリーポイント
│   │   ├── model/Spec.kt                  # データモデル
│   │   ├── loader/SpecLoader.kt           # Markdown パース処理
│   │   ├── converter/Converter.kt         # ディレクトリ走査 + Excel 出力
│   │   └── excel/AutoRowHeightCommand.kt  # JXls カスタムコマンド
│   └── test/
├── template/
│   └── template.xlsx                      # サンプルテンプレート
├── example/
│   └── mdSpec.md                          # サンプル Markdown
└── docker/
    └── Dockerfile
```

## プルリクエストの送り方

1. このリポジトリを Fork する
2. フィーチャーブランチを作成する（`git checkout -b feature/your-feature`）
3. 変更をコミットする
4. ブランチをプッシュする（`git push origin feature/your-feature`）
5. Pull Request を作成する

## バグ報告・機能要望

[GitHub Issues](https://github.com/kazuki0529/md-test-spec2excel/issues) からお知らせください。

## ライセンス

このプロジェクトは [MIT License](LICENSE) のもとで公開されています。
