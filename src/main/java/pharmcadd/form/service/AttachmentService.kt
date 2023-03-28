package pharmcadd.form.service

import mu.KotlinLogging
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile
import pharmcadd.form.jooq.Tables.ATTACHMENT
import pharmcadd.form.jooq.tables.records.AttachmentRecord
import java.io.File
import java.util.*

@Service
@Transactional
class AttachmentService {

    private val logger = KotlinLogging.logger { }

    @Autowired
    lateinit var dsl: DSLContext

    @Value("\${storage.upload-dir}")
    lateinit var uploadDir: String

    private val root by lazy { File(uploadDir).apply { mkdirs() } }

    fun add(userId: Long, file: MultipartFile): Long {
        val originalFilename = file.originalFilename!!
        val extension = originalFilename.substringAfterLast('.', "")

        val dest = root.resolve("${UUID.randomUUID()}.$extension")
        file.transferTo(dest)

        return dsl.insertInto(ATTACHMENT)
            .set(ATTACHMENT.NAME, originalFilename)
            .set(ATTACHMENT.SIZE, file.size)
            .set(ATTACHMENT.URI, "file://${dest.name}")
            .set(ATTACHMENT.CREATED_BY, userId)
            .returningResult(ATTACHMENT.ID)
            .fetchOne()!!
            .value1()!!
    }

    fun loadAsResource(uri: String): File? {
        if (uri.startsWith("file://")) {
            return root.resolve(uri.substringAfter("file://"))
        }
        return null
    }

    @Transactional(readOnly = true)
    fun findByKey(key: UUID): AttachmentRecord? {
        return dsl
            .selectFrom(ATTACHMENT)
            .where(
                ATTACHMENT.KEY.eq(key)
                    .and(ATTACHMENT.DELETED_AT.isNull)
            )
            .fetchOne()
    }

    @Transactional(readOnly = true)
    fun findOne(id: Long): AttachmentRecord? {
        return dsl
            .selectFrom(ATTACHMENT)
            .where(
                ATTACHMENT.ID.eq(id)
                    .and(ATTACHMENT.DELETED_AT.isNull)
            )
            .fetchOne()
    }

    fun deleteById(id: Long) {
        dsl.update(ATTACHMENT)
            .set(ATTACHMENT.UPDATED_AT, DSL.currentOffsetDateTime())
            .set(ATTACHMENT.DELETED_AT, DSL.currentOffsetDateTime())
            .where(
                ATTACHMENT.ID.eq(id)
                    .and(ATTACHMENT.DELETED_AT.isNull)
            )
            .execute()
    }
}
