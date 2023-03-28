package pharmcadd.form.common.util

import java.security.SecureRandom
import java.util.*

class Random {

    fun generate(size: Int): String {
        val resize = if (size < 4) {
            size - 1
        } else {
            size - 2
        }

        val secureRandom = SecureRandom()
        val byteArray = ByteArray(resize)
        secureRandom.nextBytes(byteArray)

        return Base64.getUrlEncoder().withoutPadding()
            .encodeToString(byteArray)
    }

    fun generateNumeric(size: Int): String {
        val secureRandom = SecureRandom()
        var result = ""
        for (i in 1..size) {
            result += secureRandom.nextInt(9).toString()
        }
        return result
    }
}
