package pharmcadd.form.controller.admin.form

import java.time.OffsetDateTime

data class AdminFormListFormModel(
    val id: Long,
    val title: String,
    val description: String?,
    val createdBy: Long,
    val updatedBy: Long,
    val createdByName: String,
    val updatedByName: String,
    val createdAt: OffsetDateTime,
    val updatedAt: OffsetDateTime,
)
