package pharmcadd.form.service

import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.CachePut
import org.springframework.cache.annotation.Cacheable
import org.springframework.cache.annotation.Caching
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import pharmcadd.form.jooq.Tables.TIME_ZONE
import pharmcadd.form.jooq.Tables.USER
import pharmcadd.form.jooq.tables.pojos.TimeZone
import java.time.OffsetDateTime

@Service
@Transactional
class TimeZoneService {

    companion object {
        const val TIME_ZONE_ALL_CACHE_NAME = "TimeZoneAll"
        const val TIME_ZONE_CACHE_NAME = "TimeZone"
    }

    @Autowired
    lateinit var dsl: DSLContext

    @Transactional(readOnly = true)
    fun currentOffsetDateTime(): OffsetDateTime {
        return dsl
            .select(DSL.currentOffsetDateTime())
            .fetchOne()!!
            .value1()!!
    }

    @Transactional(readOnly = true)
    fun findByUserId(userId: Long): TimeZone? {
        return dsl
            .select(
                *TIME_ZONE.fields()
            )
            .from(USER)
            .join(TIME_ZONE).on(TIME_ZONE.ID.eq(USER.TIME_ZONE_ID).and(TIME_ZONE.DELETED_AT.isNull))
            .where(
                USER.ID.eq(userId)
                    .and(USER.DELETED_AT.isNull)
            )
            .fetchOne { it.into(TimeZone::class.java) }
    }

    @Caching(
        evict = [CacheEvict(TIME_ZONE_ALL_CACHE_NAME, allEntries = true)],
    )
    fun add(country: String, zoneId: String, utc: String): TimeZone {
        return dsl.insertInto(TIME_ZONE)
            .set(TIME_ZONE.COUNTRY, country)
            .set(TIME_ZONE.ZONE_ID, zoneId)
            .set(TIME_ZONE.UTC, utc)
            .returning()
            .fetchOne { it.into(TimeZone::class.java) }!!
    }

    @Caching(
        evict = [CacheEvict(TIME_ZONE_ALL_CACHE_NAME, allEntries = true)],
        put = [CachePut(TIME_ZONE_CACHE_NAME, key = "#id")]
    )
    fun modify(id: Long, country: String, zoneId: String, utc: String): TimeZone {
        return dsl.update(TIME_ZONE)
            .set(TIME_ZONE.COUNTRY, country)
            .set(TIME_ZONE.ZONE_ID, zoneId)
            .set(TIME_ZONE.UTC, utc)
            .set(TIME_ZONE.UPDATED_AT, DSL.currentOffsetDateTime())
            .where(
                TIME_ZONE.ID.eq(id)
                    .and(TIME_ZONE.DELETED_AT.isNull)
            )
            .returning()
            .fetchOne { it.into(TimeZone::class.java) }!!
    }

    @Caching(
        cacheable = [Cacheable(TIME_ZONE_CACHE_NAME, key = "#id", unless = "#result == null")]
    )
    @Transactional(readOnly = true)
    fun findOne(id: Long): TimeZone? {
        return dsl
            .selectFrom(TIME_ZONE)
            .where(
                TIME_ZONE.ID.eq(id)
                    .and(TIME_ZONE.DELETED_AT.isNull)
            )
            .fetchOne { it.into(TimeZone::class.java) }
    }

    @Caching(
        cacheable = [Cacheable(TIME_ZONE_ALL_CACHE_NAME)]
    )
    @Transactional(readOnly = true)
    fun findAll(): List<TimeZone> {
        return dsl
            .selectFrom(TIME_ZONE)
            .where(
                TIME_ZONE.DELETED_AT.isNull
            )
            .fetch { it.into(TimeZone::class.java) }
    }

    @Caching(
        evict = [
            CacheEvict(TIME_ZONE_ALL_CACHE_NAME, allEntries = true),
            CacheEvict(TIME_ZONE_CACHE_NAME, key = "#id")
        ]
    )
    fun deleteById(id: Long) {
        dsl.update(TIME_ZONE)
            .set(TIME_ZONE.UPDATED_AT, DSL.currentOffsetDateTime())
            .set(TIME_ZONE.DELETED_AT, DSL.currentOffsetDateTime())
            .where(
                TIME_ZONE.ID.eq(id)
                    .and(TIME_ZONE.DELETED_AT.isNull)
            )
            .execute()
    }
}
