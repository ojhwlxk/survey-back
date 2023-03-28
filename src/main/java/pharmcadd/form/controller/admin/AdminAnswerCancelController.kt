package pharmcadd.form.controller.admin

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.*
import pharmcadd.form.common.controller.BaseController
import pharmcadd.form.common.extension.upperCamelToLowerUnderscore
import pharmcadd.form.common.util.pagination.DataTablePagination
import pharmcadd.form.controller.admin.form.AdminAnswerCancelListForm
import pharmcadd.form.controller.admin.form.AdminCancelAnswerApprovalForm
import pharmcadd.form.jooq.Tables.CANCEL_ANSWER
import pharmcadd.form.jooq.Tables.USER
import pharmcadd.form.model.CancelAnswerVo
import pharmcadd.form.service.CancelAnswerService
import javax.validation.Valid

@RestController
@RequestMapping("/admin/answer-cancels")
class AdminAnswerCancelController : BaseController() {

    @Autowired
    lateinit var cancelAnswerService: CancelAnswerService

    @GetMapping
    fun list(form: AdminAnswerCancelListForm): DataTablePagination<CancelAnswerVo> {
        val r = USER.`as`("r")
        val a = USER.`as`("a")

        val query = dsl
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
                CANCEL_ANSWER.DELETED_AT.isNull
            )

        if (form.type != null) {
            query.and(CANCEL_ANSWER.STATUS.eq(form.type))
        }
        if (form.requester != null) {
            query.and(r.NAME.contains(form.requester))
        }
        if (form.approver != null) {
            query.and(r.NAME.contains(form.approver))
        }

        return DataTablePagination.of(dsl, query, form) {
            it.into(CancelAnswerVo::class.java)
        }
    }

    @PatchMapping("/{id}/approve")
    fun approve(
        @PathVariable("id") id: Long,
        @RequestBody @Valid form: AdminCancelAnswerApprovalForm
    ) {
        cancelAnswerService.approve(id, securityService.userId, form.answer)
    }

    @PatchMapping("/{id}/reject")
    fun reject(
        @PathVariable("id") id: Long,
        @RequestBody @Valid form: AdminCancelAnswerApprovalForm
    ) {
        cancelAnswerService.reject(id, securityService.userId, form.answer)
    }
}
