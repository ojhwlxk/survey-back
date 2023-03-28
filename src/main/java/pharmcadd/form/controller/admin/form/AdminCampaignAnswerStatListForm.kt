package pharmcadd.form.controller.admin.form

import pharmcadd.form.common.util.pagination.DataTableForm

class AdminCampaignAnswerStatListForm : DataTableForm() {
    var respondentType: RespondentType? = null
    var keyword: String? = null

    enum class RespondentType {
        RESPONDENT, NONRESPONDENT
    }
}
