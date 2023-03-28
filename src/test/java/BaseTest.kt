import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper

abstract class BaseTest {
    val objectMapper = jacksonObjectMapper()
}
