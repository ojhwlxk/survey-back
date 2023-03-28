package pharmcadd.form.controller.front.form

import pharmcadd.form.jooq.enums.UserRole
import java.time.OffsetDateTime

data class UserListFormModel(
    val id: Long,
    val name: String,
    val username: String,
    val email: String,
    val role: UserRole,
    val timeZoneId: Long,
    val active: Boolean,
    val createdAt: OffsetDateTime,
    val updatedAt: OffsetDateTime,
    val groupNames: List<String> = emptyList(),
    val positionNames: List<String> = emptyList()
)
