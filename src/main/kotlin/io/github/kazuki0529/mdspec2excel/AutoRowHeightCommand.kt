package io.github.kazuki0529.mdspec2excel

import org.jxls.command.AbstractCommand
import org.jxls.common.CellRef
import org.jxls.common.Context
import org.jxls.common.Size
import org.jxls.transform.poi.PoiTransformer

/**
 * 自動高さと自動幅の設定を行う Jxls コマンド
 */
class AutoRowHeightCommand : AbstractCommand() {
    override fun getName(): String = "autoRowHeight"

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
