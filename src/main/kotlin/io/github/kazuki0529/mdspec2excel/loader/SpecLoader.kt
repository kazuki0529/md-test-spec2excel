package io.github.kazuki0529.mdspec2excel.loader

import io.github.kazuki0529.mdspec2excel.model.Spec
import io.github.kazuki0529.mdspec2excel.model.SpecCase

import com.vladsch.flexmark.ast.BulletList
import com.vladsch.flexmark.ast.FencedCodeBlock
import com.vladsch.flexmark.ast.Heading
import com.vladsch.flexmark.ast.OrderedList
import com.vladsch.flexmark.parser.Parser
import com.vladsch.flexmark.util.ast.Document
import com.vladsch.flexmark.util.ast.Node
import com.vladsch.flexmark.util.data.MutableDataSet
import java.io.File
import java.nio.charset.StandardCharsets

/**
 * Markdown 形式のテスト仕様書ファイルをパースするローダ。
 *
 * flexmark-java を使用して Markdown の AST を走査し、見出し・順序付きリスト・
 * 箇条書きリスト・コードブロックを読み取ってテストケース情報を構築する。
 *
 * 通常は [parse] コンパニオン関数を介して利用する。
 *
 * @param specFile パース対象の Markdown ファイル
 */
class SpecLoader(specFile: File) {
    /** 現在走査中の AST ノード。走査が終了した場合は `null`。 */
    var cursor: Node? = null
        private set
    private var document: Document

    /** Markdown ファイルの `# 見出し 1` から取得したテスト仕様のタイトル。 */
    var title = ""
        private set
    /** `## 見出し 2` から取得した大項目名。 */
    var mainItem = ""
        private set
    /** `### 見出し 3` から取得した中項目名。 */
    var middleItem = ""
        private set
    /** `#### 見出し 4` から取得した小項目名。 */
    var smallItem = ""
        private set
    /** 順序付きリストから取得した確認手順のリスト。 */
    var steps = listOf<String>()
        private set
    /** 箇条書きリスト（チェックリスト形式）から取得した想定動作のリスト。 */
    var expected = listOf<String>()
        private set
    /** コードブロックから取得した備考のリスト。 */
    var notes = listOf<String>()
        private set

    /** これまでに構築したテストケースの一覧。 */
    var cases = mutableListOf<SpecCase>()
        private set

    init {
        val parser = Parser.builder(MutableDataSet()).build()
        this.document = parser.parse(
            specFile.readLines(StandardCharsets.UTF_8).joinToString("\n")
        )
        this.cursor = this.document.firstChild
    }

    /** 次のノードが存在するかどうかを返す。 */
    fun hasNext(): Boolean = this.cursor?.next != null

    /**
     * カーソルを次のノードへ進める。
     *
     * 新しいケースの開始条件を満たす場合、または走査終了時に未確定のケースが残っている場合に
     * 現在のフィールド値から [SpecCase] を生成して [cases] へ追加する。
     *
     * @return 次のノードが存在すれば `true`、走査終了なら `false`
     */
    fun next(): Boolean {
        this.cursor = this.cursor?.next
        if (this.isNewCase() || (this.cursor == null && (this.steps.isNotEmpty() || this.expected.isNotEmpty()))) {
            this.cases.add(
                SpecCase(
                    this.mainItem,
                    this.middleItem,
                    this.smallItem,
                    this.steps.joinToString("\n"),
                    this.expected.joinToString("\n"),
                    this.notes.joinToString("\n")
                )
            )
            this.notifyNewCase()
        }
        return this.cursor != null
    }

    /**
     * 現在のノードが新しいテストケースの開始を示すかどうかを判定する。
     *
     * カーソルが見出しノードであり、かつ手順と想定動作が既に収集済みの場合に `true` を返す。
     */
    private fun isNewCase(): Boolean =
        this.cursor is Heading && this.steps.isNotEmpty() && this.expected.isNotEmpty()

    /** 新しいテストケース開始時に各フィールドを初期化する。 */
    private fun notifyNewCase() {
        this.mainItem = ""
        this.middleItem = ""
        this.smallItem = ""
        this.steps = listOf()
        this.expected = listOf()
        this.notes = listOf()
    }

    /** [title] を更新する。 */
    fun notifyTitle(value: String) { this.title = value }
    /** [mainItem] を更新する。 */
    fun notifyMainItem(value: String) { this.mainItem = value }
    /** [middleItem] を更新する。 */
    fun notifyMiddleItem(value: String) { this.middleItem = value }
    /** [smallItem] を更新する。 */
    fun notifySmallItem(value: String) { this.smallItem = value }

    /**
     * 順序付きリストのテキストを解析して [steps] を更新する。
     *
     * 各行の先頭にある番号表記を正規化し、`1. テキスト` の形式に統一する。
     *
     * @param value 順序付きリストの生テキスト
     */
    fun notifySteps(value: String) {
        var num = 1
        this.steps = value.trim().split("\n").map {
            "${num++}. " + it.replace("""^\d+\. """.toRegex(), "")
        }
    }

    /**
     * 箇条書きリストのテキストを解析して [expected] を更新する。
     *
     * チェックリスト形式（`- [ ] テキスト`）の先頭記号を `・` に置換する。
     *
     * @param value 箇条書きリストの生テキスト
     */
    fun notifyExpected(value: String) {
        this.expected = value.trim().split("\n").map {
            "・" + it.replace("""^[\*\+\-] \[ \] """.toRegex(), "")
        }
    }

    /**
     * コードブロックのテキストを解析して [notes] を更新する。
     *
     * フェンス行（`` ``` `` で始まる行）を除いたテキスト行を備考として保持する。
     *
     * @param value コードブロックの生テキスト（フェンス行を含む）
     */
    fun notifyNotes(value: String) {
        this.notes = value.split("\n").filter { !it.startsWith("```") }
    }

    companion object {
        /**
         * Markdown ファイル 1 つを解析して [Spec] を返す。
         *
         * AST を先頭から末尾まで走査し、見出しレベルに応じて
         * タイトル・大項目・中項目・小項目を更新しながら [SpecCase] を構築する。
         * 順序付きリストを手順、箇条書きリストを想定動作、コードブロックを備考として扱う。
         *
         * @param file パース対象の Markdown ファイル
         * @return パース結果の [Spec]
         */
        fun parse(file: File): Spec {
            val loader = SpecLoader(file)
            do {
                when (val current = loader.cursor) {
                    is Heading -> when (current.level) {
                        1 -> loader.notifyTitle(current.text.toString())
                        2 -> loader.notifyMainItem(current.text.toString())
                        3 -> loader.notifyMiddleItem(current.text.toString())
                        4 -> loader.notifySmallItem(current.text.toString())
                    }
                    is OrderedList -> loader.notifySteps(current.chars.toString())
                    is BulletList -> loader.notifyExpected(current.chars.toString())
                    is FencedCodeBlock -> loader.notifyNotes(current.chars.toString())
                }
            } while (loader.next())
            return Spec(file.nameWithoutExtension, loader.title, loader.cases)
        }
    }
}
