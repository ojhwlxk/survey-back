package pharmcadd.form.service

import org.jooq.DSLContext
import org.jooq.Result
import org.jooq.UpdateConditionStep
import org.jooq.impl.DSL
import org.jooq.util.postgres.PostgresDSL
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import pharmcadd.form.common.extension.batch
import pharmcadd.form.jooq.Tables.GROUP
import pharmcadd.form.jooq.Tables.PARTICIPANT
import pharmcadd.form.jooq.enums.ParticipantType
import pharmcadd.form.jooq.tables.records.ParticipantRecord

@Service
@Transactional
class ParticipantService {

    @Autowired
    lateinit var dsl: DSLContext

    fun add(campaignId: Long, type: ParticipantType, userId: Long?, groupId: Long?): Long {
        return dsl.insertInto(PARTICIPANT)
            .set(PARTICIPANT.CAMPAIGN_ID, campaignId)
            .set(PARTICIPANT.TYPE, type)
            .set(PARTICIPANT.USER_ID, userId)
            .set(PARTICIPANT.GROUP_ID, groupId)
            .returningResult(PARTICIPANT.ID)
            .fetchOne()!!
            .value1()!!
    }

    fun addUsers(campaignId: Long, userIds: List<Long>) {
        userIds
            .toSet()
            .map { userId ->
                dsl.insertInto(PARTICIPANT)
                    .set(PARTICIPANT.CAMPAIGN_ID, campaignId)
                    .set(PARTICIPANT.TYPE, ParticipantType.USER)
                    .set(PARTICIPANT.USER_ID, userId)
                    .setNull(PARTICIPANT.GROUP_ID)
            }
            .batch(dsl)
    }

    fun addUser(campaignId: Long, userId: Long): Long {
        return add(campaignId, ParticipantType.USER, userId, null)
    }

    fun addGroups(campaignId: Long, groupIds: List<Long>) {
        groupIds
            .toSet()
            .map { groupId ->
                dsl.insertInto(PARTICIPANT)
                    .set(PARTICIPANT.CAMPAIGN_ID, campaignId)
                    .set(PARTICIPANT.TYPE, ParticipantType.GROUP)
                    .setNull(PARTICIPANT.USER_ID)
                    .set(PARTICIPANT.GROUP_ID, groupId)
            }
            .batch(dsl)
    }

    fun addGroupsWithSubgroup(campaignId: Long, groupIds: List<Long>) {
        dsl
            .insertInto(
                PARTICIPANT,
                PARTICIPANT.CAMPAIGN_ID,
                PARTICIPANT.TYPE,
                PARTICIPANT.USER_ID,
                PARTICIPANT.GROUP_ID
            )
            .select(
                dsl
                    .select(
                        DSL.`val`(campaignId),
                        DSL.`val`(ParticipantType.GROUP),
                        DSL.`val`(null, Long::class.java),
                        GROUP.ID
                    )
                    .from(GROUP)
                    .where(
                        PostgresDSL.arrayOverlap(GROUP.PATHWAYS, groupIds.toSet().toTypedArray())
                            .and(GROUP.DELETED_AT.isNull)
                    )
            )
            .execute()
    }

    fun addGroup(campaignId: Long, groupId: Long, includeSubgroup: Boolean) {
        if (includeSubgroup) {
            addGroupsWithSubgroup(campaignId, listOf(groupId))
        } else {
            add(campaignId, ParticipantType.GROUP, null, groupId)
        }
    }

    @Transactional(readOnly = true)
    fun findOne(id: Long): ParticipantRecord? {
        return dsl
            .selectFrom(PARTICIPANT)
            .where(
                PARTICIPANT.ID.eq(id)
                    .and(PARTICIPANT.DELETED_AT.isNull)
            )
            .fetchOne()
    }

    @Transactional(readOnly = true)
    fun findByCampaignId(campaignId: Long): Result<ParticipantRecord> {
        return dsl
            .selectFrom(PARTICIPANT)
            .where(
                PARTICIPANT.CAMPAIGN_ID.eq(campaignId)
                    .and(PARTICIPANT.DELETED_AT.isNull)
            )
            .fetch()
    }

    private fun delete(block: (UpdateConditionStep<ParticipantRecord>) -> Unit) {
        dsl.update(PARTICIPANT)
            .set(PARTICIPANT.UPDATED_AT, DSL.currentOffsetDateTime())
            .set(PARTICIPANT.DELETED_AT, DSL.currentOffsetDateTime())
            .where(
                PARTICIPANT.DELETED_AT.isNull
            )
            .also(block)
            .execute()
    }

    fun deleteDuplicateByCampaignId(campaignId: Long) = delete { query ->
        val p = PARTICIPANT.`as`("p")

        query
            .and(PARTICIPANT.CAMPAIGN_ID.eq(campaignId))
            .and(
                PARTICIPANT.ID.`in`(
                    dsl
                        .select(
                            DSL.field("id", Long::class.java)
                        )
                        .from(
                            dsl
                                .select(
                                    p.ID.`as`("id"),
                                    DSL.rowNumber()
                                        .over()
                                        .partitionBy(
                                            p.TYPE,
                                            p.USER_ID,
                                            p.GROUP_ID
                                        )
                                        .orderBy(p.ID)
                                        .`as`("row_num")
                                )
                                .from(p)
                                .where(
                                    p.CAMPAIGN_ID.eq(campaignId)
                                        .and(p.DELETED_AT.isNull)
                                )
                        )
                        .where(
                            DSL.field("row_num", Int::class.java).greaterThan(1)
                        )
                )
            )
    }

    fun deleteById(id: Long) = delete { it.and(PARTICIPANT.ID.eq(id)) }

    fun deleteByCampaignId(campaignId: Long) = delete { it.and(PARTICIPANT.CAMPAIGN_ID.eq(campaignId)) }

    fun deleteByGroupId(groupId: Long) {
        delete {
            it
                .and(
                    PARTICIPANT.GROUP_ID.`in`(
                        dsl
                            .select(GROUP.ID)
                            .from(GROUP)
                            .where(
                                GROUP.PATHWAYS.contains(arrayOf(groupId))
                                    .and(GROUP.DELETED_AT.isNull)
                            )
                    )
                )
        }
    }

    fun deleteByUserId(userId: Long) = delete { it.and(PARTICIPANT.USER_ID.eq(userId)) }
}
