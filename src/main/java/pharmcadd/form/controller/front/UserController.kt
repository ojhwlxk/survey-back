package pharmcadd.form.controller.front

import kotlinx.coroutines.selects.select
import org.jooq.impl.DSL
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.*
import pharmcadd.form.common.controller.BaseController
import pharmcadd.form.common.exception.NotFound
import pharmcadd.form.common.util.pagination.DataTablePagination
import pharmcadd.form.controller.front.form.UserListForm
import pharmcadd.form.controller.front.form.UserListFormModel
import pharmcadd.form.jooq.Tables.*
import pharmcadd.form.model.UserDetail
import pharmcadd.form.model.UserVo
import pharmcadd.form.service.UserService

@RestController
@RequestMapping("/users")
class UserController : BaseController() {

    @Autowired
    lateinit var userService: UserService

    @GetMapping
    fun list(form: UserListForm): DataTablePagination<UserListFormModel> {
        val groupNamesField = DSL.arrayAgg(GROUP.NAME).`as`("group_names")

        val positionNamesField = DSL.arrayAgg(POSITION.NAME).`as`("position_names")

        val query = dsl
            .select(
                *USER.fields()
            )
            .select(
                groupNamesField
            )
            .select(
                positionNamesField
            )
            .from(USER)
            .leftJoin(GROUP_USER).on(GROUP_USER.USER_ID.eq(USER.ID).and(GROUP_USER.DELETED_AT.isNull))
            .leftJoin(GROUP).on(GROUP_USER.GROUP_ID.eq(GROUP.ID).and(GROUP.DELETED_AT.isNull))
            .leftJoin(POSITION).on(GROUP_USER.POSITION_ID.eq(POSITION.ID).and(POSITION.DELETED_AT.isNull))
            .where(
                USER.DELETED_AT.isNull
            )

        val keyword = form.keyword
        if (keyword != null) {
            query.and(
                USER.NAME.contains(keyword).or(USER.USERNAME.contains(keyword)).or(USER.EMAIL.contains(keyword))
            )
        }
        if (form.groupId != null) {
            val includeSubgroup = form.includeSubgroup ?: false
            if (includeSubgroup) {
                query.and(GROUP.PATHWAYS.contains(arrayOf(form.groupId)))
            } else {
                query.and(GROUP.ID.eq(form.groupId))
            }
        }

        if (form.positionId != null) {
            query.and(GROUP_USER.POSITION_ID.eq(form.positionId))
        }

        query.groupBy(*USER.fields())

        return DataTablePagination.of(dsl, query, form) {
            val user = it.into(USER)
            UserListFormModel(
                user.id,
                user.name,
                user.username,
                user.email,
                user.role,
                user.timeZoneId,
                user.active,
                user.createdAt,
                user.updatedAt,
                (it.get(groupNamesField) ?: emptyArray<String>()).filterNotNull().toList(),
                (it.get(positionNamesField) ?: emptyArray<String>()).filterNotNull().toList()
            )
        }
    }

    @GetMapping("/{id}")
    fun view(@PathVariable("id") id: Long): UserVo {
        val record = userService.findOne(id) ?: throw NotFound()
        return UserVo.of(record)
    }

    @GetMapping("/{id}/detail")
    fun detail(@PathVariable("id") id: Long): UserDetail {
        return userService.detail(id) ?: throw NotFound()
    }
}
