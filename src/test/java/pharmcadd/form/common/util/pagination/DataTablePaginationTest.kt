package pharmcadd.form.common.util.pagination

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min

internal class DataTablePaginationTest {

    private fun intCeil(x: Int, y: Int): Int {
        return ceil(x.toDouble() / y).toInt()
    }

    @Test
    fun test() {
        val total = 8
        val itemsPerPage = 10
        val forPage = 2

        val lastPage = if (total == 0) 1 else intCeil(total, itemsPerPage)
        val page = max(min(lastPage, forPage), 1)
        val offset = (page - 1) * itemsPerPage
        println(offset)
        if (forPage > lastPage) {
            println("empty")
        }
    }
}
