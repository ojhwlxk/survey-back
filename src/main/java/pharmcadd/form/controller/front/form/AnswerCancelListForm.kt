package pharmcadd.form.controller.front.form

import pharmcadd.form.common.util.pagination.DataTableForm
import pharmcadd.form.jooq.enums.CancelAnswerStatus
import java.time.LocalDate

class AnswerCancelListForm : DataTableForm() {
    var status : CancelAnswerStatus? = null
    var rangeFrom: LocalDate? = null
    var rangeTo: LocalDate? = null
}