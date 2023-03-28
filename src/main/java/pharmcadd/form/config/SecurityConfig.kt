package pharmcadd.form.config

import com.nimbusds.jose.shaded.json.JSONArray
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.env.Environment
import org.springframework.http.HttpStatus
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import org.springframework.security.web.authentication.HttpStatusEntryPoint
import pharmcadd.form.common.extension.isLocal
import pharmcadd.form.common.extension.isProd
import pharmcadd.form.common.security.MemberDetailsService

@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true, securedEnabled = true)
class SecurityConfig : WebSecurityConfigurerAdapter() {

    @Autowired
    lateinit var memberDetailsService: MemberDetailsService

    @Autowired
    lateinit var passwordEncoder: PasswordEncoder

    @Autowired
    lateinit var environment: Environment

    @Bean
    override fun authenticationManagerBean(): AuthenticationManager {
        return super.authenticationManagerBean()
    }

    override fun configure(auth: AuthenticationManagerBuilder) {
        auth
            .userDetailsService(memberDetailsService)
            .passwordEncoder(passwordEncoder)
    }

    override fun configure(security: HttpSecurity) {
        security
            .csrf().disable()
            .headers().frameOptions().disable()
            .and()
            .authorizeRequests().antMatchers("/oauth/**", "/.well-known/jwks.json").permitAll()
            .and()
//            .formLogin().and()
            .httpBasic()
            // NOTE : https://stackoverflow.com/questions/31424196/disable-browser-authentication-dialog-in-spring-security
            .authenticationEntryPoint(HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED))
            .and()
            .authorizeRequests {
                if (environment.isLocal) {
                    it
                        .anyRequest().permitAll()
                } else if (environment.isProd) {
                    it
                        .antMatchers("/valid-email", "/valid-code/**", "/join", "/password/reset").permitAll()
                        .antMatchers("/admin/**").hasAnyRole("ADMIN", "CAMPAIGN_ADMIN")
                        .anyRequest().authenticated()
                } else {
                    throw RuntimeException("error")
                }
            }
            .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            .and()
            .oauth2ResourceServer {
                it
                    .jwt()
                    .jwtAuthenticationConverter { source: Jwt ->
                        val claims = source.claims
                        val roles = claims["authorities"] as JSONArray
                        val authorities =
                            roles.map { role -> SimpleGrantedAuthority("ROLE_" + role.toString().uppercase()) }
                        JwtAuthenticationToken(source, authorities, claims["user_name"].toString())
                    }
            }
    }
}
