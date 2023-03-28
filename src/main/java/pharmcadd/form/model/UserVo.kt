package pharmcadd.form.model

import pharmcadd.form.jooq.enums.UserRole
import pharmcadd.form.jooq.tables.pojos.TimeZone
import pharmcadd.form.jooq.tables.records.UserRecord
import java.time.OffsetDateTime

data class UserDetail(
    val user: UserVo,
    val timezone: TimeZone,
    val groups: List<Group> = emptyList(),
) {
    data class Group(
        val groupId: Long,
        val groupName: String,
        val positionId: Long?,
        val positionName: String?,
    )
}

data class UserVo(
    val id: Long,
    val name: String,
    val username: String,
    val email: String,
    val role: UserRole,
    val timeZoneId: Long,
    val active: Boolean,
    val createdAt: OffsetDateTime,
    val updatedAt: OffsetDateTime
) {
    companion object {
        fun of(record: UserRecord): UserVo {
            return UserVo(
                record.id,
                record.name,
                record.username,
                record.email,
                record.role,
                record.timeZoneId,
                record.active,
                record.createdAt,
                record.updatedAt
            )
        }
    }
}
