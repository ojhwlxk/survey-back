package pharmcadd.form.controller.front.form

data class EmailForm(
    val email: String,
)

data class ValidCodeConfirmForm(
    val email: String,
    val code: String,
)

data class JoinForm(
    val code: String,
    val name: String,
    val username: String,
    val password: String,
    val email: String,
    val timeZoneId: Long,
    val groups: List<Group> = emptyList(),
) {
    data class Group(val groupId: Long, val positionId: Long?)
}
