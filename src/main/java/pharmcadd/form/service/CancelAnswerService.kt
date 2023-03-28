package pharmcadd.form.service

import org.jooq.DSLContext
import org.jooq.Result
import org.jooq.UpdateSetMoreStep
import org.jooq.impl.DSL
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import pharmcadd.form.jooq.Tables.*
import pharmcadd.form.jooq.enums.CancelAnswerStatus
import pharmcadd.form.jooq.tables.records.CancelAnswerRecord

@Service
@Transactional
class CancelAnswerService {

    @Autowired
    lateinit var dsl: DSLContext

    fun addRequest(campaignId: Long, requesterId: Long, reason: String): Long {
        return dsl.insertInto(CANCEL_ANSWER)
            .set(CANCEL_ANSWER.CAMPAIGN_ID, campaignId)
            .set(CANCEL_ANSWER.STATUS, CancelAnswerStatus.REQUEST)
            .set(CANCEL_ANSWER.USER_ID, requesterId)
            .set(CANCEL_ANSWER.REASON, reason)
            .returningResult(CANCEL_ANSWER.ID)
            .fetchOne()!!
            .value1()!!
    }

    private fun update(id: Long, block: (UpdateSetMoreStep<CancelAnswerRecord>) -> Unit) {
        dsl.update(CANCEL_ANSWER)
            .set(CANCEL_ANSWER.UPDATED_AT, DSL.currentOffsetDateTime())
            .also(block)
            .where(
                CANCEL_ANSWER.ID.eq(id)
                    .and(CANCEL_ANSWER.DELETED_AT.isNull)
            )
            .execute()
    }

    fun approve(id: Long, approverId: Long, answer: String? = null) {
        val cancelAnswer = findOne(id)!!
        val campaignId = cancelAnswer.campaignId
        val requesterId = cancelAnswer.userId

        update(id) { update ->
            update
                .set(CANCEL_ANSWER.STATUS, CancelAnswerStatus.APPROVE)
                .set(CANCEL_ANSWER.APPROVED_BY, approverId)
                .set(CANCEL_ANSWER.ANSWER, answer)
        }

        dsl.update(RESPONDENT)
            .set(RESPONDENT.UPDATED_AT, DSL.currentOffsetDateTime())
            .set(RESPONDENT.DELETED_AT, DSL.currentOffsetDateTime())
            .where(
                RESPONDENT.CAMPAIGN_ID.eq(campaignId)
                    .and(RESPONDENT.USER_ID.eq(requesterId))
            )
            .execute()

        dsl.update(ANSWER)
            .set(ANSWER.UPDATED_AT, DSL.currentOffsetDateTime())
            .set(ANSWER.DELETED_AT, DSL.currentOffsetDateTime())
            .where(
                ANSWER.CAMPAIGN_ID.eq(campaignId)
                    .and(ANSWER.CREATED_BY.eq(requesterId))
                    .and(ANSWER.DELETED_AT.isNull)
            )
            .execute()

        dsl.update(ANSWER_STAT)
            .set(ANSWER_STAT.UPDATED_AT, DSL.currentOffsetDateTime())
            .set(ANSWER_STAT.DELETED_AT, DSL.currentOffsetDateTime())
            .where(
                ANSWER_STAT.CAMPAIGN_ID.eq(campaignId)
                    .and(ANSWER_STAT.USER_ID.eq(requesterId))
                    .and(ANSWER_STAT.DELETED_AT.isNull)
            )
            .execute()
    }

    fun reject(id: Long, approverId: Long, answer: String? = null) {
        update(id) { update ->
            update
                .set(CANCEL_ANSWER.STATUS, CancelAnswerStatus.REJECT)
                .set(CANCEL_ANSWER.APPROVED_BY, approverId)
                .set(CANCEL_ANSWER.ANSWER, answer)
        }
    }

    @Transactional(readOnly = true)
    fun findByRequester(userId: Long): Result<CancelAnswerRecord> {
        return dsl
            .selectFrom(CANCEL_ANSWER)
            .where(
                CANCEL_ANSWER.USER_ID.eq(userId)
                    .and(CANCEL_ANSWER.DELETED_AT.isNull)
            )
            .fetch()
    }

    @Transactional(readOnly = true)
    fun findByCampaignIdAndRequester(campaignId: Long, userId: Long): Result<CancelAnswerRecord> {
        return dsl
            .selectFrom(CANCEL_ANSWER)
            .where(
                CANCEL_ANSWER.CAMPAIGN_ID.eq(campaignId)
                    .and(CANCEL_ANSWER.USER_ID.eq(userId))
                    .and(CANCEL_ANSWER.DELETED_AT.isNull)
            )
            .fetch()
    }

    @Transactional(readOnly = true)
    fun findByCampaignId(campaignId: Long): Result<CancelAnswerRecord> {
        return dsl
            .selectFrom(CANCEL_ANSWER)
            .where(
                CANCEL_ANSWER.CAMPAIGN_ID.eq(campaignId)
                    .and(CANCEL_ANSWER.DELETED_AT.isNull)
            )
            .fetch()
    }

    @Transactional(readOnly = true)
    fun findOne(id: Long): CancelAnswerRecord? {
        return dsl
            .selectFrom(CANCEL_ANSWER)
            .where(
                CANCEL_ANSWER.ID.eq(id)
                    .and(CANCEL_ANSWER.DELETED_AT.isNull)
            )
            .fetchOne()
    }

    fun deleteById(id: Long) {
        update(id) { update ->
            update
                .set(CANCEL_ANSWER.UPDATED_AT, DSL.currentOffsetDateTime())
                .set(CANCEL_ANSWER.DELETED_AT, DSL.currentOffsetDateTime())
        }
    }
}
