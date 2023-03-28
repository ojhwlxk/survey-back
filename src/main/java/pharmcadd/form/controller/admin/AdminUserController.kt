package pharmcadd.form.controller.admin

import org.springframework.web.bind.annotation.*
import pharmcadd.form.controller.admin.form.AdminChangePasswordForm
import pharmcadd.form.controller.admin.form.AdminChangeRoleForm
import pharmcadd.form.controller.admin.form.AdminUserAddForm
import pharmcadd.form.controller.admin.form.AdminUserModifyForm
import pharmcadd.form.controller.front.UserController
import pharmcadd.form.model.UserVo
import javax.validation.Valid

@RestController
@RequestMapping("/admin/users")
class AdminUserController : UserController() {

    @PostMapping
    fun add(@RequestBody @Valid form: AdminUserAddForm): UserVo {
        val id = userService.add(
            form.name,
            form.username,
            form.password,
            form.email,
            form.role,
            form.timeZoneId,
            true
        )
        val user = userService.findOne(id)!!
        return UserVo.of(user)
    }

    @PutMapping("/{id}")
    fun modify(@PathVariable("id") id: Long, @RequestBody @Valid form: AdminUserModifyForm) {
        userService.modify(
            id,
            form.name,
            form.password,
            form.email,
            form.role,
            form.timeZoneId,
            form.active
        )
    }

    @PatchMapping("/{id}/active")
    fun active(@PathVariable("id") id: Long) {
        userService.active(id)
    }

    @PatchMapping("/{id}/inactive")
    fun inactive(@PathVariable("id") id: Long) {
        userService.inactive(id)
    }

    @PatchMapping("/{id}/password")
    fun changePassword(@PathVariable("id") id: Long, @RequestBody form: AdminChangePasswordForm) {
        userService.changePassword(id, form.password)
    }

    @PatchMapping("/{id}/role")
    fun changeRole(@PathVariable("id") id: Long, @RequestBody form: AdminChangeRoleForm) {
        userService.changeRole(id, form.role)
    }
}
