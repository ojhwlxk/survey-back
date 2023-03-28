package pharmcadd.form.controller.admin.form

import pharmcadd.form.common.util.pagination.DataTableForm
import pharmcadd.form.jooq.enums.CancelAnswerStatus

class AdminAnswerCancelListForm : DataTableForm() {
    var type: CancelAnswerStatus? = null
    var requester: String? = null
    var approver: String? = null
}
