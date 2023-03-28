package pharmcadd.form.common.security

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.stereotype.Service
import pharmcadd.form.service.UserService

@Service
class MemberDetailsService : UserDetailsService {

    @Autowired
    lateinit var userService: UserService

    override fun loadUserByUsername(username: String): UserDetails {
        val record = userService.findByUsernameOrEmail(username) ?: throw UsernameNotFoundException(username)
        return MemberDetails(record)
    }
}
