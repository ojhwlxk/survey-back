package pharmcadd.form.controller.admin

import org.jooq.impl.DSL
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import pharmcadd.form.common.controller.BaseController
import pharmcadd.form.common.util.pagination.DataTablePagination
import pharmcadd.form.controller.admin.form.AnswerStatListForm
import pharmcadd.form.controller.admin.form.AnswerStatsForm
import pharmcadd.form.jooq.Tables.*
import pharmcadd.form.jooq.tables.pojos.AnswerStat

@RestController
@RequestMapping("/admin/answer-stats")
class AnswerStatController : BaseController() {

    @GetMapping
    fun list(form: AnswerStatListForm): DataTablePagination<AnswerStat> {
        val query = dsl
            .select(
                *ANSWER_STAT.fields()
            )
            .from(ANSWER_STAT)
            .innerJoin(CAMPAIGN).on(CAMPAIGN.ID.eq(ANSWER_STAT.CAMPAIGN_ID).and(CAMPAIGN.DELETED_AT.isNull))
            .where(
                ANSWER_STAT.DELETED_AT.isNull
            )

        if (form.campaignId != null) {
            query.and(CAMPAIGN.ID.eq(form.campaignId))
        }
        if (form.formId != null) {
            query.and(CAMPAIGN.FORM_ID.eq(form.formId))
        }

        return DataTablePagination.of(dsl, query, form) {
            it.into(AnswerStat::class.java)
        }
    }

    @GetMapping("/{campaign_id}")
    fun stats(@PathVariable("campaign_id") campaign_id: Long): List<AnswerStatsForm> {
        val answersMap = dsl
            .select(
                ANSWER.QUESTION_ID.`as`("question_id"),
                ANSWER.OPTION_ID.`as`("option_id"),
                ANSWER.TEXT.`as`("text"),
                DSL.count(ANSWER.OPTION_ID).`as`("count"),
            )
            .from(ANSWER)
            .where(
                ANSWER.CAMPAIGN_ID.eq(campaign_id)
                    .and(ANSWER.OPTION_ID.greaterThan(0))
                    .and(ANSWER.DELETED_AT.isNull)
            )
            .groupBy(ANSWER.QUESTION_ID, ANSWER.OPTION_ID, ANSWER.TEXT)
            .fetch()
            .groupBy { it.get("question_id", Long::class.java) }

        return answersMap
            .toList()
            .map { (question_id, answers) ->
                AnswerStatsForm(
                    questionId = question_id,
                    total = answers.fold(0) { total, r -> total + r.get("count", Int::class.java) },
                    optionStats = answers.map { a ->
                        AnswerStatsForm.OptionStatsForm(
                            optionId = a.get("option_id", Long::class.java),
                            text = a.get("text", String::class.java),
                            count = a.get("count", Int::class.java),
                        )
                    }
                )
            }
    }
}
