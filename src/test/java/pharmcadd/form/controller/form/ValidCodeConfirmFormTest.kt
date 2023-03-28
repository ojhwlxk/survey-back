package pharmcadd.form.controller.form

import BaseTest
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.junit.jupiter.api.Test
import pharmcadd.form.controller.front.form.ValidCodeConfirmForm

internal class ValidCodeConfirmFormTest : BaseTest() {

    @Test
    fun validCodeConfirmTest() {
        val validCodeConfirmForm = ValidCodeConfirmForm(
            email = "test@pharmcadd.com",
            code = "123456"
        )
        val valueAsString = jacksonObjectMapper().writeValueAsString(validCodeConfirmForm)
        println(valueAsString)
    }
}
