package pharmcadd.form.controller.front

import org.jooq.Record
import org.jooq.SelectConditionStep
import org.jooq.SelectHavingStep
import org.jooq.impl.DSL
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.*
import pharmcadd.form.common.controller.BaseController
import pharmcadd.form.common.exception.AccessDenied
import pharmcadd.form.common.exception.BadRequest
import pharmcadd.form.common.exception.NotFound
import pharmcadd.form.common.extension.upperCamelToLowerUnderscore
import pharmcadd.form.common.util.pagination.DataTablePagination
import pharmcadd.form.controller.front.form.AnswerForm
import pharmcadd.form.controller.front.form.AnswerCancelForm
import pharmcadd.form.controller.front.form.CampaignListForm
import pharmcadd.form.jooq.Tables.*
import pharmcadd.form.jooq.enums.AccessModifier
import pharmcadd.form.jooq.enums.CampaignStatus
import pharmcadd.form.jooq.tables.pojos.Campaign
import pharmcadd.form.model.AnswerVo
import pharmcadd.form.model.CancelAnswerVo
import pharmcadd.form.service.*
import javax.validation.Valid

@RestController
@RequestMapping("/campaigns")
class CampaignController : BaseController() {

    @Autowired
    lateinit var groupService: GroupService

    @Autowired
    lateinit var campaignService: CampaignService

    @Autowired
    lateinit var cancelAnswerService: CancelAnswerService

    @Autowired
    lateinit var timeZoneService: TimeZoneService

    @Autowired
    lateinit var userService: UserService

    @GetMapping
    fun list(form: CampaignListForm): DataTablePagination<Campaign> {
        val query = when (form.type ?: CampaignListForm.Type.ALL) {
            CampaignListForm.Type.ALL -> allCampaigns(form)
            CampaignListForm.Type.READY -> readyCampaigns(form)
            CampaignListForm.Type.COMPLETED -> completedCampaigns(form)
        }

        return DataTablePagination.of(dsl, query, form) {
            it.into(Campaign::class.java)
        }
    }

    fun allCampaigns(form: CampaignListForm): SelectConditionStep<Record> {
        val userId = securityService.userId
        val groupIds = groupService.findByUserId(userId).map { it.id }

        val query = dsl
            .select(
                *CAMPAIGN.fields()
            )
            .from(CAMPAIGN)
            .leftJoin(PARTICIPANT).on(CAMPAIGN.ID.eq(PARTICIPANT.CAMPAIGN_ID).and(PARTICIPANT.DELETED_AT.isNull))
            .where(
                CAMPAIGN.DELETED_AT.isNull
            )

        if (form.status != null) {
            query.and(CAMPAIGN.STATUS.eq(form.status))
        }
        if (form.keyword != null) {
            query.and(CAMPAIGN.TITLE.contains(form.keyword))
        }

        query
            .and(
                CAMPAIGN.ACCESS_MODIFIER.eq(AccessModifier.PUBLIC)
                    .or(
                        CAMPAIGN.ACCESS_MODIFIER.eq(AccessModifier.PRIVATE)
                            .and(
                                PARTICIPANT.USER_ID.eq(userId)
                                    .or(PARTICIPANT.GROUP_ID.`in`(groupIds))
                            )
                    )
            )
            .groupBy(*CAMPAIGN.fields())

        return query
    }

    fun readyCampaigns(form: CampaignListForm): SelectHavingStep<Record> {
        val userId = securityService.userId
        val groupIds = groupService.findByUserId(userId).map { it.id }

        val subQuery = dsl
            .select(
                CAMPAIGN.ID.`as`("campaign_id"),
                DSL.count(RESPONDENT.ID).`as`("count")
            )
            .from(CAMPAIGN)
            .leftJoin(RESPONDENT).on(
                CAMPAIGN.ID.eq(RESPONDENT.CAMPAIGN_ID)
                    .and(RESPONDENT.USER_ID.eq(userId))
                    .and(RESPONDENT.DELETED_AT.isNull)
            )
            .where(
                CAMPAIGN.STATUS.eq(CampaignStatus.RUNNING)
                    .and(CAMPAIGN.DELETED_AT.isNull)
            )
            .groupBy(CAMPAIGN.ID)

        val query = dsl
            .select(
                *CAMPAIGN.fields()
            )
            .from(CAMPAIGN)
            .join(subQuery).on(
                CAMPAIGN.ID.eq(subQuery.field("campaign_id", Long::class.java)!!)
                    .and(subQuery.field("count", Int::class.java)!!.eq(0))
            )
            .leftJoin(PARTICIPANT).on(CAMPAIGN.ID.eq(PARTICIPANT.CAMPAIGN_ID).and(PARTICIPANT.DELETED_AT.isNull))
            .where(
                CAMPAIGN.ACCESS_MODIFIER.eq(AccessModifier.PUBLIC)
                    .or(
                        CAMPAIGN.ACCESS_MODIFIER.eq(AccessModifier.PRIVATE)
                            .and(
                                PARTICIPANT.USER_ID.eq(userId)
                                    .or(PARTICIPANT.GROUP_ID.`in`(groupIds))
                            )
                    )
            )

        if (form.keyword != null) {
            query.and(CAMPAIGN.TITLE.contains(form.keyword))
        }

        query.groupBy(*CAMPAIGN.fields())

        return query
    }

    fun completedCampaigns(form: CampaignListForm): SelectHavingStep<Record> {
        val userId = securityService.userId

        val query = dsl
            .select(
                *CAMPAIGN.fields()
            )
            .from(CAMPAIGN)
            .join(RESPONDENT).on(
                CAMPAIGN.ID.eq(RESPONDENT.CAMPAIGN_ID)
                    .and(RESPONDENT.USER_ID.eq(userId))
                    .and(RESPONDENT.DELETED_AT.isNull)
            )
            .where(
                CAMPAIGN.DELETED_AT.isNull
            )

        if (form.status != null) {
            query.and(CAMPAIGN.STATUS.eq(form.status))
        }
        if (form.keyword != null) {
            query.and(CAMPAIGN.TITLE.contains(form.keyword))
        }

        query.groupBy(*CAMPAIGN.fields())

        return query
    }

    @GetMapping("/{id}")
    fun view(@PathVariable("id") id: Long): Campaign {
        return campaignService.findOne(id)?.into(Campaign::class.java) ?: throw NotFound()
    }

    @PostMapping("/{id}/answer")
    fun addAnswer(@PathVariable("id") id: Long, @RequestBody @Valid form: AnswerForm) {
        val campaign = campaignService.findOne(id) ?: throw NotFound()
        val userId = securityService.userId
        val campaignId = campaign.id

        if (campaignService.answerable(campaignId, userId).not()) {
            throw BadRequest()
        }

        campaignService.answer(campaignId, userId, form.answer)
    }

    @GetMapping("/{id}/answer")
    fun answer(@PathVariable("id") id: Long): List<AnswerVo> {
        return dsl
            .select(
                *ANSWER.fields()
            )
            .select(
                *QUESTION.fields()
            )
            .from(ANSWER)
            .join(QUESTION).on(QUESTION.ID.eq(ANSWER.QUESTION_ID).and(ANSWER.DELETED_AT.isNull))
            .where(
                ANSWER.CAMPAIGN_ID.eq(id)
                    .and(ANSWER.CREATED_BY.eq(securityService.userId))
            )
            .orderBy(
                QUESTION.ORDER.asc(),
                ANSWER.ID.asc()
            )
            .fetchGroups(QUESTION.ID)
            .map { (questionId, records) ->
                val first = records.first()!!
                AnswerVo(
                    questionId,
                    records.map { it.get(ANSWER.ID) },
                    first.get(ANSWER.ATTACHMENT_ID),
                    first.get(ANSWER.USER_ID),
                    first.get(ANSWER.GROUP_ID),
                    records.map { it.get(ANSWER.TEXT) }.joinToString(", ")
                )
            }
    }

    @GetMapping("/{id}/answer-cancels")
    fun cancelAnswers(@PathVariable id: Long): List<CancelAnswerVo> {
        val r = USER.`as`("r")
        val a = USER.`as`("a")

        return dsl
            .select(
                *CANCEL_ANSWER.fields()
            )
            .select(
                r.NAME.`as`(CancelAnswerVo::requesterName.name.upperCamelToLowerUnderscore()),
                a.NAME.`as`(CancelAnswerVo::approverName.name.upperCamelToLowerUnderscore()),
            )
            .from(CANCEL_ANSWER)
            .leftJoin(r).on(CANCEL_ANSWER.USER_ID.eq(r.ID).and(r.DELETED_AT.isNull))
            .leftJoin(a).on(CANCEL_ANSWER.APPROVED_BY.eq(a.ID).and(a.DELETED_AT.isNull))
            .where(
                CANCEL_ANSWER.CAMPAIGN_ID.eq(id)
                    .and(CANCEL_ANSWER.USER_ID.eq(securityService.userId))
                    .and(CANCEL_ANSWER.DELETED_AT.isNull)
            )
            .fetch { it.into(CancelAnswerVo::class.java) }
    }

    @PostMapping("/{id}/answer-cancels")
    fun addAnswerCancel(@PathVariable id: Long, @RequestBody form: AnswerCancelForm): Long {
        return cancelAnswerService.addRequest(id, securityService.userId, form.reason)
    }

    @DeleteMapping("/{id}/answer-cancels/{answerCancelId}")
    fun deleteAnswerCancel(
        @PathVariable("id") id: Long,
        @PathVariable("answerCancelId") answerCancelId: Long,
    ) {
        val answer = cancelAnswerService.findOne(answerCancelId) ?: throw NotFound()
        if (answer.userId != securityService.userId) throw AccessDenied()
        cancelAnswerService.deleteById(answerCancelId)
    }
}
