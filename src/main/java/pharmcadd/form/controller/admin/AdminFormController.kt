package pharmcadd.form.controller.admin

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.*
import pharmcadd.form.common.extension.upperCamelToLowerUnderscore
import pharmcadd.form.common.util.pagination.DataTablePagination
import pharmcadd.form.controller.admin.form.*
import pharmcadd.form.controller.front.FormController
import pharmcadd.form.jooq.Tables.FORM
import pharmcadd.form.jooq.Tables.USER
import pharmcadd.form.jooq.tables.pojos.Form
import pharmcadd.form.jooq.tables.pojos.FormNotification
import pharmcadd.form.jooq.tables.pojos.FormSchedule
import pharmcadd.form.jooq.tables.pojos.FormScheduleParticipant
import pharmcadd.form.model.FormVo
import pharmcadd.form.model.ParticipantVo
import pharmcadd.form.model.ScheduleVo
import pharmcadd.form.service.FormNotificationService
import pharmcadd.form.service.FormScheduleParticipantService
import pharmcadd.form.service.FormScheduleService
import javax.validation.Valid

@RestController
@RequestMapping("/admin/forms")
class AdminFormController : FormController() {

    @Autowired
    lateinit var formScheduleParticipantService: FormScheduleParticipantService

    @Autowired
    lateinit var formScheduleService: FormScheduleService

    @Autowired
    lateinit var formNotificationService: FormNotificationService

    @GetMapping
    fun list(form: AdminFormListForm): DataTablePagination<AdminFormListFormModel> {
        val c = USER.`as`("c")
        val u = USER.`as`("u")

        val query = dsl
            .select(
                *FORM.fields()
            )
            .select(
                c.NAME.`as`(AdminFormListFormModel::createdByName.name.upperCamelToLowerUnderscore()),
                u.NAME.`as`(AdminFormListFormModel::updatedByName.name.upperCamelToLowerUnderscore()),
            )
            .from(FORM)
            .leftJoin(c).on(FORM.CREATED_BY.eq(c.ID).and(c.DELETED_AT.isNull))
            .leftJoin(u).on(FORM.UPDATED_BY.eq(u.ID).and(u.DELETED_AT.isNull))
            .where(
                FORM.DELETED_AT.isNull
            )

        if (form.keyword != null) {
            query.and(FORM.TITLE.contains(form.keyword))
        }

        return DataTablePagination.of(dsl, query, form) {
            it.into(AdminFormListFormModel::class.java)
        }
    }

    @PostMapping
    fun add(@RequestBody @Valid form: FormVo): Form {
        val id = formService.save(form, securityService.userId)
        return formService.findOne(id)!!.into(Form::class.java)!!
    }

    @PutMapping("/{id}")
    fun modify(@PathVariable("id") id: Long, @RequestBody @Valid form: FormVo): Form {
        formService.save(form, securityService.userId)
        return formService.findOne(id)!!.into(Form::class.java)!!
    }

    // schedules

    @GetMapping("/{id}/schedules")
    fun schedules(@PathVariable("id") formId: Long): List<FormSchedule> {
        return formScheduleService.findByFormId(formId).map { it.into(FormSchedule::class.java) }
    }

    @PostMapping("/{id}/schedules")
    fun addSchedule(@PathVariable("id") formId: Long, @RequestBody @Valid form: ScheduleVo): FormSchedule {
        val scheduleId = formScheduleService.add(formId, form)
        return formScheduleService.findOne(scheduleId)!!.into(FormSchedule::class.java)!!
    }

    @PostMapping("/{id}/schedules/{scheduleId}")
    fun modifySchedule(
        @PathVariable("id") formId: Long,
        @PathVariable("scheduleId") scheduleId: Long,
        @RequestBody @Valid form: ScheduleVo
    ): FormSchedule {
        formScheduleService.modify(scheduleId, formId, form)
        return formScheduleService.findOne(scheduleId)!!.into(FormSchedule::class.java)!!
    }

    @DeleteMapping("/{id}/schedules/{scheduleId}")
    fun deleteSchedule(@PathVariable("id") formId: Long, @PathVariable("scheduleId") scheduleId: Long) {
        formScheduleService.deleteById(scheduleId)
    }

    @PatchMapping("/{id}/schedules/{scheduleId}/active")
    fun activeSchedule(
        @PathVariable("id") formId: Long,
        @PathVariable("scheduleId") scheduleId: Long,
    ) {
        formScheduleService.active(scheduleId)
    }

    @PatchMapping("/{id}/schedules/{scheduleId}/inactive")
    fun inactiveSchedule(
        @PathVariable("id") formId: Long,
        @PathVariable("scheduleId") scheduleId: Long,
    ) {
        formScheduleService.inactive(scheduleId)
    }

    // participants

    @GetMapping("/{id}/schedules/{scheduleId}/participants")
    fun participants(
        @PathVariable("id") formId: Long,
        @PathVariable("scheduleId") scheduleId: Long
    ): List<FormScheduleParticipant> {
        return formScheduleParticipantService.findByScheduleId(scheduleId)
            .map { it.into(FormScheduleParticipant::class.java) }
    }

    @DeleteMapping("/{id}/schedules/{scheduleId}/participants")
    fun deleteParticipant(
        @PathVariable("id") formId: Long,
        @PathVariable("scheduleId") scheduleId: Long,
    ) {
        formScheduleParticipantService.deleteByScheduleId(scheduleId)
    }

    @PostMapping("/{id}/schedules/{scheduleId}/participants")
    fun addParticipant(
        @PathVariable("id") formId: Long,
        @PathVariable("scheduleId") scheduleId: Long,
        @RequestBody @Valid form: ParticipantVo
    ): FormScheduleParticipant {
        val participantId = formScheduleParticipantService.add(scheduleId, formId, form)
        return formScheduleParticipantService.findOne(participantId)!!.into(FormScheduleParticipant::class.java)!!
    }

    @PostMapping("/{id}/schedules/{scheduleId}/participants/{participantId}")
    fun modifyParticipant(
        @PathVariable("id") formId: Long,
        @PathVariable("scheduleId") scheduleId: Long,
        @PathVariable("participantId") participantId: Long,
        @RequestBody @Valid form: ParticipantVo
    ): FormScheduleParticipant {
        formScheduleParticipantService.modify(participantId, scheduleId, formId, form)
        return formScheduleParticipantService.findOne(participantId)!!.into(FormScheduleParticipant::class.java)!!
    }

    @DeleteMapping("/{id}/schedules/{scheduleId}/participants/{participantId}")
    fun deleteParticipant(
        @PathVariable("id") formId: Long,
        @PathVariable("scheduleId") scheduleId: Long,
        @PathVariable("participantId") participantId: Long
    ) {
        formScheduleParticipantService.deleteById(participantId)
    }

    // notification

    @GetMapping("/{id}/notifications")
    fun notifications(@PathVariable("id") formId: Long): List<FormNotification> {
        return formNotificationService.findByFormId(formId).map { it.into(FormNotification::class.java) }
    }

    @PostMapping("/{id}/notifications")
    fun addNotification(
        @PathVariable("id") formId: Long,
        @RequestBody @Valid form: AdminNotificationForm
    ): FormNotification {
        val notificationId = formNotificationService.add(formId, form.userId)
        return formNotificationService.findOne(notificationId)!!.into(FormNotification::class.java)!!
    }

    @DeleteMapping("/{id}/notifications/{notificationId}")
    fun deleteNotification(@PathVariable("id") formId: Long, @PathVariable("notificationId") notificationId: Long) {
        formNotificationService.deleteById(notificationId)
    }
}
