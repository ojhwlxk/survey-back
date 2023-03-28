package pharmcadd.form.service

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.time.ZoneId
import java.util.*

internal class FormScheduleServiceTest {

    @Test
    fun timezoneTest() {
        val utc = "+09:00"
        val timeZone = TimeZone.getTimeZone(ZoneId.of("$utc"))
        println(timeZone)
    }
}
