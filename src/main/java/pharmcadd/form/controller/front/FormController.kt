package pharmcadd.form.controller.front

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import pharmcadd.form.common.controller.BaseController
import pharmcadd.form.common.exception.NotFound
import pharmcadd.form.jooq.tables.pojos.Form
import pharmcadd.form.jooq.tables.pojos.Question
import pharmcadd.form.model.FormVo
import pharmcadd.form.service.FormService

@RestController
@RequestMapping("/forms")
class FormController : BaseController() {

    @Autowired
    lateinit var formService: FormService

    @GetMapping("/{id}")
    fun view(@PathVariable("id") id: Long): Form {
        return formService.findOne(id)?.into(Form::class.java) ?: throw NotFound()
    }

    @GetMapping("/{id}/questions")
    fun questions(@PathVariable("id") id: Long): List<Question> {
        return formService.questions(id)
            .map { it.into(Question::class.java) }
    }

    @GetMapping("/{id}/detail")
    fun detail(@PathVariable("id") id: Long): FormVo {
        return formService.detail(id) ?: throw NotFound()
    }
}
