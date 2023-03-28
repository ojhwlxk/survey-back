package pharmcadd.form.common.util.excel

import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.junit.jupiter.api.Test
import java.io.File

internal class excelTest {

    @Test
    fun test() {

        val file = File.createTempFile("_temp", ".xlsx")
        val wb = XSSFWorkbook()

        val sheet = wb.createSheet("title")
        val row = sheet.createRow(10)
        //header
        val cell1 = row.createCell(0)

        //body
        cell1.setCellValue("3")

        wb.write(file.outputStream())
        wb.close()

    }

}
