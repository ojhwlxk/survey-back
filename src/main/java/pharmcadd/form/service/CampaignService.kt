package pharmcadd.form.service

import org.jooq.DSLContext
import org.jooq.Field
import org.jooq.UpdateSetMoreStep
import org.jooq.impl.DSL
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import pharmcadd.form.common.Constants
import pharmcadd.form.common.exception.BadRequest
import pharmcadd.form.common.exception.NotFound
import pharmcadd.form.common.extension.plus
import pharmcadd.form.common.extension.zoneId
import pharmcadd.form.common.extension.zoneOffset
import pharmcadd.form.common.service.MailService
import pharmcadd.form.common.service.TemplateService
import pharmcadd.form.controller.admin.form.AdminCampaignForm
import pharmcadd.form.jooq.Tables.*
import pharmcadd.form.jooq.enums.*
import pharmcadd.form.jooq.tables.pojos.User
import pharmcadd.form.jooq.tables.records.AnswerRecord
import pharmcadd.form.jooq.tables.records.CampaignRecord
import pharmcadd.form.jooq.tables.records.ParticipantRecord
import pharmcadd.form.jooq.tables.records.UserRecord
import pharmcadd.form.model.AnswerVo
import java.time.LocalDateTime
import java.time.OffsetDateTime

@Service
@Transactional
class CampaignService {

    @Autowired
    lateinit var dsl: DSLContext

    @Autowired
    lateinit var formService: FormService

    @Autowired
    lateinit var participantService: ParticipantService

    @Autowired
    lateinit var formScheduleParticipantService: FormScheduleParticipantService

    @Autowired
    lateinit var timeZoneService: TimeZoneService

    @Autowired
    lateinit var userService: UserService

    @Autowired
    lateinit var mailService: MailService

    @Autowired
    lateinit var templateService: TemplateService

    data class AddGroup(val groupId: Long, val includeSubgroup: Boolean)

    fun addBySchedule(formScheduleId: Long) {
        val (form, schedule) = dsl
                .select(
                        *FORM.fields()
                )
                .select(
                        *FORM_SCHEDULE.fields()
                )
                .from(FORM_SCHEDULE)
                .join(FORM).on(FORM.ID.eq(FORM_SCHEDULE.FORM_ID).and(FORM.DELETED_AT.isNull))
                .where(
                        FORM_SCHEDULE.ID.eq(formScheduleId)
                                .and(FORM_SCHEDULE.DELETED_AT.isNull)
                )
                .fetchOne { it.into(FORM) to it.into(FORM_SCHEDULE) }!!

        val adminUser = dsl
                .selectFrom(USER)
                .where(
                        USER.ROLE.eq(UserRole.ADMIN)
                                .and(USER.DELETED_AT.isNull)
                )
                .orderBy(USER.UPDATED_AT.desc())
                .limit(1)
                .fetchOne { it.into(USER) }!!

        val participants = formScheduleParticipantService.findByScheduleId(formScheduleId)

        add(
                form.id,
                form.title + " " + LocalDateTime.now().format(Constants.LOCAL_DATE_FORMAT),
                form.description,
                DSL.currentOffsetDateTime(),
                when (schedule.type) {
                    FormScheduleType.MANUAL -> {
                        val endsAt = schedule.endsAt
                        if (endsAt == null) {
                            DSL.`val`(null, OffsetDateTime::class.java)
                        } else {
                            val scheduleTimeZone = timeZoneService.findOne(schedule.timeZoneId)!!
                            DSL.`val`(endsAt.atZone(scheduleTimeZone.zoneId()).toOffsetDateTime())
                        }
                    }
                    FormScheduleType.CRON -> {
                        DSL.field("now() + '${schedule.cronDuration} ms'", OffsetDateTime::class.java)
                    }
                },
                if (participants.isEmpty()) {
                    AccessModifier.PUBLIC
                } else {
                    AccessModifier.PRIVATE
                },
                CampaignStatus.RUNNING,
                adminUser.id,
                participants.filter { it.type == ParticipantType.USER }.map { it.userId!! },
                participants.filter { it.type == ParticipantType.GROUP }.map { AddGroup(it.groupId!!, it.includeSubgroup) },
                schedule.mailing,
        )
    }

    fun addByUser(form: AdminCampaignForm, createdBy: Long): Long {
        val timeZone = form.timeZoneId?.let { timeZoneService.findOne(it) }
                ?: timeZoneService.findByUserId(createdBy) ?: throw NotFound()

        val participants = form.participants

        return add(
                form.formId,
                form.title,
                form.description,
                if (form.startsAt == null) {
                    DSL.currentOffsetDateTime()
                } else {
                    DSL.`val`(form.startsAt + timeZone.zoneOffset())
                },
                form.endsAt?.let { DSL.`val`(it + timeZone.zoneOffset()) },
                if (participants.isEmpty()) {
                    AccessModifier.PUBLIC
                } else {
                    AccessModifier.PRIVATE
                },
                if (form.startsAt == null) {
                    CampaignStatus.RUNNING
                } else {
                    CampaignStatus.READY
                },
                createdBy,
                participants.filter { it.type == ParticipantType.USER }.map { it.userId!! },
                participants.filter { it.type == ParticipantType.GROUP }.map { AddGroup(it.groupId!!, it.includeSubgroup) },
                form.mailing
        )
    }

    fun add(
            formId: Long,
            title: String,
            description: String?,
            startsAt: Field<OffsetDateTime>?,
            endsAt: Field<OffsetDateTime>?,
            accessModifier: AccessModifier,
            status: CampaignStatus,
            createdBy: Long,
            participantUserIds: List<Long>,
            participantGroupIds: List<AddGroup>,
            mailing: Boolean? = true,
    ): Long {
        val campaignId = add(
                formId,
                title,
                description,
                startsAt,
                endsAt,
                accessModifier,
                status,
                createdBy
        )

        addParticipants(campaignId, participantUserIds, participantGroupIds)

        if (status == CampaignStatus.RUNNING && mailing == true) {
            sendNotifications(campaignId)
        }

        return campaignId
    }

    private fun addParticipants(
            id: Long,
            userIds: List<Long>,
            groupIds: List<AddGroup>
    ) {
        if (userIds.isNotEmpty()) {
            participantService.addUsers(id, userIds.toList())
        }

        val gIds = groupIds
                .filter { it.includeSubgroup.not() }
                .map { it.groupId }
        if (gIds.isNotEmpty()) {
            participantService.addGroups(id, gIds)
        }

        val gIds2 = groupIds
                .filter { it.includeSubgroup }
                .map { it.groupId }
        if (gIds2.isNotEmpty()) {
            participantService.addGroupsWithSubgroup(id, gIds2)
        }

        // 중복 제거 처리
        participantService.deleteDuplicateByCampaignId(id)
    }

    fun add(
            formId: Long,
            title: String,
            description: String?,
            startsAt: Field<OffsetDateTime>?,
            endsAt: Field<OffsetDateTime>?,
            accessModifier: AccessModifier,
            status: CampaignStatus,
            createdBy: Long
    ): Long {
        return dsl.insertInto(CAMPAIGN)
                .set(CAMPAIGN.FORM_ID, formId)
                .set(CAMPAIGN.TITLE, title)
                .set(CAMPAIGN.DESCRIPTION, description)
                .set(CAMPAIGN.STARTS_AT, startsAt)
                .set(CAMPAIGN.ENDS_AT, endsAt)
                .set(CAMPAIGN.ACCESS_MODIFIER, accessModifier)
                .set(CAMPAIGN.STATUS, status)
                .set(CAMPAIGN.CREATED_BY, createdBy)
                .set(CAMPAIGN.UPDATED_BY, createdBy)
                .returningResult(CAMPAIGN.ID)
                .fetchOne()!!
                .value1()!!
    }

    @Transactional(readOnly = true)
    fun findOne(id: Long): CampaignRecord? {
        return dsl
                .selectFrom(CAMPAIGN)
                .where(
                        CAMPAIGN.ID.eq(id)
                                .and(CAMPAIGN.DELETED_AT.isNull)
                )
                .fetchOne()
    }

    @Transactional(readOnly = true)
    fun answerable(id: Long, userId: Long): Boolean {
        return dsl
                .selectCount()
                .from(RESPONDENT)
                .where(
                        RESPONDENT.CAMPAIGN_ID.eq(id)
                                .and(RESPONDENT.USER_ID.eq(userId))
                                .and(RESPONDENT.DELETED_AT.isNull)
                )
                .fetchOne()!!
                .value1()!! == 0
    }

    fun answer(campaignId: Long, userId: Long, answers: List<AnswerVo>) {
        val campaign = findOne(campaignId) ?: throw NotFound()
        val formId = campaign.formId

        dsl.insertInto(RESPONDENT)
                .set(RESPONDENT.CAMPAIGN_ID, campaignId)
                .set(RESPONDENT.USER_ID, userId)
                .execute()

        val questions = formService.questions(formId)
        val optionsMap = formService.options(formId).map { it.id to it.text }.toMap()

        val baseInsert: () -> AnswerRecord = {
            AnswerRecord().also {
                it.createdBy = userId
                it.formId = formId
                it.campaignId = campaignId
            }
        }

        questions.zip(answers)
                .map { (q, a) ->
                    when (q.type) {
                        QuestionType.CHOICE_MULTIPLE -> {
                            val text = a.optionIds.map { optionsMap[it] }.joinToString(", ")
                            text to a.optionIds.map { optionId ->
                                baseInsert()
                                        .also {
                                            it.questionId = a.questionId
                                            it.optionId = optionId
                                            it.attachmentId = a.attachmentId
                                            it.text = optionsMap[optionId]!!
                                            it.createdBy = userId
                                        }
                            }
                        }
                        QuestionType.CHOICE_SINGLE -> {
                            val text = optionsMap[a.optionIds.first()]!!
                            text to listOf(
                                    baseInsert()
                                            .also {
                                                it.questionId = a.questionId
                                                it.optionId = a.optionIds.first()
                                                it.attachmentId = a.attachmentId
                                                it.text = text
                                                it.createdBy = userId
                                            }
                            )
                        }
                        QuestionType.TEXT_SHORT,
                        QuestionType.TEXT_LONG,
                        QuestionType.DATE,
                        QuestionType.DATE_TIME,
                        QuestionType.ATTACHMENT,
                        QuestionType.USER,
                        QuestionType.GROUP -> {
                            val text = a.text ?: "-"
                            text to listOf(
                                    baseInsert()
                                            .also {
                                                it.questionId = a.questionId
                                                it.optionId = null
                                                it.attachmentId = a.attachmentId
                                                it.userId = a.userId
                                                it.groupId = a.groupId
                                                it.text = text
                                                it.createdBy = userId
                                            }
                            )
                        }
                    }
                }
                .also { list ->
                    val texts = list.map { it.first }
                    dsl.insertInto(ANSWER_STAT)
                            .set(ANSWER_STAT.CAMPAIGN_ID, campaignId)
                            .set(ANSWER_STAT.USER_ID, userId)
                            .also { insert ->
                                texts.mapIndexed { i, text ->
                                    insert.set(DSL.field("ANS_${i + 1}"), text)
                                }
                            }
                            .execute()

                    val inserts = list.flatMap { it.second }
                    dsl.batchStore(inserts).execute()
                }

        // 캠페인의 access_modifier 가 private 인 경우 모든 참여자가 설문을 완료 했다면 캠페인 상태를 FINISHED 로 수정
        if (campaign.accessModifier == AccessModifier.PRIVATE) {
            val puv = PARTICIPANT_USER_VIEW.`as`("puv")

            val noRespondentCount = dsl
                    .selectCount()
                    .from(puv)
                    .leftJoin(RESPONDENT).on(
                            RESPONDENT.USER_ID.eq(puv.USER_ID)
                                    .and(RESPONDENT.CAMPAIGN_ID.eq(puv.CAMPAIGN_ID))
                                    .and(RESPONDENT.DELETED_AT.isNull)
                    )
                    .where(
                            puv.CAMPAIGN_ID.eq(campaignId)
                                    .and(RESPONDENT.USER_ID.isNull)
                    )
                    .fetchOne()!!
                    .value1()!!

            if (noRespondentCount == 0) {
                finish(campaignId, userId, false)
            }
        }
    }

    fun addParticipant(
            id: Long,
            type: ParticipantType,
            userId: Long?,
            groupId: Long?,
            includeSubgroup: Boolean = false
    ) {
        if (type == ParticipantType.GROUP) {
            addParticipants(id, emptyList(), listOf(AddGroup(groupId!!, includeSubgroup)))
        } else {
            addParticipants(id, listOf(userId!!), emptyList())
        }
    }

    @Transactional(readOnly = true)
    fun participant(id: Long, participantId: Long): ParticipantRecord? {
        return participantService.findOne(participantId)
    }

    fun deleteParticipant(id: Long, participantId: Long) {
        participantService.deleteById(participantId)
    }

    fun modifyStatus(
            id: Long,
            updatedBy: Long,
            status: CampaignStatus,
            block: (UpdateSetMoreStep<CampaignRecord>) -> Unit = {}
    ) {
        dsl.update(CAMPAIGN)
                .set(CAMPAIGN.STATUS, status)
                .set(CAMPAIGN.UPDATED_AT, DSL.currentOffsetDateTime())
                .set(CAMPAIGN.UPDATED_BY, updatedBy)
                .also(block)
                .where(
                        CAMPAIGN.ID.eq(id)
                                .and(CAMPAIGN.DELETED_AT.isNull)
                )
                .execute()
    }

    fun stop(id: Long, updatedBy: Long) {
        modifyStatus(id, updatedBy, CampaignStatus.STOPPED)
    }

    fun pause(id: Long, updatedBy: Long) {
        val campaign = findOne(id) ?: throw NotFound()
        if (campaign.status != CampaignStatus.RUNNING) throw BadRequest()

        modifyStatus(id, updatedBy, CampaignStatus.SUSPENDED)
    }

    fun resume(id: Long, updatedBy: Long) {
        val campaign = findOne(id) ?: throw NotFound()
        if (campaign.status != CampaignStatus.SUSPENDED) throw BadRequest()

        modifyStatus(id, updatedBy, CampaignStatus.RUNNING)
    }

    fun run(id: Long, updatedBy: Long) {
        val campaign = findOne(id) ?: throw NotFound()
        val executableStatus = listOf(CampaignStatus.READY, CampaignStatus.SUSPENDED, CampaignStatus.STOPPED)
        if (executableStatus.contains(campaign.status).not()) throw BadRequest()

        modifyStatus(id, updatedBy, CampaignStatus.RUNNING) { query ->
            // 이전 상태가 READY 였다면 시작 시간 갱신
            if (campaign.status == CampaignStatus.READY && campaign.startsAt == null) {
                query.set(CAMPAIGN.STARTS_AT, DSL.currentOffsetDateTime())
            }
        }

        // 이전 상태가 READY 인 경우에만
        if (campaign.status == CampaignStatus.READY) {
            sendNotifications(id)
        }
    }

    /***
     * @param expired true 면 시간 만료, false 면 모든 참여자 참여 완료
     */
    fun finish(id: Long, updatedBy: Long, expired: Boolean = false) {
        val campaign = findOne(id) ?: throw NotFound()
        if (campaign.status != CampaignStatus.RUNNING) throw BadRequest()

        modifyStatus(id, updatedBy, CampaignStatus.FINISHED) { query ->
//            finished_at 컬럼을 따로...?
//            if (campaign.endsAt == null) {
//                query.set(CAMPAIGN.ENDS_AT, DSL.currentOffsetDateTime())
//            }
        }

        val notifications = notifications(id)
        if (notifications.isNotEmpty()) {
            val formId = campaign.formId
            val questions = formService.questions(formId)

            // TODO : 통계 html 생성 후 메일 노티
        }
    }

    /***
     * "최초 실행 시" 노티를 보내도록 설계되어 있다.
     */
    data class NotiForm(
            val title : String,
            val description : String?,
            val linkUrl : String,
            val duration : String,
    )

    @Transactional(readOnly = true)
    fun sendNotifications(campaignId: Long) {
        // NOTE : 만약에 이전 상태가 SUSPENDED 나 STOPPED 인 경우에는, 설문 완료한 사용자는 제외 하고 보내도록 수정
        val users = dsl
                .select(
                        USER.EMAIL,
                        USER.NAME,
                )
                .from(PARTICIPANT_USER_VIEW)
                .join(USER).on(PARTICIPANT_USER_VIEW.USER_ID.eq(USER.ID).and(USER.DELETED_AT.isNull))
                .where(
                        PARTICIPANT_USER_VIEW.CAMPAIGN_ID.eq(campaignId)
                )
                .fetch{ it.into(User::class.java)}

        if (users.isEmpty()) return

        val campaignRecord = findOne(campaignId)!!

        val startsAt = campaignRecord.startsAt!!.toLocalDate().format(Constants.LOCAL_DATE_FORMAT)
        val endsAt = campaignRecord.endsAt?.toLocalDate()?.format(Constants.LOCAL_DATE_FORMAT) ?: ' '
        @Suppress("HttpUrlsUsage")
        val linkUrl = "http://form.pharmcadd.com/campaigns/$campaignId"

        val notiForm = NotiForm(
                    title = campaignRecord.title,
                    description = campaignRecord.description ?: "-",
                    linkUrl = linkUrl,
                    duration = "$startsAt ~ $endsAt",
        )

        for (user in users) {
            val content = templateService.compile("template/campaignToAnswer.html.hbs", mapOf("notiForm" to notiForm, "name" to user.name))
            mailService.sendAndForget {
                title("[Pharmcadd] 설문조사 - ${campaignRecord.title}")
                to(user.email)
                content(content)
            }
        }
    }

    @Transactional(readOnly = true)
    fun notifications(campaignId: Long): List<UserRecord> {
        return dsl
                .select(
                        *USER.fields()
                )
                .from(CAMPAIGN)
                .join(FORM_NOTIFICATION)
                .on(FORM_NOTIFICATION.FORM_ID.eq(CAMPAIGN.FORM_ID).and(FORM_NOTIFICATION.DELETED_AT.isNull))
                .join(USER).on(FORM_NOTIFICATION.USER_ID.eq(USER.ID).and(USER.DELETED_AT.isNull))
                .where(
                        CAMPAIGN.ID.eq(campaignId)
                                .and(CAMPAIGN.DELETED_AT.isNull)
                )
                .fetch { it.into(USER) }
    }
}
