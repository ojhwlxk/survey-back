package pharmcadd.form.service

import org.jooq.DSLContext
import org.jooq.Result
import org.jooq.UpdateConditionStep
import org.jooq.impl.DSL
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import pharmcadd.form.common.extension.batch
import pharmcadd.form.jooq.Tables.FORM_SCHEDULE_PARTICIPANT
import pharmcadd.form.jooq.Tables.GROUP
import pharmcadd.form.jooq.enums.ParticipantType
import pharmcadd.form.jooq.tables.records.FormScheduleParticipantRecord
import pharmcadd.form.model.ParticipantVo

@Service
@Transactional
class FormScheduleParticipantService {

    @Autowired
    lateinit var dsl: DSLContext

    fun add(scheduleId: Long, formId: Long, form: ParticipantVo): Long {
        return add(scheduleId, formId, form.type, form.userId, form.groupId, form.includeSubgroup)
    }

    fun add(
        scheduleId: Long,
        formId: Long,
        type: ParticipantType,
        userId: Long?,
        groupId: Long?,
        includeSubgroup: Boolean
    ): Long {
        return dsl.insertInto(FORM_SCHEDULE_PARTICIPANT)
            .set(FORM_SCHEDULE_PARTICIPANT.SCHEDULE_ID, scheduleId)
            .set(FORM_SCHEDULE_PARTICIPANT.FORM_ID, formId)
            .set(FORM_SCHEDULE_PARTICIPANT.TYPE, type)
            .set(FORM_SCHEDULE_PARTICIPANT.USER_ID, userId)
            .set(FORM_SCHEDULE_PARTICIPANT.GROUP_ID, groupId)
            .set(FORM_SCHEDULE_PARTICIPANT.INCLUDE_SUBGROUP, includeSubgroup)
            .returningResult(FORM_SCHEDULE_PARTICIPANT.ID)
            .fetchOne()!!
            .value1()!!
    }

    fun addUsers(scheduleId: Long, formId: Long, userIds: List<Long>) {
        userIds
            .map { userId ->
                dsl.insertInto(FORM_SCHEDULE_PARTICIPANT)
                    .set(FORM_SCHEDULE_PARTICIPANT.SCHEDULE_ID, scheduleId)
                    .set(FORM_SCHEDULE_PARTICIPANT.FORM_ID, formId)
                    .set(FORM_SCHEDULE_PARTICIPANT.TYPE, ParticipantType.USER)
                    .set(FORM_SCHEDULE_PARTICIPANT.USER_ID, userId)
                    .setNull(FORM_SCHEDULE_PARTICIPANT.GROUP_ID)
                    .set(FORM_SCHEDULE_PARTICIPANT.INCLUDE_SUBGROUP, false)
            }
            .batch(dsl)
    }

    fun addUser(scheduleId: Long, formId: Long, userId: Long): Long {
        return add(scheduleId, formId, ParticipantType.USER, userId, null, false)
    }

    fun addGroup(scheduleId: Long, formId: Long, groupId: Long, includeSubgroup: Boolean): Long {
        return add(scheduleId, formId, ParticipantType.GROUP, null, groupId, includeSubgroup)
    }

    fun modify(id: Long, scheduleId: Long, formId: Long, form: ParticipantVo) {
        modify(id, scheduleId, formId, form.type, form.userId, form.groupId, form.includeSubgroup)
    }

    fun modify(
        id: Long,
        scheduleId: Long,
        formId: Long,
        type: ParticipantType,
        userId: Long?,
        groupId: Long?,
        includeSubgroup: Boolean
    ) {
        dsl.update(FORM_SCHEDULE_PARTICIPANT)
            .set(FORM_SCHEDULE_PARTICIPANT.SCHEDULE_ID, scheduleId)
            .set(FORM_SCHEDULE_PARTICIPANT.FORM_ID, formId)
            .set(FORM_SCHEDULE_PARTICIPANT.TYPE, type)
            .set(FORM_SCHEDULE_PARTICIPANT.USER_ID, userId)
            .set(FORM_SCHEDULE_PARTICIPANT.GROUP_ID, groupId)
            .set(FORM_SCHEDULE_PARTICIPANT.INCLUDE_SUBGROUP, includeSubgroup)
            .set(FORM_SCHEDULE_PARTICIPANT.UPDATED_AT, DSL.currentOffsetDateTime())
            .where(
                FORM_SCHEDULE_PARTICIPANT.ID.eq(id)
                    .and(FORM_SCHEDULE_PARTICIPANT.SCHEDULE_ID.eq(scheduleId))
                    .and(FORM_SCHEDULE_PARTICIPANT.FORM_ID.eq(formId))
                    .and(FORM_SCHEDULE_PARTICIPANT.DELETED_AT.isNull)
            )
            .execute()
    }

    @Transactional(readOnly = true)
    fun findOne(id: Long): FormScheduleParticipantRecord? {
        return dsl
            .selectFrom(FORM_SCHEDULE_PARTICIPANT)
            .where(
                FORM_SCHEDULE_PARTICIPANT.ID.eq(id)
                    .and(FORM_SCHEDULE_PARTICIPANT.DELETED_AT.isNull)
            )
            .fetchOne()
    }

    @Transactional(readOnly = true)
    fun findByFormId(formId: Long): Result<FormScheduleParticipantRecord> {
        return dsl
            .selectFrom(FORM_SCHEDULE_PARTICIPANT)
            .where(
                FORM_SCHEDULE_PARTICIPANT.FORM_ID.eq(formId)
                    .and(FORM_SCHEDULE_PARTICIPANT.DELETED_AT.isNull)
            )
            .fetch()
    }

    @Transactional(readOnly = true)
    fun findByScheduleId(scheduleId: Long): Result<FormScheduleParticipantRecord> {
        return dsl
            .selectFrom(FORM_SCHEDULE_PARTICIPANT)
            .where(
                FORM_SCHEDULE_PARTICIPANT.SCHEDULE_ID.eq(scheduleId)
                    .and(FORM_SCHEDULE_PARTICIPANT.DELETED_AT.isNull)
            )
            .fetch()
    }

    private fun delete(block: (UpdateConditionStep<FormScheduleParticipantRecord>) -> Unit) {
        dsl.update(FORM_SCHEDULE_PARTICIPANT)
            .set(FORM_SCHEDULE_PARTICIPANT.UPDATED_AT, DSL.currentOffsetDateTime())
            .set(FORM_SCHEDULE_PARTICIPANT.DELETED_AT, DSL.currentOffsetDateTime())
            .where(
                FORM_SCHEDULE_PARTICIPANT.DELETED_AT.isNull
            )
            .also(block)
            .execute()
    }

    fun deleteById(id: Long) = delete { it.and(FORM_SCHEDULE_PARTICIPANT.ID.eq(id)) }

    fun deleteByFormId(formId: Long) = delete { it.and(FORM_SCHEDULE_PARTICIPANT.FORM_ID.eq(formId)) }

    fun deleteByScheduleId(scheduleId: Long) = delete { it.and(FORM_SCHEDULE_PARTICIPANT.SCHEDULE_ID.eq(scheduleId)) }

    fun deleteByGroupId(groupId: Long) {
        delete {
            it
                .and(
                    FORM_SCHEDULE_PARTICIPANT.GROUP_ID.`in`(
                        dsl
                            .select(GROUP.ID)
                            .from(GROUP)
                            .where(
                                GROUP.PATHWAYS.contains(arrayOf(groupId))
                                    .and(GROUP.DELETED_AT.isNull)
                            )
                    )
                )
        }
    }

    fun deleteByUserId(userId: Long) = delete { it.and(FORM_SCHEDULE_PARTICIPANT.USER_ID.eq(userId)) }
}
