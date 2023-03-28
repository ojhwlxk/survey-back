package pharmcadd.form.service

import org.jooq.DSLContext
import org.jooq.UpdateSetMoreStep
import org.jooq.impl.DSL
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import pharmcadd.form.common.extension.batch
import pharmcadd.form.controller.front.form.JoinForm
import pharmcadd.form.jooq.Tables.*
import pharmcadd.form.jooq.enums.UserRole
import pharmcadd.form.jooq.tables.records.UserRecord
import pharmcadd.form.model.UserDetail
import pharmcadd.form.model.UserVo

@Service
@Transactional
class UserService {

    @Autowired
    lateinit var dsl: DSLContext

    @Autowired
    lateinit var passwordEncoder: PasswordEncoder

    @Autowired
    lateinit var authorizationCodeService: AuthorizationCodeService

    @Autowired
    lateinit var timeZoneService: TimeZoneService

    fun detail(id: Long): UserDetail? {
        val user = findOne(id) ?: return null
        val timeZone = timeZoneService.findOne(user.timeZoneId)!!

        val groups = dsl
            .select(
                *GROUP.fields()
            )
            .select(
                *POSITION.fields()
            )
            .from(USER)
            .join(GROUP_USER).on(USER.ID.eq(GROUP_USER.USER_ID).and(GROUP_USER.DELETED_AT.isNull))
            .leftJoin(GROUP).on(GROUP_USER.GROUP_ID.eq(GROUP.ID).and(GROUP.DELETED_AT.isNull))
            .leftJoin(POSITION).on(GROUP_USER.POSITION_ID.eq(POSITION.ID).and(POSITION.DELETED_AT.isNull))
            .where(
                USER.ID.eq(id)
                    .and(USER.DELETED_AT.isNull)
            )
            .fetch {
                UserDetail.Group(
                    it.get(GROUP.ID),
                    it.get(GROUP.NAME),
                    it.get(POSITION.ID),
                    it.get(POSITION.NAME),
                )
            }

        return UserDetail(
            UserVo.of(user),
            timeZone,
            groups,
        )
    }

    fun join(form: JoinForm): Long {
        authorizationCodeService.deleteByEmail(form.email)

        val userId = add(
            form.name,
            form.username,
            form.password,
            form.email,
            UserRole.USER,
            form.timeZoneId,
            true
        )

        form.groups
            .map { (groupId, positionId) ->
                dsl.insertInto(GROUP_USER)
                    .set(GROUP_USER.USER_ID, userId)
                    .set(GROUP_USER.GROUP_ID, groupId)
                    .set(GROUP_USER.POSITION_ID, positionId)
            }
            .batch(dsl)

        return userId
    }

    fun add(
        name: String,
        username: String,
        password: String,
        email: String,
        role: UserRole,
        timeZoneId: Long,
        active: Boolean
    ): Long {
        return dsl.insertInto(USER)
            .set(USER.NAME, name)
            .set(USER.USERNAME, username)
            .set(USER.PASSWORD, passwordEncoder.encode(password))
            .set(USER.EMAIL, email)
            .set(USER.ROLE, role)
            .set(USER.TIME_ZONE_ID, timeZoneId)
            .set(USER.ACTIVE, active)
            .returningResult(USER.ID)
            .fetchOne()!!
            .value1()!!
    }

    private fun update(id: Long, block: (UpdateSetMoreStep<UserRecord>) -> Unit = { }) {
        dsl.update(USER)
            .set(USER.UPDATED_AT, DSL.currentOffsetDateTime())
            .also(block)
            .where(
                USER.ID.eq(id)
                    .and(USER.DELETED_AT.isNull)
            )
            .execute()
    }

    fun modify(
        id: Long,
        name: String,
        password: String,
        email: String,
        role: UserRole,
        timeZoneId: Long,
        active: Boolean
    ) {
        update(id) { query ->
            query
                .set(USER.NAME, name)
                .set(USER.PASSWORD, passwordEncoder.encode(password))
                .set(USER.EMAIL, email)
                .set(USER.ROLE, role)
                .set(USER.TIME_ZONE_ID, timeZoneId)
                .set(USER.ACTIVE, active)
        }

        // name 변경 시 데이터 보정
        // TODO : answer 테이블 user_id 를 찾아서 text 컬럼 업데이트 처리, answer_stat 테이블 업데이트 처리
    }

    fun changePassword(id: Long, password: String) {
        update(id) { query -> query.set(USER.PASSWORD, passwordEncoder.encode(password)) }

        // TODO : 비밀번호 변경 노티
    }

    fun changeRole(id: Long, role: UserRole) {
        update(id) { query -> query.set(USER.ROLE, role) }
    }

    @Transactional(readOnly = true)
    fun findOne(id: Long): UserRecord? {
        return dsl
            .selectFrom(USER)
            .where(
                USER.ID.eq(id)
                    .and(USER.DELETED_AT.isNull)
            )
            .fetchOne()
    }

    @Transactional(readOnly = true)
    fun findByEmail(email: String): UserRecord? {
        return dsl
            .selectFrom(USER)
            .where(
                USER.EMAIL.eq(email)
                    .and(USER.DELETED_AT.isNull)
            )
            .fetchOne()
    }

    @Transactional(readOnly = true)
    fun findByUsername(username: String): UserRecord? {
        return dsl
            .selectFrom(USER)
            .where(
                USER.USERNAME.eq(username)
                    .and(USER.DELETED_AT.isNull)
            )
            .fetchOne()
    }

    @Transactional(readOnly = true)
    fun findByUsernameOrEmail(usernameOrEmail: String): UserRecord? {
        return dsl
            .selectFrom(USER)
            .where(
                USER.DELETED_AT.isNull
                    .and(USER.USERNAME.eq(usernameOrEmail).or(USER.EMAIL.eq(usernameOrEmail)))
            )
            .fetchOne()
    }

    fun addGroup(id: Long, groupId: Long, positionId: Long? = null) {
        dsl.insertInto(GROUP_USER)
            .set(GROUP_USER.USER_ID, id)
            .set(GROUP_USER.GROUP_ID, groupId)
            .set(GROUP_USER.POSITION_ID, positionId)
            .execute()
    }

    fun deleteGroup(id: Long, groupId: Long) {
        dsl.update(GROUP_USER)
            .set(GROUP_USER.UPDATED_AT, DSL.currentOffsetDateTime())
            .set(GROUP_USER.DELETED_AT, DSL.currentOffsetDateTime())
            .where(
                GROUP_USER.USER_ID.eq(id)
                    .and(GROUP_USER.GROUP_ID.eq(groupId))
                    .and(GROUP_USER.DELETED_AT.isNull)
            )
            .execute()
    }

    fun deletePosition(id: Long, positionId: Long) {
        dsl.update(GROUP_USER)
            .setNull(GROUP_USER.POSITION_ID)
            .set(GROUP_USER.UPDATED_AT, DSL.currentOffsetDateTime())
            .where(
                GROUP_USER.USER_ID.eq(id)
                    .and(GROUP_USER.POSITION_ID.eq(positionId))
                    .and(GROUP_USER.DELETED_AT.isNull)
            )
            .execute()
    }

    fun active(id: Long) {
        dsl.update(USER)
            .set(USER.ACTIVE, true)
            .set(USER.UPDATED_AT, DSL.currentOffsetDateTime())
            .where(
                USER.ID.eq(id)
                    .and(USER.DELETED_AT.isNull)
            )
            .execute()
    }

    fun inactive(id: Long) {
        dsl.update(USER)
            .set(USER.ACTIVE, false)
            .set(USER.UPDATED_AT, DSL.currentOffsetDateTime())
            .where(
                USER.ID.eq(id)
                    .and(USER.DELETED_AT.isNull)
            )
            .execute()
    }
}
