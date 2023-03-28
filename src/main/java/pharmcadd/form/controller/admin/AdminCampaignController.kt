package pharmcadd.form.controller.admin

import org.apache.commons.compress.utils.IOUtils
import org.jooq.impl.DSL
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.*
import pharmcadd.form.common.Constants
import pharmcadd.form.common.controller.BaseController
import pharmcadd.form.common.exception.NotFound
import pharmcadd.form.common.extension.*
import pharmcadd.form.common.util.excel.generateWorkBook
import pharmcadd.form.common.util.pagination.DataTableForm
import pharmcadd.form.common.util.pagination.DataTablePagination
import pharmcadd.form.controller.admin.form.*
import pharmcadd.form.jooq.Tables.*
import pharmcadd.form.jooq.enums.AccessModifier
import pharmcadd.form.jooq.enums.ParticipantType
import pharmcadd.form.jooq.tables.pojos.Campaign
import pharmcadd.form.jooq.tables.pojos.CancelAnswer
import pharmcadd.form.model.AnswerStatVo
import pharmcadd.form.model.ParticipantVo
import pharmcadd.form.service.*
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.time.LocalDateTime
import javax.validation.Valid


@RestController
@RequestMapping("/admin/campaigns")
class AdminCampaignController : BaseController() {

    @Autowired
    lateinit var campaignService: CampaignService

    @Autowired
    lateinit var cancelAnswerService: CancelAnswerService

    @Autowired
    lateinit var userService: UserService

    @Autowired
    lateinit var timeZoneService: TimeZoneService

    @Autowired
    lateinit var formService: FormService

    @GetMapping
    fun list(form: AdminCampaignListForm): DataTablePagination<Campaign> {
        val query = dsl
            .select(
                *CAMPAIGN.fields()
            )
            .from(CAMPAIGN)
            .where(
                CAMPAIGN.DELETED_AT.isNull
            )

        if (form.keyword != null) {
            query.and(CAMPAIGN.TITLE.contains(form.keyword))
        }
        if (form.status != null) {
            query.and(CAMPAIGN.STATUS.eq(form.status))
        }
        if (form.formId != null) {
            query.and(CAMPAIGN.FORM_ID.eq(form.formId))
        }
        val rangeFrom = form.rangeFrom
        val rangeTo = form.rangeTo
        if (rangeFrom != null || rangeTo != null) {
            val timeZone = timeZoneService.findByUserId(securityService.userId)!!
            val zoneOffset = timeZone.zoneOffset()
            if (rangeFrom != null) {
                query.and(
                    CAMPAIGN.STARTS_AT.lessOrEqual(rangeFrom.withZeroTime() + zoneOffset)
                        .or(CAMPAIGN.STARTS_AT.isNull)
                )
            }
            if (rangeTo != null) {
                query.and(
                    CAMPAIGN.ENDS_AT.greaterOrEqual(rangeTo.withLastTime() + zoneOffset)
                        .or(CAMPAIGN.ENDS_AT.isNull)
                )
            }
        }

        return DataTablePagination.of(dsl, query, form) {
            it.into(Campaign::class.java)
        }
    }

    @GetMapping("/{id}")
    fun view(@PathVariable("id") id: Long): Campaign {
        return campaignService.findOne(id)?.into(Campaign::class.java) ?: throw NotFound()
    }

    @PostMapping
    fun add(@RequestBody form: AdminCampaignForm): Campaign {
        val id: Long = campaignService.addByUser(form, securityService.userId)
        return campaignService.findOne(id)?.into(Campaign::class.java) ?: throw NotFound()
    }

    @PatchMapping("/{id}/stop")
    fun stop(@PathVariable("id") id: Long) {
        campaignService.stop(id, securityService.userId)
    }

    @PatchMapping("/{id}/pause")
    fun pause(@PathVariable("id") id: Long) {
        campaignService.pause(id, securityService.userId)
    }

    @PatchMapping("/{id}/resume")
    fun resume(@PathVariable("id") id: Long) {
        campaignService.resume(id, securityService.userId)
    }

    @PatchMapping("/{id}/run")
    fun run(@PathVariable("id") id: Long) {
        campaignService.run(id, securityService.userId)
    }

    @PatchMapping("/{id}")
    fun patch(
        @PathVariable("id") id: Long,
        @RequestBody param: Map<String, String> = mapOf()
    ) {
        if (param.isEmpty()) return

        val userId = securityService.userId

        val title = param["title"]
        val description = param["description"]
        val endsAt = param["endsAt"]?.let { LocalDateTime.parse(it, Constants.LOCAL_DATE_TIME_FORMAT) }

        val update = dsl.update(CAMPAIGN)
        if (title != null) {
            update.set(CAMPAIGN.TITLE, title)
        }
        if (description != null) {
            update.set(CAMPAIGN.DESCRIPTION, description)
        }
        if (endsAt != null) {
            val timeZoneId = timeZoneService.findByUserId(userId) ?: throw NotFound()
            update.set(CAMPAIGN.ENDS_AT, endsAt + timeZoneId.zoneOffset())
        }

        update
            .set(CAMPAIGN.UPDATED_AT, DSL.currentOffsetDateTime())
            .set(CAMPAIGN.UPDATED_BY, userId)
            .where(
                CAMPAIGN.ID.eq(id)
                    .and(CAMPAIGN.DELETED_AT.isNull)
            )
            .execute()
    }

    // participant

    @GetMapping("/{id}/participants")
    fun participants(@PathVariable("id") id: Long, form: DataTableForm): DataTablePagination<ParticipantListFormModel> {
        val query = dsl
            .select(
                *PARTICIPANT.fields()
            )
            .select(
                USER.NAME.`as`(ParticipantListFormModel::userName.name.upperCamelToLowerUnderscore()),
                GROUP.NAME.`as`(ParticipantListFormModel::groupName.name.upperCamelToLowerUnderscore()),
            )
            .from(PARTICIPANT)
            .leftJoin(USER).on(
                PARTICIPANT.USER_ID.eq(USER.ID)
                    .and(PARTICIPANT.TYPE.eq(ParticipantType.USER))
                    .and(USER.DELETED_AT.isNull)
            )
            .leftJoin(GROUP).on(
                PARTICIPANT.GROUP_ID.eq(GROUP.ID)
                    .and(PARTICIPANT.TYPE.eq(ParticipantType.GROUP))
                    .and(GROUP.DELETED_AT.isNull)
            )
            .where(
                PARTICIPANT.CAMPAIGN_ID.eq(id)
                    .and(PARTICIPANT.DELETED_AT.isNull)
            )

        return DataTablePagination.of(dsl, query, form) {
            it.into(ParticipantListFormModel::class.java)
        }
    }

    @PostMapping("/{id}/participants")
    fun addParticipant(@PathVariable("id") id: Long, @RequestBody @Valid form: ParticipantVo) {
        campaignService.addParticipant(id, form.type, form.userId, form.groupId, form.includeSubgroup)
    }

    @DeleteMapping("/{id}/participants/{participantId}")
    fun deleteParticipant(@PathVariable("id") id: Long, @PathVariable("participantId") participantId: Long) {
        campaignService.deleteParticipant(id, participantId)
    }

    // answers

    @GetMapping("/{id}/answers")
    fun answers(
        @PathVariable("id") id: Long,
        form: AdminCampaignAnswerStatListForm
    ): DataTablePagination<AnswerStatVo> {
        val campaign = campaignService.findOne(id) ?: throw NotFound()

        val c = CAMPAIGN.`as`("c")
        val u = when (campaign.accessModifier) {
            AccessModifier.PRIVATE -> {
                dsl
                    .select(
                        PARTICIPANT_USER_VIEW.CAMPAIGN_ID,
                        PARTICIPANT_USER_VIEW.USER_ID,
                        USER.NAME.`as`("user_name")
                    )
                    .from(PARTICIPANT_USER_VIEW)
                    .join(USER).on(PARTICIPANT_USER_VIEW.USER_ID.eq(USER.ID))
                    .where(
                        PARTICIPANT_USER_VIEW.CAMPAIGN_ID.eq(id)
                    )
            }
            AccessModifier.PUBLIC -> {
                dsl
                    .select(
                        DSL.`val`(id).`as`("campaign_id"),
                        USER.ID.`as`("user_id"),
                        USER.NAME.`as`("user_name")
                    )
                    .from(USER)
                    .where(
                        USER.DELETED_AT.isNull
                    )
            }
        }.asTable("u")

        val campaignIdField = u.field("campaign_id", Long::class.java)!!
        val userIdField = u.field("user_id", Long::class.java)!!
        val userNameField = u.field("user_name", String::class.java)!!

        val query = dsl
            .select(
                *ANSWER_STAT.fieldsExcludes(ANSWER_STAT.CAMPAIGN_ID, ANSWER_STAT.USER_ID)
            )
            .select(
                campaignIdField,
                userIdField,
                userNameField
            )
            .from(c)
            .join(u).on(c.ID.eq(campaignIdField))
            .leftJoin(ANSWER_STAT).on(
                ANSWER_STAT.CAMPAIGN_ID.eq(c.ID)
                    .and(ANSWER_STAT.USER_ID.eq(userIdField))
                    .and(ANSWER_STAT.DELETED_AT.isNull)
            )
            .where(
                c.ID.eq(id)
                    .and(c.DELETED_AT.isNull)
            )

        val respondentType = form.respondentType
        if (respondentType != null) {
            when (respondentType) {
                AdminCampaignAnswerStatListForm.RespondentType.RESPONDENT -> {
                    query.and(ANSWER_STAT.ID.greaterThan(0))
                }
                AdminCampaignAnswerStatListForm.RespondentType.NONRESPONDENT -> {
                    query.and(ANSWER_STAT.ID.isNull)
                }
            }
        }
        if (form.keyword != null) {
            query.and(userNameField.contains(form.keyword))
        }

        return DataTablePagination.of(dsl, query, form) {
            AnswerStatVo(
                it.getValue(ANSWER_STAT.ID),
                it.getValue(campaignIdField),
                it.getValue(userIdField),
                it.getValue(userNameField),
                it.getValue(ANSWER_STAT.ANS_1),
                it.getValue(ANSWER_STAT.ANS_2),
                it.getValue(ANSWER_STAT.ANS_3),
                it.getValue(ANSWER_STAT.ANS_4),
                it.getValue(ANSWER_STAT.ANS_5),
                it.getValue(ANSWER_STAT.ANS_6),
                it.getValue(ANSWER_STAT.ANS_7),
                it.getValue(ANSWER_STAT.ANS_8),
                it.getValue(ANSWER_STAT.ANS_9),
                it.getValue(ANSWER_STAT.ANS_10),
                it.getValue(ANSWER_STAT.ANS_11),
                it.getValue(ANSWER_STAT.ANS_12),
                it.getValue(ANSWER_STAT.ANS_13),
                it.getValue(ANSWER_STAT.ANS_14),
                it.getValue(ANSWER_STAT.ANS_15),
                it.getValue(ANSWER_STAT.ANS_16),
                it.getValue(ANSWER_STAT.ANS_17),
                it.getValue(ANSWER_STAT.ANS_18),
                it.getValue(ANSWER_STAT.ANS_19),
                it.getValue(ANSWER_STAT.ANS_20),
                it.getValue(ANSWER_STAT.CREATED_AT),
                it.getValue(ANSWER_STAT.UPDATED_AT),
                it.getValue(ANSWER_STAT.DELETED_AT),
            )
        }
    }

    @GetMapping("/{id}/answer-cancels")
    fun cancelAnswers(@PathVariable id: Long, form: DataTableForm): DataTablePagination<CancelAnswer> {
        val query = dsl
            .selectFrom(CANCEL_ANSWER)
            .where(
                CANCEL_ANSWER.DELETED_AT.isNull
            )
        return DataTablePagination.of(dsl, query, form) {
            it.into(CancelAnswer::class.java)
        }
    }

    @PatchMapping("/{id}/answer-cancels/{answerCancelId}/approve")
    fun cancelAnswerApprove(
        @PathVariable("id") id: Long,
        @PathVariable("answerCancelId") answerCancelId: Long,
        @RequestBody @Valid form: AdminCancelAnswerApprovalForm
    ) {
        cancelAnswerService.approve(answerCancelId, securityService.userId, form.answer)
    }

    @PatchMapping("/{id}/answer-cancels/{answerCancelId}/reject")
    fun cancelAnswerReject(
        @PathVariable("id") id: Long,
        @PathVariable("answerCancelId") answerCancelId: Long,
        @RequestBody @Valid form: AdminCancelAnswerApprovalForm
    ) {
        cancelAnswerService.reject(answerCancelId, securityService.userId, form.answer)
    }

    @GetMapping("/{id}/answers/excel")
    @Throws(IOException::class)
    fun answerExcelDownload(@PathVariable("id") id: Long) {
        val campaignRecord = campaignService.findOne(id)!!
        val questions = formService.questions(campaignRecord.formId)
        val answerStat = dsl.select(
            *ANSWER_STAT.fields()
        )
            .select(
                USER.NAME.`as`("userName")
            )
            .from(ANSWER_STAT)
            .join(USER)
            .on(
                USER.ID.eq(ANSWER_STAT.USER_ID)
                    .and(USER.DELETED_AT.isNull)
            )
            .and(ANSWER_STAT.DELETED_AT.isNull)
            .where(
                ANSWER_STAT.CAMPAIGN_ID.eq(id)
            )
            .fetch()

        val headerForm = questions.map { it["title"].toString() }
        headerForm.add(0, "설문자")
        val answers = answerStat.map { record ->
            val list = (1 until headerForm.size)
                .map {
                    record["ans_${it}"].toString()
                }.toMutableList()
            list.add(0, record["userName"].toString())
            list
        }

        response.apply {
            this.setHeader("Content-disposition", "attachment;filename=\"${campaignRecord.title}.xls\"")
            this.contentType = "application/vnd.ms-excel"
            this.characterEncoding = StandardCharsets.UTF_8.toString()
        }

        val wb = generateWorkBook {
            sheet {
                row {
                    headerForm.mapIndexed { index, value ->
                        cell(value, index)
                    }
                }
                answers.map { answer ->
                    row {
                        answer.forEachIndexed { index, value ->
                            cell(value, index)
                        }
                    }
                }
            }
            generate(response.outputStream)
        }
        wb.close()
        response.flushBuffer()
    }
}



