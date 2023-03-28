package pharmcadd.form.controller

import mu.KotlinLogging
import org.jooq.DSLContext
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import pharmcadd.form.common.util.clientIP
import pharmcadd.form.common.service.GeoPluginService
import javax.servlet.http.HttpServletRequest

@RestController
class MainController {

    private val logger = KotlinLogging.logger { }

    @Autowired
    lateinit var geoPluginService: GeoPluginService

    @Autowired
    lateinit var dsl: DSLContext

    @GetMapping("/")
    fun index(request: HttpServletRequest): Map<String, Int> {
        val clientIP = clientIP(request)!!
        val geoInfo = geoPluginService.info(clientIP)
        logger.info { geoInfo }

        return mapOf("foo" to 1, "bar" to 2)
    }
}
