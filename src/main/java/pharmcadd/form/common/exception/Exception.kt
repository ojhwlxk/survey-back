package pharmcadd.form.common.exception

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ResponseStatus

sealed class HttpStatusException(val status: HttpStatus, message: String? = null, cause: Throwable? = null) :
    RuntimeException(message, cause)

@ResponseStatus(code = HttpStatus.NO_CONTENT)
class NoContent(message: String? = null, cause: Throwable? = null) :
    HttpStatusException(HttpStatus.NO_CONTENT, message, cause)

// 40x client error

@ResponseStatus(code = HttpStatus.BAD_REQUEST)
class BadRequest(message: String? = null, cause: Throwable? = null) :
    HttpStatusException(HttpStatus.BAD_REQUEST, message, cause)

@ResponseStatus(code = HttpStatus.UNAUTHORIZED)
class Unauthorized(message: String? = null, cause: Throwable? = null) :
    HttpStatusException(HttpStatus.UNAUTHORIZED, message, cause)

@ResponseStatus(code = HttpStatus.FORBIDDEN)
class AccessDenied(message: String? = null, cause: Throwable? = null) :
    HttpStatusException(HttpStatus.FORBIDDEN, message, cause)

@ResponseStatus(code = HttpStatus.NOT_FOUND)
class NotFound(message: String? = null, cause: Throwable? = null) :
    HttpStatusException(HttpStatus.NOT_FOUND, message, cause)

// 50x server error

@ResponseStatus(code = HttpStatus.NOT_IMPLEMENTED)
class NotYet(message: String? = null, cause: Throwable? = null) :
    HttpStatusException(HttpStatus.FORBIDDEN, message, cause)
