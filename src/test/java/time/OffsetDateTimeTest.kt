package time

import org.junit.jupiter.api.Test
import pharmcadd.form.common.extension.plus
import pharmcadd.form.common.extension.withLastTime
import pharmcadd.form.common.extension.withZeroTime
import java.time.*

class OffsetDateTimeTest {

    @Test
    fun test() {
        val userZoneOffset = ZoneOffset.of("+09:00") // ZoneOffset.ofHoursMinutes(9, 0)
        val sDate = LocalDate.of(2021, 12, 28)
        val eDate = LocalDate.of(2021, 12, 31)
        val rangeFrom = OffsetDateTime.of(sDate, LocalTime.of(0, 0, 0), userZoneOffset)
        val rangeTo = OffsetDateTime.of(eDate, LocalTime.of(23, 59, 59), userZoneOffset)
        println(rangeFrom)
        println(rangeTo)
    }

    @Test
    fun operatorTest() {
        val userZoneOffset = ZoneOffset.of("+09:00")!!
        val sDate = LocalDate.of(2021, 12, 28).withZeroTime()
        val eDate = LocalDate.of(2021, 12, 31).withLastTime()

        val rangeFrom = sDate + userZoneOffset
        val rangeTo = eDate + userZoneOffset

        println(rangeFrom)
        println(rangeTo)
    }
}
