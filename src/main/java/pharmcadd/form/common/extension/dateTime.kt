package pharmcadd.form.common.extension

import java.time.*

private val ZERO_TIME = LocalTime.of(0, 0, 0)
private val LAST_TIME = ZERO_TIME.minusNanos(1)

fun LocalDate.withZeroTime(): LocalDateTime {
    return LocalDateTime.of(this, ZERO_TIME)
}

fun LocalDate.withLastTime(): LocalDateTime {
    return LocalDateTime.of(this, LAST_TIME)
}

operator fun LocalDate.plus(time: LocalTime): LocalDateTime {
    return LocalDateTime.of(this, time)
}

operator fun LocalDateTime.plus(zoneOffset: ZoneOffset): OffsetDateTime {
    return OffsetDateTime.of(this, zoneOffset)
}
