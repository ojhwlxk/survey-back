package pharmcadd.form.common.util.pagination

import com.google.common.base.CaseFormat
import org.jooq.*
import org.jooq.impl.DSL
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min

fun <R : Record> SelectOrderByStep<R>.orderBy(form: DataTableForm) {
    if (form.sortBy.isNullOrBlank()) {
        return
    }

    val fields = form.sortBy!!.split(",")
        .map { it.trim() }
        .filter { it.isNotBlank() }

    val sortOrders = form.sortDesc!!.split(",")
        .filter { it.isNotBlank() }
        .map {
            if (it.trim().toBoolean()) {
                SortOrder.DESC
            } else {
                SortOrder.ASC
            }
        }

    if (fields.size != sortOrders.size) {
        throw RuntimeException("number of fields and sort directions do not match")
    }

    fields.zip(sortOrders).forEach { (field, sortOrder) ->
        val to = CaseFormat.LOWER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, field)
        orderBy(DSL.field("\"${to}\"").sort(sortOrder))
    }
}

data class DataTablePagination<E>(
    val total: Int,
    val content: List<E>
) {
    companion object {
        @JvmStatic
        fun <R : Record, E> of(
            ctx: DSLContext,
            query: SelectLimitStep<R>,
            form: DataTableForm,
            mapper: (Record: R) -> E
        ): DataTablePagination<E> {
            val forPage = form.page!!
            val itemsPerPage = form.itemsPerPage!!

            val total = ctx.fetchCount(query)
            (query as SelectOrderByStep<R>).orderBy(form)

            val content = if (total > 0) {
                val lastPage = intCeil(total, itemsPerPage)
                if (forPage > lastPage) {
                    emptyList()
                } else {
                    val page = max(min(lastPage, forPage), 1)
                    val offset = (page - 1) * itemsPerPage
                    query.limit(offset, itemsPerPage).map(mapper)
                }
            } else {
                emptyList()
            }
            return DataTablePagination(total, content)
        }
    }
}

private fun intCeil(x: Int, y: Int): Int {
    return ceil(x.toDouble() / y).toInt()
}
