package pharmcadd.form.model

import pharmcadd.form.jooq.enums.FormScheduleType
import pharmcadd.form.jooq.enums.ParticipantType
import pharmcadd.form.jooq.enums.QuestionType
import java.time.LocalDateTime
import java.time.OffsetDateTime

data class FormVo(
    val id: Long? = null,
    val title: String,
    val description: String? = null,
    val questions: List<QuestionVo> = emptyList(),
    val schedules: List<ScheduleVo> = emptyList(),
) {
    data class QuestionVo(
        val id: Long? = null,
        val title: String,
        val abbr: String? = null,
        val type: QuestionType,
        val required: Boolean = false,
        val options: List<OptionVo> = emptyList(),
    ) {
        data class OptionVo(
            val id: Long? = null,
            val text: String,
        )
    }
}

data class ScheduleVo(
    val id: Long? = null,
    val type: FormScheduleType,
    val timeZoneId: Long,
    val startsAt: LocalDateTime? = null,
    val endsAt: LocalDateTime? = null,
    val cronExpression: String? = null,
    val cronDuration: Long? = null,
    val active: Boolean,
    val mailing: Boolean? = true,
    val participants: List<ParticipantVo> = emptyList(),
)

data class ParticipantVo(
    val id: Long? = null,
    val type: ParticipantType,
    val userId: Long?,
    val groupId: Long?,
    val includeSubgroup: Boolean = false,
)

data class AnswerVo(
    val questionId: Long,
    val optionIds: List<Long> = emptyList(),
    val attachmentId: Long? = null,
    val userId: Long? = null,
    val groupId: Long? = null,
    val text: String? = null,
)

data class AnswerStatVo(
    val id: Long? = null,
    val campaignId: Long,
    val userId: Long,
    val userName: String,
    val ans1: String? = null,
    val ans2: String? = null,
    val ans3: String? = null,
    val ans4: String? = null,
    val ans5: String? = null,
    val ans6: String? = null,
    val ans7: String? = null,
    val ans8: String? = null,
    val ans9: String? = null,
    val ans10: String? = null,
    val ans11: String? = null,
    val ans12: String? = null,
    val ans13: String? = null,
    val ans14: String? = null,
    val ans15: String? = null,
    val ans16: String? = null,
    val ans17: String? = null,
    val ans18: String? = null,
    val ans19: String? = null,
    val ans20: String? = null,
    val createdAt: OffsetDateTime? = null,
    val updatedAt: OffsetDateTime? = null,
    val deletedAt: OffsetDateTime? = null,
)
