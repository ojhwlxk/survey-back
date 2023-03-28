package pharmcadd.form.controller.admin.form

import BaseTest
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.time.ZoneId

internal class AdminTimeZoneFormTest : BaseTest() {
    @Test
    fun test() {

        println(ZoneId.getAvailableZoneIds())

        val zoneForm = AdminTimeZoneForm("United Kingdom", "Europe/London", "+00:00")
        println(objectMapper.writeValueAsString(zoneForm))
    }
}
