package pharmcadd.form.common.service

import com.github.jknack.handlebars.Handlebars
import com.github.jknack.handlebars.io.ClassPathTemplateLoader
import mu.KotlinLogging
import org.springframework.stereotype.Component

@Component
class TemplateService {

    private val logger = KotlinLogging.logger { }

    private val loader by lazy { ClassPathTemplateLoader("/", "") }
    private val handlebars by lazy { Handlebars(loader) }

    @JvmOverloads
    fun compile(nameOfResource: String, obj: Any? = null): String {
        logger.debug { "nameOfResource : $nameOfResource" + if (obj != null) ", obj : $obj" else "" }

        val compile = handlebars.compile(nameOfResource)
        return compile.apply(obj)
    }

    @JvmOverloads
    fun compileInline(input: String, obj: Any? = null): String {
        logger.debug { "input : $input" + if (obj != null) ", obj : $obj" else "" }

        val compile = handlebars.compileInline(input)
        return compile.apply(obj)
    }
}
