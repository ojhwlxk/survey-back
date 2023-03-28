package pharmcadd.form.controller.admin.form

import pharmcadd.form.jooq.enums.ParticipantType

data class ParticipantListFormModel(
    val id: Long,
    val campaignId: Long,
    val type: ParticipantType,
    val userId: Long?,
    val userName: String?,
    val groupId: Long?,
    val groupName: String?
)
