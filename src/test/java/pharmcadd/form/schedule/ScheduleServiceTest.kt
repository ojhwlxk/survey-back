package pharmcadd.form.schedule

import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.*

internal class ScheduleServiceTest {

    @Test
    fun localServerDateTest() {
        // 인도 시간으로 0 시 0 분 0 초는
        val startsAt = LocalDateTime.of(2020, 1, 27, 0, 0, 0).atZone(ZoneId.of("+05:30"))

        // 서버 시간(한국)으로 3시 30분 이다.
        val localServerDateTime = startsAt.withZoneSameInstant(ZoneId.systemDefault())
        val date = Date.from(localServerDateTime.toInstant())
        println(date)
    }

    @Test
    fun localServerDateTest2() {
        // 한국 시간으로 0 시 0 분 0 초는
        val startsAt = LocalDateTime.of(2020, 1, 27, 0, 0, 0).atZone(ZoneId.of("+09:00"))

        // 서버 시간(한국)으로 변환 시 동일 하다.
        val localServerDateTime = startsAt.withZoneSameInstant(ZoneId.systemDefault())
        val date = Date.from(localServerDateTime.toInstant())
        println(date)
    }
}
