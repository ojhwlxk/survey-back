package pharmcadd.form.service

import org.jooq.DSLContext
import org.jooq.Result
import org.jooq.UpdateConditionStep
import org.jooq.impl.DSL
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import pharmcadd.form.jooq.Tables.FORM_SCHEDULE
import pharmcadd.form.jooq.enums.FormScheduleType
import pharmcadd.form.jooq.tables.records.FormScheduleRecord
import pharmcadd.form.model.ScheduleVo
import pharmcadd.form.schedule.ScheduleService
import java.time.LocalDateTime

@Service
@Transactional
class FormScheduleService {

    @Autowired
    lateinit var dsl: DSLContext

    @Autowired
    lateinit var scheduleService: ScheduleService

    @Autowired
    lateinit var timeZoneService: TimeZoneService

    fun add(
        formId: Long,
        type: FormScheduleType,
        timeZoneId: Long,
        startsAt: LocalDateTime? = null,
        endsAt: LocalDateTime? = null,
        cronExpression: String? = null,
        cronDuration: Long? = null,
        active: Boolean,
        mailing: Boolean? = true,
    ): Long {
        val record = dsl.insertInto(FORM_SCHEDULE)
            .set(FORM_SCHEDULE.FORM_ID, formId)
            .set(FORM_SCHEDULE.TYPE, type)
            .set(FORM_SCHEDULE.TIME_ZONE_ID, timeZoneId)
            .set(FORM_SCHEDULE.STARTS_AT, startsAt)
            .set(FORM_SCHEDULE.ENDS_AT, endsAt)
            .set(FORM_SCHEDULE.CRON_EXPRESSION, cronExpression)
            .set(FORM_SCHEDULE.CRON_DURATION, cronDuration)
            .set(FORM_SCHEDULE.ACTIVE, active)
            .set(FORM_SCHEDULE.MAILING, mailing)
            .returning(*FORM_SCHEDULE.fields())
            .fetchOne()!!

        scheduleService.scheduleJob(record)

        return record.id
    }

    fun addManual(
        formId: Long,
        timeZoneId: Long,
        startsAt: LocalDateTime,
        endsAt: LocalDateTime? = null,
        active: Boolean,
        mailing: Boolean? = true,
    ): Long {
        return add(
            formId,
            FormScheduleType.MANUAL,
            timeZoneId,
            startsAt,
            endsAt,
            null,
            null,
            active,
            mailing
        )
    }

    fun addCron(
        formId: Long,
        timeZoneId: Long,
        cronExpression: String,
        cronDuration: Long,
        active: Boolean,
        mailing: Boolean? = true,
    ): Long {
        return add(
            formId,
            FormScheduleType.CRON,
            timeZoneId,
            null,
            null,
            cronExpression,
            cronDuration,
            active,
            mailing,
        )
    }

    fun modify(
        id: Long,
        formId: Long,
        type: FormScheduleType,
        timeZoneId: Long,
        startsAt: LocalDateTime? = null,
        endsAt: LocalDateTime? = null,
        cronExpression: String? = null,
        cronDuration: Long? = null,
        active: Boolean,
        mailing: Boolean? = true
    ) {
        val record = dsl.update(FORM_SCHEDULE)
            .set(FORM_SCHEDULE.FORM_ID, formId)
            .set(FORM_SCHEDULE.TYPE, type)
            .set(FORM_SCHEDULE.TIME_ZONE_ID, timeZoneId)
            .set(FORM_SCHEDULE.STARTS_AT, startsAt)
            .set(FORM_SCHEDULE.ENDS_AT, endsAt)
            .set(FORM_SCHEDULE.CRON_EXPRESSION, cronExpression)
            .set(FORM_SCHEDULE.CRON_DURATION, cronDuration)
            .set(FORM_SCHEDULE.ACTIVE, active)
            .set(FORM_SCHEDULE.MAILING, mailing)
            .set(FORM_SCHEDULE.UPDATED_AT, DSL.currentOffsetDateTime())
            .where(
                FORM_SCHEDULE.ID.eq(id)
                    .and(FORM_SCHEDULE.FORM_ID.eq(formId))
                    .and(FORM_SCHEDULE.DELETED_AT.isNull)
            )
            .returning(*FORM_SCHEDULE.fields())
            .fetchOne()!!

        scheduleService.unscheduleJob(id)
        scheduleService.scheduleJob(record)
    }

    fun add(formId: Long, form: ScheduleVo): Long {
        // FIXME : participants 배치 등록 처리
        return add(
            formId,
            form.type,
            form.timeZoneId,
            form.startsAt,
            form.endsAt,
            form.cronExpression,
            form.cronDuration,
            form.active,
            form.mailing
        )
    }

    fun modify(id: Long, formId: Long, form: ScheduleVo) {
        // FIXME : participants 배치 수정 처리
        modify(
            id,
            formId,
            form.type,
            form.timeZoneId,
            form.startsAt,
            form.endsAt,
            form.cronExpression,
            form.cronDuration,
            form.active,
            form.mailing
        )
    }

    @Transactional(readOnly = true)
    fun findOne(id: Long): FormScheduleRecord? {
        return dsl
            .selectFrom(FORM_SCHEDULE)
            .where(
                FORM_SCHEDULE.ID.eq(id)
                    .and(FORM_SCHEDULE.DELETED_AT.isNull)
            )
            .fetchOne()
    }

    @Transactional(readOnly = true)
    fun findByFormId(formId: Long): Result<FormScheduleRecord> {
        return dsl
            .selectFrom(FORM_SCHEDULE)
            .where(
                FORM_SCHEDULE.FORM_ID.eq(formId)
                    .and(FORM_SCHEDULE.DELETED_AT.isNull)
            )
            .fetch()
    }

    fun active(id: Long) {
        dsl.update(FORM_SCHEDULE)
            .set(FORM_SCHEDULE.ACTIVE, true)
            .set(FORM_SCHEDULE.UPDATED_AT, DSL.currentOffsetDateTime())
            .where(
                FORM_SCHEDULE.ID.eq(id)
                    .and(FORM_SCHEDULE.DELETED_AT.isNull)
            )
            .execute()
    }

    fun inactive(id: Long) {
        dsl.update(FORM_SCHEDULE)
            .set(FORM_SCHEDULE.ACTIVE, false)
            .set(FORM_SCHEDULE.UPDATED_AT, DSL.currentOffsetDateTime())
            .where(
                FORM_SCHEDULE.ID.eq(id)
                    .and(FORM_SCHEDULE.DELETED_AT.isNull)
            )
            .execute()
    }

    private fun delete(block: (UpdateConditionStep<FormScheduleRecord>) -> Unit) {
        dsl.update(FORM_SCHEDULE)
            .set(FORM_SCHEDULE.UPDATED_AT, DSL.currentOffsetDateTime())
            .set(FORM_SCHEDULE.DELETED_AT, DSL.currentOffsetDateTime())
            .where(
                FORM_SCHEDULE.DELETED_AT.isNull
            )
            .also(block)
            .execute()
    }

    fun deleteById(id: Long) {
        scheduleService.unscheduleJob(id)
        delete { it.and(FORM_SCHEDULE.ID.eq(id)) }
    }

    fun deleteByFormId(formId: Long) {
        findByFormId(formId).forEach { scheduleService.unscheduleJob(it.id) }
        delete { it.and(FORM_SCHEDULE.FORM_ID.eq(formId)) }
    }
}
