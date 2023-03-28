package pharmcadd.form.controller.admin.form

import pharmcadd.form.common.util.pagination.DataTableForm

class AnswerStatListForm : DataTableForm() {
    var formId: Long? = null
    var campaignId: Long? = null
}

data class AnswerStatsForm(
    val questionId: Long,
    val total: Int,
    val optionStats: List<OptionStatsForm> = emptyList(),
) {
    data class OptionStatsForm(
        val optionId: Long,
        val text: String,
        val count: Int,
    )
}