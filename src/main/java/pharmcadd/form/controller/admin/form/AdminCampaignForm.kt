package pharmcadd.form.controller.admin.form

import pharmcadd.form.model.ParticipantVo
import java.time.LocalDateTime

data class AdminCampaignForm(
    val formId: Long,
    val title: String,
    val description: String?,
    val startsAt: LocalDateTime? = null,
    val endsAt: LocalDateTime? = null,
    val timeZoneId: Long? = null,
    val participants: List<ParticipantVo>,
    val mailing: Boolean? = true
)
