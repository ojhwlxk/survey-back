package pharmcadd.form.controller.front

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import pharmcadd.form.common.controller.BaseController
import pharmcadd.form.common.exception.NotFound
import pharmcadd.form.common.util.pagination.DataTableForm
import pharmcadd.form.controller.front.form.GroupListForm
import pharmcadd.form.jooq.Tables.*
import pharmcadd.form.jooq.tables.pojos.Group
import pharmcadd.form.model.UserVo
import pharmcadd.form.service.GroupService

@RestController
@RequestMapping("/groups")
class GroupController : BaseController() {

    @Autowired
    lateinit var groupService: GroupService

    @GetMapping
    fun list(form: GroupListForm): List<Group> {
        val query = dsl
            .selectFrom(GROUP)
            .where(
                GROUP.DELETED_AT.isNull
            )

        if (form.parentId != null) {
            query.and(GROUP.PARENT_ID.eq(form.parentId))
        }
        if (form.keyword != null) {
            query.and(GROUP.NAME.contains(form.keyword))
        }

        return query.fetch().map { it.into(Group::class.java) }
    }

    @GetMapping("/{id}")
    fun view(@PathVariable("id") id: Long): Group {
        return groupService.findOne(id)?.into(Group::class.java) ?: throw NotFound()
    }

    @GetMapping("/{id}/users")
    fun users(@PathVariable("id") id: Long, form: DataTableForm): List<UserVo> {
        return groupService.users(id).map { UserVo.of(it) }
    }

    @GetMapping("/{id}/children")
    fun children(@PathVariable("id") id: Long): List<Group> {
        return groupService.findByParentId(id).map { it.into(Group::class.java) }
    }

    @GetMapping("/{id}/pathways")
    fun pathways(@PathVariable("id") id: Long): List<Group> {
        return groupService.findByPathways(id).map { it.into(Group::class.java) }
    }
}
