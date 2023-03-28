package pharmcadd.form.controller.front.form

import pharmcadd.form.common.util.pagination.DataTableForm

class UserListForm : DataTableForm() {
    var groupId: Long? = null
    var includeSubgroup: Boolean? = null
    var keyword: String? = null
    var positionId: Long? = null
}
