package pharmcadd.form.controller

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

internal class SignControllerTest {

    @Test
    fun codeTest() {
        val target = '0'..'9'
        val code = (1..6).map { target.random() }.joinToString("")
        println(code)
    }
}
