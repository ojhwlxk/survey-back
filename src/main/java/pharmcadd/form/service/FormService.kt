package pharmcadd.form.service

import org.jooq.DSLContext
import org.jooq.InsertSetMoreStep
import org.jooq.Result
import org.jooq.UpdateConditionStep
import org.jooq.impl.DSL
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import pharmcadd.form.jooq.Sequences
import pharmcadd.form.jooq.Tables.*
import pharmcadd.form.jooq.tables.records.FormNotificationRecord
import pharmcadd.form.jooq.tables.records.FormRecord
import pharmcadd.form.jooq.tables.records.QuestionOptionRecord
import pharmcadd.form.jooq.tables.records.QuestionRecord
import pharmcadd.form.model.FormVo

@Service
class FormService {

    @Autowired
    lateinit var dsl: DSLContext

    @Autowired
    lateinit var formNotificationService: FormNotificationService

    @Autowired
    lateinit var formScheduleService: FormScheduleService

    @Autowired
    lateinit var formScheduleParticipantService: FormScheduleParticipantService

    fun save(form: FormVo, loginUserId: Long): Long {
        val questionIds = form.questions.map { it.id }
        val optionIds = form.questions.flatMap { q -> q.options.map { it.id } }

        val isNew = form.id == null

        // 1. 제거된 문항과 보기를 삭제
        if (isNew.not()) {
            val prevQuestionIds = dsl
                .select(
                    QUESTION.ID
                )
                .from(QUESTION)
                .where(
                    QUESTION.FORM_ID.eq(form.id)
                        .and(QUESTION.DELETED_AT.isNull)
                )
                .fetch { it.get(QUESTION.ID) }

            val prevOptionIds = dsl
                .select(
                    QUESTION_OPTION.ID
                )
                .from(QUESTION_OPTION)
                .where(
                    QUESTION_OPTION.FORM_ID.eq(form.id)
                        .and(QUESTION_OPTION.DELETED_AT.isNull)
                )
                .fetch { it.get(QUESTION_OPTION.ID) }

            @Suppress("ConvertArgumentToSet")
            val removedQuestionIds = prevQuestionIds - questionIds.filterNotNull()

            @Suppress("ConvertArgumentToSet")
            val removedOptionIds = prevOptionIds - optionIds.filterNotNull()

            if (removedQuestionIds.isNotEmpty()) {
                dsl.update(QUESTION)
                    .set(QUESTION.DELETED_AT, DSL.currentOffsetDateTime())
                    .where(
                        QUESTION.ID.`in`(removedQuestionIds)
                            .and(QUESTION.DELETED_AT.isNull)
                    )
                    .execute()
            }

            if (removedOptionIds.isNotEmpty()) {
                dsl.update(QUESTION_OPTION)
                    .set(QUESTION_OPTION.DELETED_AT, DSL.currentOffsetDateTime())
                    .where(
                        QUESTION_OPTION.ID.`in`(removedQuestionIds)
                            .and(QUESTION_OPTION.DELETED_AT.isNull)
                    )
                    .execute()
            }
        }

        // 폼의 생성/수정
        val formId = if (isNew) {
            dsl.insertInto(FORM)
                .set(FORM.TITLE, form.title)
                .set(FORM.DESCRIPTION, form.description)
                .set(FORM.CREATED_BY, loginUserId)
                .set(FORM.UPDATED_BY, loginUserId)
                .returningResult(FORM.ID)
                .fetchOne()!!
                .value1()!!
        } else {
            dsl.update(FORM)
                .set(FORM.TITLE, form.title)
                .set(FORM.DESCRIPTION, form.description)
                .set(FORM.UPDATED_BY, loginUserId)
                .set(FORM.UPDATED_AT, DSL.currentOffsetDateTime())
                .where(
                    FORM.ID.eq(form.id)
                        .and(FORM.DELETED_AT.isNull)
                )
                .execute()
            form.id!!
        }

        // 2. 문항의 id 가 없으면 신규 insert, 있으면 update
        val newQuestionIds = dsl.nextvals(Sequences.QUESTION_ID_SEQ, questionIds.map { it == null }.size).listIterator()
        val newOptionIds =
            dsl.nextvals(Sequences.QUESTION_OPTION_ID_SEQ, optionIds.map { it == null }.size).listIterator()

        val insertQuestions = mutableListOf<InsertSetMoreStep<QuestionRecord>>()
        val updateQuestions = mutableListOf<UpdateConditionStep<QuestionRecord>>()
        val insertOptions = mutableListOf<InsertSetMoreStep<QuestionOptionRecord>>()
        val updateOptions = mutableListOf<UpdateConditionStep<QuestionOptionRecord>>()

        for ((i, q) in form.questions.withIndex()) {
            val questionId = q.id ?: newQuestionIds.next()
            if (q.id == null) {
                insertQuestions.add(
                    dsl.insertInto(QUESTION)
                        .set(QUESTION.ID, questionId)
                        .set(QUESTION.FORM_ID, formId)
                        .set(QUESTION.TITLE, q.title)
                        .set(QUESTION.ABBR, q.abbr)
                        .set(QUESTION.TYPE, q.type)
                        .set(QUESTION.REQUIRED, q.required)
                        .set(QUESTION.ORDER, i + 1)
                )
            } else {
                updateQuestions.add(
                    dsl.update(QUESTION)
                        .set(QUESTION.FORM_ID, formId)
                        .set(QUESTION.TITLE, q.title)
                        .set(QUESTION.ABBR, q.abbr)
                        .set(QUESTION.TYPE, q.type)
                        .set(QUESTION.REQUIRED, q.required)
                        .set(QUESTION.ORDER, i + 1)
                        .set(QUESTION.UPDATED_AT, DSL.currentOffsetDateTime())
                        .where(
                            QUESTION.ID.eq(q.id)
                                .and(QUESTION.DELETED_AT.isNull)
                        )
                )
            }
            for ((n, o) in q.options.withIndex()) {
                val optionId = o.id ?: newOptionIds.next()
                if (o.id == null) {
                    insertOptions.add(
                        dsl.insertInto(QUESTION_OPTION)
                            .set(QUESTION_OPTION.ID, optionId)
                            .set(QUESTION_OPTION.FORM_ID, formId)
                            .set(QUESTION_OPTION.QUESTION_ID, questionId)
                            .set(QUESTION_OPTION.TEXT, o.text)
                            .set(QUESTION_OPTION.ORDER, n + 1)
                    )
                } else {
                    updateOptions.add(
                        dsl.update(QUESTION_OPTION)
                            .set(QUESTION_OPTION.QUESTION_ID, questionId)
                            .set(QUESTION_OPTION.FORM_ID, formId)
                            .set(QUESTION_OPTION.TEXT, o.text)
                            .set(QUESTION_OPTION.ORDER, n + 1)
                            .set(QUESTION_OPTION.UPDATED_AT, DSL.currentOffsetDateTime())
                            .where(
                                QUESTION_OPTION.ID.eq(o.id)
                                    .and(QUESTION_OPTION.DELETED_AT.isNull)
                            )
                    )
                }
            }
        }

        for (query in listOf(insertQuestions, updateQuestions, insertOptions, updateOptions)) {
            if (query.isNotEmpty()) {
                dsl.batch(query).execute()
            }
        }

        // FIXME: 배치 insert 처리 & 수정 처리
        if (isNew) {
            form.schedules.map { s ->
                val scheduleId = formScheduleService.add(
                    formId,
                    s.type,
                    s.timeZoneId,
                    s.startsAt,
                    s.endsAt,
                    s.cronExpression,
                    s.cronDuration,
                    s.active
                )

                s.participants.map { p ->
                    formScheduleParticipantService.add(
                        scheduleId,
                        formId,
                        p.type,
                        p.userId,
                        p.groupId,
                        p.includeSubgroup
                    )
                }
            }
        }

        return formId
    }

    @Transactional(readOnly = true)
    fun detail(id: Long): FormVo? {
        val form = findOne(id) ?: return null
        val questions: List<QuestionRecord> = questions(id)
        val optionsMap: Map<Long, List<QuestionOptionRecord>> = options(id).groupBy { it.questionId }

        return form.let { f ->
            FormVo(
                id = f.id,
                title = f.title,
                description = f.description,
                questions = questions.map { q ->
                    val options = optionsMap[q.id] ?: emptyList()
                    FormVo.QuestionVo(
                        id = q.id,
                        title = q.title,
                        abbr = q.abbr,
                        type = q.type,
                        required = q.required,
                        options = options.map { o ->
                            FormVo.QuestionVo.OptionVo(
                                id = o.id,
                                text = o.text,
                            )
                        }
                    )
                }
            )
        }
    }

    @Transactional(readOnly = true)
    fun findOne(id: Long): FormRecord? {
        return dsl
            .selectFrom(FORM)
            .where(
                FORM.ID.eq(id)
                    .and(FORM.DELETED_AT.isNull)
            )
            .fetchOne()
    }

    @Transactional(readOnly = true)
    fun questions(formId: Long): Result<QuestionRecord> {
        return dsl
            .selectFrom(QUESTION)
            .where(
                QUESTION.FORM_ID.eq(formId)
                    .and(QUESTION.DELETED_AT.isNull)
            )
            .orderBy(QUESTION.ORDER.asc())
            .fetch()
    }

    @Transactional(readOnly = true)
    fun options(formId: Long): Result<QuestionOptionRecord> {
        return dsl
            .selectFrom(QUESTION_OPTION)
            .where(
                QUESTION_OPTION.FORM_ID.eq(formId)
                    .and(QUESTION_OPTION.DELETED_AT.isNull)
            )
            .orderBy(QUESTION_OPTION.ORDER.asc())
            .fetch()
    }

    // notification

    fun addNotification(formId: Long, userId: Long): Long {
        return formNotificationService.add(formId, userId)
    }

    @Transactional(readOnly = true)
    fun notifications(formId: Long): Result<FormNotificationRecord> {
        return formNotificationService.findByFormId(formId)
    }

    fun deleteNotificationByFormId(formId: Long) {
        formNotificationService.deleteByFormId(formId)
    }
}
