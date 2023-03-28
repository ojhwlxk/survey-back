package pharmcadd.form.service

import org.jooq.DSLContext
import org.jooq.Result
import org.jooq.impl.DSL
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import pharmcadd.form.jooq.Tables.FORM_NOTIFICATION
import pharmcadd.form.jooq.tables.records.FormNotificationRecord

@Service
@Transactional
class FormNotificationService {

    @Autowired
    lateinit var dsl: DSLContext

    fun add(formId: Long, userId: Long): Long {
        return dsl.insertInto(FORM_NOTIFICATION)
            .set(FORM_NOTIFICATION.FORM_ID, formId)
            .set(FORM_NOTIFICATION.USER_ID, userId)
            .returningResult(FORM_NOTIFICATION.ID)
            .fetchOne()!!
            .value1()!!
    }

    @Transactional(readOnly = true)
    fun findByFormId(formId: Long): Result<FormNotificationRecord> {
        return dsl.selectFrom(FORM_NOTIFICATION)
            .where(
                FORM_NOTIFICATION.FORM_ID.eq(formId)
                    .and(FORM_NOTIFICATION.DELETED_AT.isNull)
            )
            .fetch()
    }

    @Transactional(readOnly = true)
    fun findOne(id: Long): FormNotificationRecord? {
        return dsl.selectFrom(FORM_NOTIFICATION)
            .where(
                FORM_NOTIFICATION.ID.eq(id)
                    .and(FORM_NOTIFICATION.DELETED_AT.isNull)
            )
            .fetchOne()
    }

    fun deleteByFormId(formId: Long) {
        dsl.update(FORM_NOTIFICATION)
            .set(FORM_NOTIFICATION.UPDATED_AT, DSL.currentOffsetDateTime())
            .set(FORM_NOTIFICATION.DELETED_AT, DSL.currentOffsetDateTime())
            .where(
                FORM_NOTIFICATION.FORM_ID.eq(formId)
                    .and(FORM_NOTIFICATION.DELETED_AT.isNull)
            )
            .execute()
    }

    fun deleteById(id: Long) {
        dsl.update(FORM_NOTIFICATION)
            .set(FORM_NOTIFICATION.UPDATED_AT, DSL.currentOffsetDateTime())
            .set(FORM_NOTIFICATION.DELETED_AT, DSL.currentOffsetDateTime())
            .where(
                FORM_NOTIFICATION.ID.eq(id)
                    .and(FORM_NOTIFICATION.DELETED_AT.isNull)
            )
            .execute()
    }
}
