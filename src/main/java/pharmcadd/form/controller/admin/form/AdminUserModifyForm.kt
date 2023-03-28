package pharmcadd.form.controller.admin.form

import pharmcadd.form.jooq.enums.UserRole

data class AdminUserModifyForm(
    val name: String,
    val password: String,
    val email: String,
    val role: UserRole,
    val timeZoneId: Long,
    val active: Boolean,
)
