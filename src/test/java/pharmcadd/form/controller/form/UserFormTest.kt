package pharmcadd.form.controller.form

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.junit.jupiter.api.Test
import pharmcadd.form.controller.admin.form.AdminUserAddForm
import pharmcadd.form.jooq.enums.UserRole

internal class UserFormTest {

    @Test
    fun test() {
        val user = AdminUserAddForm(
            "테스트",
            "test",
            "1234",
            "test@test.com",
            UserRole.CAMPAIGN_ADMIN,
            1,
            true
        )

        val jacksonObjectMapper = jacksonObjectMapper()
        val json = jacksonObjectMapper.writeValueAsString(user)
        println(json)
    }
}
