package pharmcadd.form.controller.admin

import org.springframework.web.bind.annotation.*
import pharmcadd.form.controller.admin.form.AdminTimeZoneForm
import pharmcadd.form.controller.front.TimeZoneController
import pharmcadd.form.jooq.tables.pojos.TimeZone
import javax.validation.Valid

@RestController
@RequestMapping("/admin/time-zones")
class AdminTimeZoneController : TimeZoneController() {

    @PostMapping
    fun add(@RequestBody @Valid form: AdminTimeZoneForm): TimeZone {
        return timeZoneService.add(form.country, form.zoneId, form.utc)
    }

    @PutMapping("/{id}")
    fun modify(@PathVariable("id") id: Long, @RequestBody @Valid form: AdminTimeZoneForm): TimeZone {
        return timeZoneService.modify(id, form.country, form.zoneId, form.utc)
    }

    @DeleteMapping("/{id}")
    fun delete(@PathVariable("id") id: Long) {
        timeZoneService.deleteById(id)
    }
}
