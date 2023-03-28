package pharmcadd.form.controller.front

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import pharmcadd.form.common.controller.BaseController
import pharmcadd.form.common.exception.NotFound
import pharmcadd.form.jooq.tables.pojos.TimeZone
import pharmcadd.form.service.TimeZoneService

@RestController
@RequestMapping("/time-zones")
class TimeZoneController : BaseController() {

    @Autowired
    lateinit var timeZoneService: TimeZoneService

    @GetMapping
    fun list(): List<TimeZone> {
        return timeZoneService.findAll()
    }

    @GetMapping("/{id}")
    fun view(@PathVariable("id") id: Long): TimeZone {
        return timeZoneService.findOne(id) ?: throw NotFound()
    }
}
