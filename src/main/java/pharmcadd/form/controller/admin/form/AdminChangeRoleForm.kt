package pharmcadd.form.controller.admin.form

import pharmcadd.form.jooq.enums.UserRole

data class AdminChangeRoleForm(
    val role: UserRole,
)
