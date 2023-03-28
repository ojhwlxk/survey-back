package pharmcadd.form.model

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import pharmcadd.form.jooq.enums.QuestionType

internal class FormVoTest {

    @Test
    fun removeTest() {
        val prevIds = listOf(1, 2, 3)
        val newIds = listOf(2, 3, null)
        assertEquals(listOf(1), prevIds - newIds)
    }

    val objectMapper = jacksonObjectMapper()

    @Test
    fun newTest() {
        val form = FormVo(
            null, "양식~~~", "내용 테스트",
            listOf(
                FormVo.QuestionVo(
                    null, "질문1", null, QuestionType.CHOICE_SINGLE, true,
                    listOf(
                        FormVo.QuestionVo.OptionVo(null, "보기1-1"),
                        FormVo.QuestionVo.OptionVo(null, "보기1-2"),
                        FormVo.QuestionVo.OptionVo(null, "보기1-3"),
                        FormVo.QuestionVo.OptionVo(null, "보기1-4"),
                    )
                ),
                FormVo.QuestionVo(
                    null, "질문2", null, QuestionType.CHOICE_MULTIPLE, true,
                    listOf(
                        FormVo.QuestionVo.OptionVo(null, "보기2-1"),
                        FormVo.QuestionVo.OptionVo(null, "보기2-2"),
                        FormVo.QuestionVo.OptionVo(null, "보기2-3"),
                        FormVo.QuestionVo.OptionVo(null, "보기2-4"),
                    )
                ),
                FormVo.QuestionVo(null, "질문3", null, QuestionType.TEXT_LONG),
            )
        )

        val json = objectMapper.writeValueAsString(form)
        println(json)
    }

    @Test
    fun modifyTest() {
        val json = """
            {
                "id": 6,
                "title": "양식~~~",
                "description": "설명",
                "questions": [
                    {
                        "id": 4,
                        "title": "질문1",
                        "type": "CHOICE_MULTIPLE",
                        "options": [
                            {
                                "id": 9,
                                "text": "보기1-1 수정"
                            },
                            {
                                "id": 10,
                                "text": "보기1-2 수정"
                            },
                        ]
                    },
                    {
                        "id": 6,
                        "title": "질문3",
                        "type": "TEXT_LONG",
                        "options": []
                    }
                ]
            }
        """.trimIndent()

        val readValue = objectMapper.readValue<FormVo>(json)
        val copy = readValue

        val json2 = objectMapper.writeValueAsString(copy)
        println(json2)
    }
}
