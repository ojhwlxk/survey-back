package pharmcadd.form.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.mail.javamail.JavaMailSenderImpl

@Configuration
class MailConfig {

    @Bean
    fun javaMailSender(): JavaMailSender {
        return JavaMailSenderImpl().apply {
            protocol = "smtps"
            host = "smtps.hiworks.com"
            port = 465
            username = "pharmulator@pharmcadd.com"
            password = "ph4rmc4dd!"
            defaultEncoding = "UTF-8"
        }
    }
}
