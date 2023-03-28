package pharmcadd.form.controller.admin

import org.springframework.web.bind.annotation.*
import pharmcadd.form.controller.admin.form.AdminPositionForm
import pharmcadd.form.controller.front.PositionController
import pharmcadd.form.jooq.tables.pojos.Position
import javax.validation.Valid

@RestController
@RequestMapping("/admin/positions")
class AdminPositionController : PositionController() {

    @PostMapping
    fun add(@RequestBody @Valid form: AdminPositionForm): Position {
        val id = positionService.add(form.name)
        return positionService.findOne(id)!!.into(Position::class.java)!!
    }

    @PutMapping("/{id}")
    fun modify(@PathVariable("id") id: Long, @RequestBody @Valid form: AdminPositionForm): Position {
        positionService.modify(id, form.name)
        return positionService.findOne(id)!!.into(Position::class.java)!!
    }

    @DeleteMapping("/{id}")
    fun delete(@PathVariable("id") id: Long) {
        positionService.deleteById(id)
    }
}
