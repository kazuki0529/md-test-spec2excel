package io.github.kazuki0529.mdspec2excel.excel

import org.jxls.command.AbstractCommand
import org.jxls.common.CellRef
import org.jxls.common.Context
import org.jxls.common.Size
import org.jxls.transform.poi.PoiTransformer

/**
 * JXls のカスタムコマンド: セルの描画後に行の高さを自動調整する。
 *
 * Excel テンプレート内のセルコメントで `jx:autoRowHeight(lastCell=...)` と指定することで使用する。
 * [applyAt] 内でテンプレート領域を適用した後、Apache POI の行高さを `-1`（自動）に設定する。
 */
class AutoRowHeightCommand : AbstractCommand() {
    /** このコマンドの識別名を返す。JXls テンプレートから参照される名前と一致させる必要がある。 */
    override fun getName(): String = "autoRowHeight"

    /**
     * テンプレート領域を [cellRef] へ適用し、適用後に行の高さを自動調整する。
     *
     * @param cellRef 出力先のセル参照
     * @param context JXls の変数コンテキスト
     * @return 適用後のセル領域サイズ
     */
    override fun applyAt(cellRef: CellRef?, context: Context?): Size {
        val area = this.areaList[0]
        val size = area.applyAt(cellRef, context)
        if (cellRef != null) {
            val transformer = area.transformer as PoiTransformer
            transformer.workbook.getSheet(cellRef.sheetName).getRow(cellRef.row).apply {
                height = -1
            }
        }
        return size
    }
}
