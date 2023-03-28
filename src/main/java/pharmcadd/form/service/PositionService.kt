package pharmcadd.form.service

import org.jooq.DSLContext
import org.jooq.Result
import org.jooq.impl.DSL
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import pharmcadd.form.common.extension.batch
import pharmcadd.form.jooq.Tables.GROUP_USER
import pharmcadd.form.jooq.Tables.POSITION
import pharmcadd.form.jooq.tables.records.PositionRecord

@Service
@Transactional
class PositionService {

    @Autowired
    lateinit var dsl: DSLContext

    fun add(names: List<String>) {
        names
            .map {
                dsl.insertInto(POSITION)
                    .set(POSITION.NAME, it)
            }
            .batch(dsl)
    }

    fun add(name: String): Long {
        return dsl.insertInto(POSITION)
            .set(POSITION.NAME, name)
            .returningResult(POSITION.ID)
            .fetchOne()!!
            .value1()!!
    }

    fun modify(id: Long, name: String) {
        dsl.update(POSITION)
            .set(POSITION.NAME, name)
            .set(POSITION.UPDATED_AT, DSL.currentOffsetDateTime())
            .where(
                POSITION.ID.eq(id)
                    .and(POSITION.DELETED_AT.isNull)
            )
            .execute()
    }

    @Transactional(readOnly = true)
    fun findAll(): Result<PositionRecord> {
        return dsl
            .selectFrom(POSITION)
            .where(
                POSITION.DELETED_AT.isNull
            )
            .fetch()
    }

    @Transactional(readOnly = true)
    fun findOne(id: Long): PositionRecord? {
        return dsl
            .selectFrom(POSITION)
            .where(
                POSITION.ID.eq(id)
                    .and(POSITION.DELETED_AT.isNull)

            )
            .fetchOne()
    }

    fun deleteById(id: Long) {
        dsl.update(POSITION)
            .set(POSITION.UPDATED_AT, DSL.currentOffsetDateTime())
            .set(POSITION.DELETED_AT, DSL.currentOffsetDateTime())
            .where(
                POSITION.ID.eq(id)
                    .and(POSITION.DELETED_AT.isNull)
            )
            .execute()

        dsl.update(GROUP_USER)
            .set(GROUP_USER.UPDATED_AT, DSL.currentOffsetDateTime())
            .set(GROUP_USER.DELETED_AT, DSL.currentOffsetDateTime())
            .where(
                GROUP_USER.POSITION_ID.eq(id)
                    .and(GROUP_USER.DELETED_AT.isNull)
            )
            .execute()
    }
}
