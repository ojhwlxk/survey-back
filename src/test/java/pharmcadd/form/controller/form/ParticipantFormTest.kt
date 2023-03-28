package pharmcadd.form.controller.form

import BaseTest
import org.junit.jupiter.api.Test
import pharmcadd.form.jooq.enums.ParticipantType
import pharmcadd.form.model.ParticipantVo

internal class ParticipantFormTest : BaseTest() {

    @Test
    fun userJsonTest() {
        val user = ParticipantVo(null, ParticipantType.USER, 1, null, false)
        val json = objectMapper.writeValueAsString(user)
        println(json)
        println()
    }

    @Test
    fun groupJsonTest() {
        val group = ParticipantVo(null, ParticipantType.GROUP, null, 1, false)
        val json = objectMapper.writeValueAsString(group)
        println(json)
        println()
    }
}
