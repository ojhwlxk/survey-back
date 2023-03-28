package pharmcadd.form.common.service

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import mu.KotlinLogging
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.mail.javamail.MimeMessageHelper
import org.springframework.stereotype.Component
import java.io.File
import kotlin.properties.Delegates

data class MailForm(
    val to: List<String>,
    val title: String,
    val content: String,
    val attachments: List<File> = emptyList(),
    val embeddedImages: List<File> = emptyList()
) {
    class Builder {
        private val to: MutableList<String> by lazy { arrayListOf() }
        private var title: String by Delegates.notNull()
        private var content: String by Delegates.notNull()
        private val embeddedImages: MutableList<File> by lazy { arrayListOf() }
        private val attachments: MutableList<File> by lazy { arrayListOf() }

        fun to(vararg to: String): Builder {
            this.to.addAll(to)
            return this
        }

        fun title(title: String): Builder {
            this.title = title
            return this
        }

        fun content(content: String): Builder {
            this.content = content
            return this
        }

        fun attachments(vararg file: File): Builder {
            this.attachments.addAll(file)
            return this
        }

        fun embeddedImages(vararg file: File): Builder {
            this.embeddedImages.addAll(file)
            return this
        }

        fun build(): MailForm = MailForm(to, title, content, attachments, embeddedImages)
    }
}

@Component
class MailService(
    private val javaMailSender: JavaMailSender,
    private val templateService: TemplateService
) {
    companion object {
        const val maxUploadSize: Long = (1024 * 1024 * 25).toLong()
    }

    private val logger = KotlinLogging.logger { }

    suspend fun send(form: MailForm) {
        val files = form.attachments + form.embeddedImages
        if (files.isNotEmpty()) {
            for (attachment in form.attachments + form.embeddedImages) {
                if (attachment.length() > maxUploadSize) {
                    /*
                    클래스패스에 뻔히 있지만 로드타임에서 로드하지 못한다
                    java.lang.NoClassDefFoundError: org/springframework/web/multipart/MaxUploadSizeExceededException
                    throw MaxUploadSizeExceededException(maxUploadSize)
                    */

                    throw RuntimeException("maxUploadSize : $maxUploadSize")
                }
            }
        }

        val attachments = form.attachments
        val multipart = attachments.isNotEmpty()

        val mimeMessage = javaMailSender.createMimeMessage()

        val messageHelper = MimeMessageHelper(mimeMessage, multipart, "UTF-8").apply {
            setFrom("pharmulator@pharmcadd.com")
            setTo(form.to.toTypedArray())
            setSubject(form.title)
            setText(templateService.compile("template/mail.html.hbs", form), true)
            for (attachment in attachments) {
                addAttachment(attachment.name, attachment)
            }
            for (embeddedImage in form.embeddedImages) {
                addInline(embeddedImage.name, embeddedImage)
            }
        }

        try {
            javaMailSender.send(messageHelper.mimeMessage)
        } catch (e: Exception) {
            logger.error(e) { "mail send error : ${e.localizedMessage}" }
        }
    }

    suspend fun send(block: MailForm.Builder.() -> Unit) = send(MailForm.Builder().apply(block).build())

    @Suppress("DeferredResultUnused")
    fun sendAndForget(block: MailForm.Builder.() -> Unit) {
        GlobalScope.async(Dispatchers.IO) { send(block) }
    }
}
