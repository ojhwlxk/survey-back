package pharmcadd.form.common.util.excel

import org.apache.poi.ss.usermodel.Cell
import org.apache.poi.ss.usermodel.Row
import org.apache.poi.ss.usermodel.Sheet
import org.apache.poi.ss.usermodel.Workbook
import org.apache.poi.xssf.streaming.SXSSFWorkbook
import java.io.File
import java.io.OutputStream
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

fun generateWorkBook(init: pharmcadd.form.common.util.excel.Workbook.() -> Unit): Workbook = Workbook(
    SXSSFWorkbook()
).apply(init)


class Workbook(
    private val workbook: org.apache.poi.ss.usermodel.Workbook
) : org.apache.poi.ss.usermodel.Workbook by workbook {

    fun generate(path: String) = generate(File(path))

    fun generate(path: File) = generate(path.outputStream())

    fun generate(os: OutputStream) = workbook.use { os.use { workbook.write(os) } }

    fun sheet(init: pharmcadd.form.common.util.excel.Sheet.() -> Unit): pharmcadd.form.common.util.excel.Sheet {
        return Sheet(workbook, workbook.createSheet()).apply(init)
    }
}

class Sheet(
    private val workbook: Workbook,
    private val sheet: org.apache.poi.ss.usermodel.Sheet
) : org.apache.poi.ss.usermodel.Sheet by sheet {

    fun row(rowNum: Int = sheet.physicalNumberOfRows, init: pharmcadd.form.common.util.excel.Row.() -> Unit): Row {
        return Row(
            workbook,
            sheet.createRow(rowNum)
        ).apply(init)
    }

}

class Row(
    private val workbook: Workbook,
    private val row: org.apache.poi.ss.usermodel.Row
) : org.apache.poi.ss.usermodel.Row by row {
    fun cell(value: String, column: Int, init: (pharmcadd.form.common.util.excel.Cell.() -> Unit)? = null): Cell {
        return cell(column, value, init)
    }

    private fun cell(column: Int, value: Any?, init: (pharmcadd.form.common.util.excel.Cell.() -> Unit)? = null): Cell {
        val cell = row.createCell(column)

        if (value == null) {
            cell.setCellValue("")
        } else {
            when (value) {
                is Date -> cell.setCellValue(value)
                is String -> cell.setCellValue(value)
                is Number -> cell.setCellValue(value.toDouble())
                is Boolean -> cell.setCellValue(value)
                is LocalDate -> cell.setCellValue(value)
                is LocalDateTime -> cell.setCellValue(value)
                else -> cell.setCellValue(value.toString())
            }
        }
        return Cell(cell).apply {
            if (init != null) {
                init()
            }
        }
    }
}

class Cell(private val cell: Cell) : Cell by cell