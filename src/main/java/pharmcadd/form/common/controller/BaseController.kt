package pharmcadd.form.common.controller

import org.jooq.DSLContext
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.validation.BindException
import org.springframework.validation.BindingResult
import pharmcadd.form.common.security.SecurityService
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

abstract class BaseController {

    @Autowired
    lateinit var request: HttpServletRequest

    @Autowired
    lateinit var response: HttpServletResponse

    @Autowired
    lateinit var securityService: SecurityService

    @Autowired
    lateinit var dsl: DSLContext

    val currentUserId: Long
        get() {
            return securityService.userId
        }

    fun validate(bindingResult: BindingResult, block: BindingResult.() -> Unit) {
        if (bindingResult.hasErrors()) throw BindException(bindingResult)
        block(bindingResult)
        if (bindingResult.hasErrors()) throw BindException(bindingResult)
    }

    fun <T> validateResult(bindingResult: BindingResult, block: BindingResult.() -> T): T {
        if (bindingResult.hasErrors()) throw BindException(bindingResult)
        val t = block(bindingResult)
        if (bindingResult.hasErrors()) throw BindException(bindingResult)
        return t
    }
}
