package pharmcadd.form.controller.form

import BaseTest
import org.junit.jupiter.api.Test
import pharmcadd.form.controller.front.form.AnswerForm
import pharmcadd.form.model.AnswerVo

internal class AnswerFormTest : BaseTest() {

    @Test
    fun formTest() {
        val form = AnswerForm(
            listOf(
                AnswerVo(4, listOf(10)),
                AnswerVo(6, emptyList(), null, text = "블라블라"),
            )
        )
        val json = objectMapper.writeValueAsString(form)
        println(json)
    }
}
