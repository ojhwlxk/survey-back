package pharmcadd.form.service

import org.jooq.DSLContext
import org.jooq.Result
import org.jooq.impl.DSL
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import pharmcadd.form.common.exception.NotFound
import pharmcadd.form.common.extension.batch
import pharmcadd.form.jooq.Sequences
import pharmcadd.form.jooq.Tables.*
import pharmcadd.form.jooq.tables.records.GroupRecord
import pharmcadd.form.jooq.tables.records.UserRecord

@Service
@Transactional
class GroupService {

    @Autowired
    lateinit var dsl: DSLContext

    @Autowired
    lateinit var formScheduleParticipantService: FormScheduleParticipantService

    @Autowired
    lateinit var participantService: ParticipantService

    fun add(name: String, parentId: Long? = null): Long {
        val newId = dsl.nextval(Sequences.GROUP_ID_SEQ)
        val insert = dsl.insertInto(GROUP)
            .set(GROUP.ID, newId)
            .set(GROUP.NAME, name)

        if (parentId != null) {
            val parent = findOne(parentId) ?: throw NotFound()

            insert
                .set(GROUP.PARENT_ID, parentId)
                .set(GROUP.LEVEL, parent.level + 1)
                .set(GROUP.PATHWAYS, parent.pathways + newId)
        } else {
            insert
                .set(GROUP.LEVEL, 1)
                .set(GROUP.PATHWAYS, arrayOf(newId))
        }

        return insert
            .returningResult(GROUP.ID)
            .fetchOne()!!
            .value1()!!
    }

    fun modify(id: Long, name: String, parentId: Long? = null) {
        val prev = findOne(id) ?: throw NotFound()

        dsl.update(GROUP)
            .set(GROUP.NAME, name)
            .set(GROUP.PARENT_ID, parentId)
            .set(GROUP.UPDATED_AT, DSL.currentOffsetDateTime())
            .where(
                GROUP.ID.eq(id)
                    .and(GROUP.DELETED_AT.isNull)
            )
            .execute()

        if (prev.parentId != parentId) {
            dsl
                .selectFrom(GROUP_VIEW)
                .where(
                    GROUP_VIEW.PATHWAYS.contains(arrayOf(id))
                )
                .fetch()
                .map {
                    dsl.update(GROUP)
                        .set(GROUP.LEVEL, it.level)
                        .set(GROUP.PATHWAYS, it.pathways)
                        .set(GROUP.UPDATED_AT, DSL.currentOffsetDateTime())
                        .where(
                            GROUP.ID.eq(it.id)
                        )
                }
                .batch(dsl)
        }

        // name 변경 시 데이터 보정
        // TODO : answer 테이블 group_id 를 찾아서 text 컬럼 업데이트 처리, answer_stat 테이블 업데이트 처리
    }

    @Transactional(readOnly = true)
    fun users(id: Long): List<UserRecord> {
        return dsl
            .select(
                *USER.fields()
            )
            .from(USER)
            .join(GROUP_USER).on(GROUP_USER.USER_ID.eq(USER.ID).and(GROUP_USER.DELETED_AT.isNull))
            .join(GROUP).on(GROUP_USER.GROUP_ID.eq(GROUP.ID).and(GROUP.DELETED_AT.isNull))
            .where(
                GROUP.ID.eq(id)
                    .and(USER.DELETED_AT.isNull)
            )
            .fetch { it.into(USER) }
    }

    @Transactional(readOnly = true)
    fun findAll(): Result<GroupRecord> {
        return dsl
            .selectFrom(GROUP)
            .where(
                GROUP.DELETED_AT.isNull
            )
            .fetch()
    }

    @Transactional(readOnly = true)
    fun findOne(id: Long): GroupRecord? {
        return dsl
            .selectFrom(GROUP)
            .where(
                GROUP.ID.eq(id)
                    .and(GROUP.DELETED_AT.isNull)
            )
            .fetchOne()
    }

    @Transactional(readOnly = true)
    fun findByPathways(id: Long): Result<GroupRecord> {
        return dsl
            .selectFrom(GROUP)
            .where(
                GROUP.PATHWAYS.contains(arrayOf(id))
                    .and(GROUP.DELETED_AT.isNull)
            )
            .fetch()
    }

    @Transactional(readOnly = true)
    fun findByParentId(parentId: Long): Result<GroupRecord> {
        return dsl
            .selectFrom(GROUP)
            .where(
                GROUP.PARENT_ID.eq(parentId)
                    .and(GROUP.DELETED_AT.isNull)
            )
            .fetch()
    }

    @Transactional(readOnly = true)
    fun findByUserId(userId: Long): List<GroupRecord> {
        return dsl
            .select(
                *GROUP.fields()
            )
            .from(GROUP_USER)
            .join(GROUP).on(GROUP.ID.eq(GROUP_USER.GROUP_ID).and(GROUP.DELETED_AT.isNull))
            .and(
                GROUP_USER.USER_ID.eq(userId)
                    .and(GROUP_USER.DELETED_AT.isNull)
            )
            .fetch { it.into(GROUP) }
    }

    fun deleteById(id: Long) {
        formScheduleParticipantService.deleteByGroupId(id)

        participantService.deleteByGroupId(id)

        dsl.update(GROUP_USER)
            .set(GROUP_USER.UPDATED_AT, DSL.currentOffsetDateTime())
            .set(GROUP_USER.DELETED_AT, DSL.currentOffsetDateTime())
            .where(
                GROUP_USER.DELETED_AT.isNull
                    .and(
                        GROUP_USER.GROUP_ID.`in`(
                            dsl
                                .select(GROUP.ID)
                                .from(GROUP)
                                .where(
                                    GROUP.PATHWAYS.contains(arrayOf(id))
                                        .and(GROUP.DELETED_AT.isNull)
                                )
                        )
                    )
            )
            .execute()

        dsl.update(GROUP)
            .set(GROUP.UPDATED_AT, DSL.currentOffsetDateTime())
            .set(GROUP.DELETED_AT, DSL.currentOffsetDateTime())
            .where(
                GROUP.PATHWAYS.contains(arrayOf(id))
                    .and(GROUP.DELETED_AT.isNull)
            )
            .execute()
    }
}
