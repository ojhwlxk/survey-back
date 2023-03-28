package pharmcadd.form.common.controller

import arrow.core.Either
import org.springframework.beans.propertyeditors.StringTrimmerEditor
import org.springframework.core.MethodParameter
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.converter.HttpMessageConverter
import org.springframework.http.server.ServerHttpRequest
import org.springframework.http.server.ServerHttpResponse
import org.springframework.http.server.ServletServerHttpResponse
import org.springframework.web.bind.WebDataBinder
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.InitBinder
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice
import pharmcadd.form.common.Constants
import java.beans.PropertyEditorSupport
import java.time.LocalDate
import java.time.LocalDateTime

@ControllerAdvice
class AdviceController {

    @InitBinder
    fun initBinder(binder: WebDataBinder) {
        binder.registerCustomEditor(String::class.java, StringTrimmerEditor(true))
        binder.registerCustomEditor(
            LocalDate::class.java,
            object : PropertyEditorSupport() {
                override fun setAsText(text: String) {
                    value = LocalDate.parse(text, Constants.LOCAL_DATE_FORMAT)
                }
            }
        )
        binder.registerCustomEditor(
            LocalDateTime::class.java,
            object : PropertyEditorSupport() {
                override fun setAsText(text: String) {
                    value = LocalDateTime.parse(text, Constants.LOCAL_DATE_TIME_FORMAT)
                }
            }
        )
    }
}

@RestControllerAdvice
class EitherResponseBodyAdvice : ResponseBodyAdvice<Any?> {

    override fun supports(returnType: MethodParameter, converterType: Class<out HttpMessageConverter<*>>): Boolean {
        return true
    }

    override fun beforeBodyWrite(
        body: Any?,
        returnType: MethodParameter,
        selectedContentType: MediaType,
        selectedConverterType: Class<out HttpMessageConverter<*>>,
        request: ServerHttpRequest,
        response: ServerHttpResponse
    ): Any? {
        if (body == null) {
            return null
        }
        if (body is Either<*, *>) {
            val res = (response as ServletServerHttpResponse).servletResponse
            return when (body) {
                is Either.Left<*> -> {
                    val exception = body.value as Throwable
                    val status = HttpStatus.INTERNAL_SERVER_ERROR.also {
                        res.status = it.value()
                    }
                    mapOf(
                        "status" to status.value(),
                        "error" to status.reasonPhrase,
                        "message" to exception.localizedMessage,
                        "path" to request.uri.path,
                    )
                }
                is Either.Right<*> -> {
                    body.value
                }
            }
        }
        return body
    }
}
