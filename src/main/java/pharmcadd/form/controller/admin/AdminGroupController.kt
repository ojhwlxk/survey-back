package pharmcadd.form.controller.admin

import org.springframework.web.bind.annotation.*
import pharmcadd.form.controller.admin.form.AdminGroupForm
import pharmcadd.form.controller.front.GroupController
import pharmcadd.form.jooq.tables.pojos.Group
import pharmcadd.form.jooq.tables.pojos.Position
import javax.validation.Valid

@RestController
@RequestMapping("/admin/groups")
class AdminGroupController : GroupController() {

    @PostMapping
    fun add(@RequestBody @Valid form: AdminGroupForm): Group {
        val id = groupService.add(form.name, form.parentId)
        return groupService.findOne(id)!!.into(Group::class.java)!!
    }

    @PutMapping("/{id}")
    fun modify(@PathVariable("id") id: Long, @RequestBody @Valid form: AdminGroupForm): Position {
        groupService.modify(id, form.name, form.parentId)
        return groupService.findOne(id)!!.into(Position::class.java)!!
    }

    @DeleteMapping("/{id}")
    fun delete(@PathVariable("id") id: Long) {
        groupService.deleteById(id)
    }
}
