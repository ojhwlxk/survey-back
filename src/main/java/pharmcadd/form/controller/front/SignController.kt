package pharmcadd.form.controller.front

import org.jooq.impl.DSL
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import pharmcadd.form.common.controller.BaseController
import pharmcadd.form.common.exception.BadRequest
import pharmcadd.form.controller.front.form.EmailForm
import pharmcadd.form.controller.front.form.JoinForm
import pharmcadd.form.controller.front.form.ResetPasswordForm
import pharmcadd.form.controller.front.form.ValidCodeConfirmForm
import pharmcadd.form.jooq.Tables.AUTHORIZATION_CODE
import pharmcadd.form.model.UserVo
import pharmcadd.form.service.AuthorizationCodeService
import pharmcadd.form.service.UserService
import java.time.OffsetDateTime

@RestController
class SignController : BaseController() {

    @Autowired
    lateinit var userService: UserService

    @Autowired
    lateinit var authorizationCodeService: AuthorizationCodeService

    @PostMapping("/valid-email")
    fun validEmail(@RequestBody form: EmailForm) {
        val user = userService.findByEmail(form.email)
        if (user != null) {
            throw BadRequest()
        }
    }

    @PostMapping("/valid-code")
    fun validCode(@RequestBody form: EmailForm) {
        authorizationCodeService.add(form.email)
    }

    @PostMapping("/valid-code/confirm")
    fun validCodeConfirm(@RequestBody form: ValidCodeConfirmForm) {
        val record =
            authorizationCodeService.findByEmailAndCodeAndVerify(form.email, form.code, false) ?: throw BadRequest()

        dsl.update(AUTHORIZATION_CODE)
            .set(AUTHORIZATION_CODE.VERIFICATION, true)
            .set(AUTHORIZATION_CODE.EXPIRED_AT, DSL.field("now() + '5 minute'", OffsetDateTime::class.java))
            .where(
                AUTHORIZATION_CODE.ID.eq(record.id)
                    .and(AUTHORIZATION_CODE.DELETED_AT.isNull)
            )
            .execute()
    }

    @PostMapping("/join")
    fun join(@RequestBody form: JoinForm): UserVo {
        authorizationCodeService.findByEmailAndCodeAndVerify(form.email, form.code, true) ?: throw BadRequest()

        val id = userService.join(form)
        val user = userService.findOne(id)!!
        return UserVo.of(user)
    }

    @PatchMapping("/password/reset")
    fun resetPassword(@RequestBody form: ResetPasswordForm) {
        val user = userService.findByEmail(form.email)!!
        authorizationCodeService.findByEmailAndCodeAndVerify(form.email, form.code, true) ?: throw BadRequest()
        userService.changePassword(user.id, form.newPassword)
    }
}
