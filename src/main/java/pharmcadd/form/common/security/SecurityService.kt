package pharmcadd.form.common.security

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.env.Environment
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import org.springframework.stereotype.Component
import pharmcadd.form.common.exception.NotFound
import pharmcadd.form.common.extension.isLocal
import pharmcadd.form.jooq.enums.UserRole

@Component
class SecurityService {

    @Autowired
    lateinit var environment: Environment

    fun currentUser(): JwtAuthenticationToken? =
        SecurityContextHolder.getContext().authentication as? JwtAuthenticationToken

    private fun hasRole(vararg role: UserRole): Boolean {
        val roles = role.map { "ROLE_" + it.name }
        return currentUser()?.authorities?.any { roles.contains(it.authority) } ?: false
    }

    val userId: Long
        get() {
            val userId = currentUser()?.tokenAttributes?.get("user_id") as? Long
            return userId ?: if (environment.isLocal) {
                1L
            } else {
                throw NotFound()
            }
        }

    val isAdmin: Boolean
        get() = hasRole(UserRole.ADMIN)

    val isCampaignAdmin: Boolean
        get() = hasRole(UserRole.CAMPAIGN_ADMIN)

    val isManageCampaign: Boolean
        get() = hasRole(UserRole.ADMIN, UserRole.CAMPAIGN_ADMIN)
}
