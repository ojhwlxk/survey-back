package pharmcadd.form.controller.front

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import pharmcadd.form.common.controller.BaseController
import pharmcadd.form.common.exception.NotFound
import pharmcadd.form.jooq.tables.pojos.Position
import pharmcadd.form.service.PositionService

@RestController
@RequestMapping("/positions")
class PositionController : BaseController() {

    @Autowired
    lateinit var positionService: PositionService

    @GetMapping
    fun list(): List<Position> {
        return positionService.findAll().map { it.into(Position::class.java) }
    }

    @GetMapping("/{id}")
    fun view(@PathVariable("id") id: Long): Position {
        return positionService.findOne(id)?.into(Position::class.java) ?: throw NotFound()
    }
}
