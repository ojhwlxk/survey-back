package pharmcadd.form.service

import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import pharmcadd.form.common.service.MailService
import pharmcadd.form.common.service.TemplateService
import pharmcadd.form.jooq.Tables.AUTHORIZATION_CODE
import pharmcadd.form.jooq.tables.records.AuthorizationCodeRecord

@Service
@Transactional
class AuthorizationCodeService {

    @Autowired
    lateinit var dsl: DSLContext

    @Autowired
    lateinit var mailService: MailService

    @Autowired
    lateinit var templateService: TemplateService

    fun add(email: String) {
        deleteByEmail(email)

        val target = '0'..'9'
        val code = (1..6).map { target.random() }.joinToString("")

        dsl.insertInto(AUTHORIZATION_CODE)
            .set(AUTHORIZATION_CODE.EMAIL, email)
            .set(AUTHORIZATION_CODE.CODE, code)
            .execute()

        val content = templateService.compile("template/validCode.html.hbs", mapOf("code" to code))
        mailService.sendAndForget {
            title("[Pharmcadd] 인증코드")
            to(email)
            content(content)
        }
    }

    @Transactional(readOnly = true)
    fun findByEmailAndCodeAndVerify(email: String, code: String, verify: Boolean): AuthorizationCodeRecord? {
        return dsl
            .selectFrom(AUTHORIZATION_CODE)
            .where(
                AUTHORIZATION_CODE.EMAIL.eq(email)
                    .and(AUTHORIZATION_CODE.CODE.eq(code))
                    .and(AUTHORIZATION_CODE.VERIFICATION.eq(verify))
                    .and(AUTHORIZATION_CODE.EXPIRED_AT.greaterThan(DSL.currentOffsetDateTime()))
                    .and(AUTHORIZATION_CODE.DELETED_AT.isNull)
            )
            .fetchOne()
    }

    fun deleteByEmail(email: String) {
        dsl.update(AUTHORIZATION_CODE)
            .set(AUTHORIZATION_CODE.DELETED_AT, DSL.currentOffsetDateTime())
            .where(
                AUTHORIZATION_CODE.EMAIL.eq(email)
                    .and(AUTHORIZATION_CODE.DELETED_AT.isNull)
            )
            .execute()
    }
}
