package pharmcadd.form.controller.front.form

data class ResetPasswordForm(
    val email: String,
    val code: String,
    val newPassword: String,
)
