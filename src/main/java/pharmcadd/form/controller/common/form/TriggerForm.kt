package pharmcadd.form.controller.common.form

data class TriggerForm(
    val cronExpression: String,
    val repeat: Int,
    val timeZoneId: Long? = null,
)
