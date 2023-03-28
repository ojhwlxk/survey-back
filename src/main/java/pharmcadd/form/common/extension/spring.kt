package pharmcadd.form.common.extension

import org.springframework.core.env.Environment
import org.springframework.validation.BindException
import org.springframework.validation.BindingResult
import pharmcadd.form.common.Constants

fun BindingResult.validate(block: () -> Unit) {
    if (hasErrors()) throw BindException(this)
    block()
    if (hasErrors()) throw BindException(this)
}

val Environment.isLocal: Boolean
    get() = activeProfiles.contains(Constants.Env.LOCAL)

val Environment.isProd: Boolean
    get() = activeProfiles.contains(Constants.Env.PROD)
