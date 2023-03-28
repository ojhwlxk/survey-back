package pharmcadd.form.common.security

import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.UserDetails
import pharmcadd.form.jooq.tables.records.UserRecord

class MemberDetails(val user: UserRecord) : UserDetails {

    override fun getAuthorities(): List<GrantedAuthority> =
        listOf(SimpleGrantedAuthority(user.role.name.uppercase()))

    override fun getPassword(): String = user.password

    override fun getUsername(): String = user.username

    override fun isAccountNonExpired(): Boolean = true

    override fun isAccountNonLocked(): Boolean = true

    override fun isCredentialsNonExpired(): Boolean = true

    override fun isEnabled(): Boolean = user.active
}
