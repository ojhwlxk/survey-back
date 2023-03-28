package pharmcadd.form.common

import java.time.format.DateTimeFormatter

object Constants {
    val LOCAL_DATE_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    val LOCAL_DATE_TIME_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

    object Env {
        const val LOCAL = "local"
        const val PROD = "prod"
    }
}
