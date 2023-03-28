package pharmcadd.form.controller.admin.form

data class AdminGroupForm(
    val name: String,
    val parentId: Long? = null,
)
