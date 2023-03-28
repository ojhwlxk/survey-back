package pharmcadd.form.common.util

import javax.servlet.http.HttpServletRequest

fun clientIP(request: HttpServletRequest): String? {
    return sequenceOf(
        "X-Forwarded-For",
        "HTTP_CLIENT_IP",
        "HTTP_X_FORWARDED_FOR",
        "HTTP_X_FORWARDED",
        "HTTP_FORWARDED_FOR",
        "HTTP_FORWARDED",
        "REMOTE_ADDR"
    )
        .map { request.getHeader(it) }
        .firstOrNull() ?: request.remoteAddr
}
