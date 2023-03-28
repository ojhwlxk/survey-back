package pharmcadd.form.config

import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.jwk.JWKSet
import com.nimbusds.jose.jwk.KeyUse
import com.nimbusds.jose.jwk.RSAKey
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.io.ClassPathResource
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.jwt.JwtHelper
import org.springframework.security.jwt.crypto.sign.RsaSigner
import org.springframework.security.oauth2.common.OAuth2AccessToken
import org.springframework.security.oauth2.common.util.JsonParserFactory
import org.springframework.security.oauth2.config.annotation.configurers.ClientDetailsServiceConfigurer
import org.springframework.security.oauth2.config.annotation.web.configuration.AuthorizationServerConfigurerAdapter
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableAuthorizationServer
import org.springframework.security.oauth2.config.annotation.web.configurers.AuthorizationServerEndpointsConfigurer
import org.springframework.security.oauth2.config.annotation.web.configurers.AuthorizationServerSecurityConfigurer
import org.springframework.security.oauth2.provider.OAuth2Authentication
import org.springframework.security.oauth2.provider.token.TokenStore
import org.springframework.security.oauth2.provider.token.store.JwtAccessTokenConverter
import org.springframework.security.oauth2.provider.token.store.JwtTokenStore
import org.springframework.security.oauth2.provider.token.store.KeyStoreKeyFactory
import pharmcadd.form.common.security.MemberDetails
import pharmcadd.form.common.security.MemberDetailsService
import java.security.KeyPair
import java.security.interfaces.RSAPrivateKey
import java.security.interfaces.RSAPublicKey

// NOTE : 우선은 파뮬레이터랑 동일하게 세팅 해두었는데, 특별한 이유없이 세팅 손대지 말 것
@Configuration
@EnableAuthorizationServer
class AuthorizationServerConfig : AuthorizationServerConfigurerAdapter() {

    companion object {
        private const val JWK_KID = "mdss-key-id"
    }

    @Autowired
    lateinit var passwordEncoder: PasswordEncoder

    @Autowired
    lateinit var authenticationManager: AuthenticationManager

    @Autowired
    lateinit var memberDetailsService: MemberDetailsService

    @Bean
    fun accessTokenConverter(): JwtAccessTokenConverter =
        JwtHeadersAccessTokenConverter(mapOf("kid" to JWK_KID), keyPair())

    @Bean
    fun keyPair(): KeyPair {
        val resource = ClassPathResource("mdss-jwt.jks")
        val keyStoreKeyFactory = KeyStoreKeyFactory(resource, "ph4rmc4dd".toCharArray())
        return keyStoreKeyFactory.getKeyPair("mdss-oauth-jwt")
    }

    @Bean
    fun jwkSet(): JWKSet {
        val builder = RSAKey.Builder(keyPair().public as RSAPublicKey)
            .keyUse(KeyUse.SIGNATURE)
            .algorithm(JWSAlgorithm.RS256)
            .keyID(JWK_KID)
        return JWKSet(builder.build())
    }

    override fun configure(clients: ClientDetailsServiceConfigurer) {
        clients
            .inMemory()
            .withClient("pharmulator")
            .secret(passwordEncoder.encode("ph4rmc4dd"))
            .authorizedGrantTypes("authorization_code", "password", "refresh_token")
            .scopes("read", "write")
            .accessTokenValiditySeconds(60 * 60)
            .refreshTokenValiditySeconds(6 * 60 * 60)
            .autoApprove(true)
    }

    override fun configure(endpoints: AuthorizationServerEndpointsConfigurer) {
        endpoints
            .authenticationManager(authenticationManager)
            .userDetailsService(memberDetailsService)
            .tokenStore(tokenStore())
            .accessTokenConverter(accessTokenConverter())
    }

    override fun configure(security: AuthorizationServerSecurityConfigurer) {
        security
            .tokenKeyAccess("permitAll()")
            .checkTokenAccess("permitAll()")
            .allowFormAuthenticationForClients()
    }

    @Bean
    fun tokenStore(): TokenStore = JwtTokenStore(accessTokenConverter())
}

class JwtHeadersAccessTokenConverter(
    private val headers: Map<String, String>,
    keyPair: KeyPair
) : JwtAccessTokenConverter() {

    private val objectMapper = JsonParserFactory.create()
    private val signer = RsaSigner(keyPair.private as RSAPrivateKey)

    init {
        setKeyPair(keyPair)
    }

    override fun encode(accessToken: OAuth2AccessToken, authentication: OAuth2Authentication): String {
        val userDetails = authentication.principal as MemberDetails
        val member = userDetails.user
        accessToken.additionalInformation.apply {
            put("name", member.name)
            put("email", member.email)
            put("user_id", member.id)
        }
        val content = try {
            objectMapper.formatMap(accessTokenConverter.convertAccessToken(accessToken, authentication))
        } catch (e: Exception) {
            throw IllegalStateException("Cannot convert access token to JSON", e)
        }
        return JwtHelper.encode(content, signer, headers).encoded
    }
}
