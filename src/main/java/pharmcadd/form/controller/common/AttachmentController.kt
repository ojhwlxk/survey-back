package pharmcadd.form.controller.common

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.io.InputStreamResource
import org.springframework.core.io.Resource
import org.springframework.http.ContentDisposition
import org.springframework.http.HttpHeaders
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import pharmcadd.form.common.controller.BaseController
import pharmcadd.form.common.exception.NotFound
import pharmcadd.form.jooq.tables.pojos.Attachment
import pharmcadd.form.service.AttachmentService
import java.io.FileInputStream
import java.nio.charset.StandardCharsets
import java.util.*

@RestController
@RequestMapping("/attachments")
class AttachmentController : BaseController() {

    @Autowired
    lateinit var attachmentService: AttachmentService

    @PostMapping
    fun add(@RequestParam("file") file: MultipartFile): Attachment {
        val id = attachmentService.add(securityService.userId, file)
        return attachmentService.findOne(id)!!.into(Attachment::class.java)!!
    }

    @GetMapping("/{key}")
    fun view(@PathVariable("key") key: UUID): ResponseEntity<Resource> {
        val record = attachmentService.findByKey(key) ?: throw NotFound()
        val file = attachmentService.loadAsResource(record.uri) ?: throw NotFound()

        val headers = HttpHeaders().apply {
            contentLength = file.length()
            contentDisposition = ContentDisposition.builder("attachment")
                .filename(file.name, StandardCharsets.UTF_8)
                .build()
        }
        return ResponseEntity.ok().headers(headers).body(InputStreamResource(FileInputStream(file)))
    }
}
