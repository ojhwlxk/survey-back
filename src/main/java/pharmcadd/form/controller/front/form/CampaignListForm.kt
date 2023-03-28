package pharmcadd.form.controller.front.form

import pharmcadd.form.controller.admin.form.AdminCampaignListForm

class CampaignListForm : AdminCampaignListForm() {
    var type: Type? = null

    enum class Type {
        ALL, READY, COMPLETED
    }
}
