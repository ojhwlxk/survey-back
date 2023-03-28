package pharmcadd.form.model

import pharmcadd.form.jooq.enums.ApprovalType
import java.time.OffsetDateTime

data class CancelAnswerVo(
    val id: Long,
    val campaignId: Long,
    val approvalType: ApprovalType,
    val requester: Long,
    val reason: String?,
    val approver: Long?,
    val answer: String?,
    val createdAt: OffsetDateTime,
    val updatedAt: OffsetDateTime,
    val deletedAt: OffsetDateTime? = null,

    val requesterName: String,
    val approverName: String?,
)
