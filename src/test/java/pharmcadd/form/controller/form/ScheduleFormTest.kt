package pharmcadd.form.controller.form

import BaseTest
import org.junit.jupiter.api.Test
import pharmcadd.form.jooq.enums.FormScheduleType
import pharmcadd.form.model.ScheduleVo
import java.time.Duration

internal class ScheduleFormTest : BaseTest() {

    @Test
    fun cronFormTest() {
        val form = ScheduleVo(
            type = FormScheduleType.CRON,
            timeZoneId = 1,
            startsAt = null,
            endsAt = null,
            cronExpression = "0 * * * * ?",
            cronDuration = Duration.ofDays(1).toMillis(),
            active = true
        )
        val json = objectMapper.writeValueAsString(form)
        println(json)
    }
}
