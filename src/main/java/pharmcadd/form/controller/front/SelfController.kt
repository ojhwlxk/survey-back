package pharmcadd.form.controller.front

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.web.bind.annotation.*
import pharmcadd.form.common.controller.BaseController
import pharmcadd.form.common.exception.BadRequest
import pharmcadd.form.common.exception.NotFound
import pharmcadd.form.common.extension.plus
import pharmcadd.form.common.extension.withLastTime
import pharmcadd.form.common.extension.withZeroTime
import pharmcadd.form.common.extension.zoneOffset
import pharmcadd.form.common.util.pagination.DataTablePagination
import pharmcadd.form.controller.front.form.AnswerCancelListForm
import pharmcadd.form.controller.front.form.ChangePasswordForm
import pharmcadd.form.controller.front.form.JoinForm
import pharmcadd.form.jooq.Tables.CANCEL_ANSWER
import pharmcadd.form.jooq.tables.pojos.CancelAnswer
import pharmcadd.form.model.UserDetail
import pharmcadd.form.service.TimeZoneService
import pharmcadd.form.service.UserService

@RestController
@RequestMapping("/users/self")
class SelfController : BaseController() {

    @Autowired
    lateinit var userService: UserService

    @Autowired
    lateinit var passwordEncoder: PasswordEncoder

    @Autowired
    lateinit var timeZoneService: TimeZoneService

    @GetMapping
    fun detail(): UserDetail {
        return userService.detail(securityService.userId) ?: throw NotFound()
    }

    @PatchMapping("/password")
    fun changePassword(@RequestBody form: ChangePasswordForm) {
        val user = userService.findOne(securityService.userId) ?: throw NotFound()
        if (passwordEncoder.matches(form.password, user.password).not()) {
            throw BadRequest()
        } else {
            userService.changePassword(user.id, form.newPassword)
        }
    }

    @PostMapping("/groups")
    fun addGroup(@RequestBody form: JoinForm.Group) {
        userService.addGroup(securityService.userId, form.groupId, form.positionId)
    }

    @DeleteMapping("/groups/{groupId}")
    fun deleteGroup(@PathVariable("groupId") groupId: Long) {
        userService.deleteGroup(securityService.userId, groupId)
    }

    @DeleteMapping("/positions/{positionId}")
    fun deletePosition(@PathVariable("positionId") positionId: Long) {
        userService.deletePosition(securityService.userId, positionId)
    }

    @GetMapping("/answer-cancels")
    fun answerCancels(form: AnswerCancelListForm): DataTablePagination<CancelAnswer> {
        val userId = securityService.userId

        val query = dsl
            .selectFrom(
                CANCEL_ANSWER
            )
            .where(
                CANCEL_ANSWER.USER_ID.eq(userId)
                    .and(CANCEL_ANSWER.DELETED_AT.isNull)
            )

        if(form.status != null) {
            query.and(
                CANCEL_ANSWER.STATUS.eq(form.status)
            )
        }

        val rangeFrom = form.rangeFrom
        val rangeTo = form.rangeTo
        if (rangeFrom != null || rangeTo != null) {
            val timeZone = timeZoneService.findByUserId(userId)!!
            val zoneOffset = timeZone.zoneOffset()
            if(rangeFrom != null) {
                query.and(
                    CANCEL_ANSWER.CREATED_AT.greaterOrEqual(rangeFrom.withZeroTime()+zoneOffset)
                )
            }

            if(rangeTo != null) {
                query.and(
                    CANCEL_ANSWER.CREATED_AT.lessOrEqual(rangeTo.withLastTime() + zoneOffset)
                )
            }
        }

       return DataTablePagination.of(dsl, query, form) {
           it.into(CancelAnswer::class.java)
       }
    }
}
