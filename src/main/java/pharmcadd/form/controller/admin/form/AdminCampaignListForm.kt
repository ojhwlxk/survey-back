package pharmcadd.form.controller.admin.form

import pharmcadd.form.common.util.pagination.DataTableForm
import pharmcadd.form.jooq.enums.CampaignStatus
import java.time.LocalDate

open class AdminCampaignListForm : DataTableForm() {
    var status: CampaignStatus? = null
    var formId: Long? = null
    var rangeFrom: LocalDate? = null
    var rangeTo: LocalDate? = null
    var keyword: String? = null
}
