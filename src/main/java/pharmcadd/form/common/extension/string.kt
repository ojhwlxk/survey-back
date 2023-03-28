package pharmcadd.form.common.extension

import com.google.common.base.CaseFormat

fun String.upperCamelToLowerUnderscore(): String {
    return CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, this)
}

operator fun String.times(level: Int): String = repeat(level)
