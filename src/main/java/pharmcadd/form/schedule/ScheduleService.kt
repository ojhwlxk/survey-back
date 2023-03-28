package pharmcadd.form.schedule

import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.quartz.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.scheduling.quartz.QuartzJobBean
import org.springframework.stereotype.Component
import pharmcadd.form.common.extension.timeZone
import pharmcadd.form.common.extension.zoneId
import pharmcadd.form.jooq.Tables.*
import pharmcadd.form.jooq.enums.CampaignStatus
import pharmcadd.form.jooq.enums.FormScheduleType
import pharmcadd.form.jooq.tables.records.FormScheduleRecord
import pharmcadd.form.service.CampaignService
import pharmcadd.form.service.TimeZoneService
import java.time.ZoneId
import java.util.*
import javax.annotation.PostConstruct

class CampaignExpireJob : QuartzJobBean() {

    @Autowired
    lateinit var dsl: DSLContext

    @Autowired
    lateinit var campaignService: CampaignService

    override fun executeInternal(context: JobExecutionContext) {
        val campaigns = dsl
            .selectFrom(CAMPAIGN)
            .where(
                CAMPAIGN.STATUS.eq(CampaignStatus.RUNNING)
                    .and(CAMPAIGN.ENDS_AT.lessThan(DSL.currentOffsetDateTime()))
                    .and(CAMPAIGN.DELETED_AT.isNull)
            )
            .fetch()

        for (campaign in campaigns) {
            campaignService.finish(campaign.id, campaign.updatedBy, true)
        }
    }
}

class CampaignStartJob : QuartzJobBean() {

    @Autowired
    lateinit var dsl: DSLContext

    @Autowired
    lateinit var campaignService: CampaignService

    override fun executeInternal(context: JobExecutionContext) {
        val campaigns = dsl
            .selectFrom(CAMPAIGN)
            .where(
                CAMPAIGN.STATUS.eq(CampaignStatus.READY)
                    .and(CAMPAIGN.STARTS_AT.lessThan(DSL.currentOffsetDateTime()))
                    .and(CAMPAIGN.DELETED_AT.isNull)
            )
            .fetch()

        for (campaign in campaigns) {
            campaignService.run(campaign.id, campaign.updatedBy)
        }
    }
}

class CampaignScheduleStartJob : QuartzJobBean() {

    companion object {
        const val FORM_SCHEDULE_ID = "formScheduleId"
    }

    @Autowired
    lateinit var campaignService: CampaignService

    override fun executeInternal(context: JobExecutionContext) {
        val mergedJobDataMap = context.mergedJobDataMap
        val formScheduleId = mergedJobDataMap[FORM_SCHEDULE_ID] as Long
        campaignService.addBySchedule(formScheduleId)
    }
}

@Component
class ScheduleService {

    @Autowired
    lateinit var scheduler: Scheduler

    @Autowired
    lateinit var dsl: DSLContext

    @Autowired
    lateinit var timeZoneService: TimeZoneService

    private val campaignScheduleStartJobKey = JobKey.jobKey(CampaignScheduleStartJob::class.simpleName)

    @PostConstruct
    fun init() {
        scheduleJob<CampaignExpireJob>("0/30 * * * * ?")
        scheduleJob<CampaignStartJob>("0/30 * * * * ?")

        val campaignStartJob = JobBuilder.newJob(CampaignScheduleStartJob::class.java)
            .storeDurably()
            .withIdentity(campaignScheduleStartJobKey)
            .build()
        scheduler.addJob(campaignStartJob, true)

        // NOTE : 서버 시작 시 스케쥴 등록 처리
        dsl
            .select(
                *FORM_SCHEDULE.fields()
            )
            .from(FORM_SCHEDULE)
            .join(FORM).on(FORM.ID.eq(FORM_SCHEDULE.FORM_ID).and(FORM.DELETED_AT.isNull))
            .where(
                FORM_SCHEDULE.ACTIVE.eq(true)
                    .and(FORM_SCHEDULE.DELETED_AT.isNull)
            )
            .fetch { it.into(FORM_SCHEDULE) }
            .forEach(::scheduleJob)
    }

    private inline fun <reified T : QuartzJobBean> scheduleJob(cronExpression: String) {
        val jobDetail = JobBuilder.newJob(T::class.java)
            .withIdentity(JobKey.jobKey(T::class.simpleName))
            .build()

        val trigger = TriggerBuilder.newTrigger()
            .withIdentity(TriggerKey.triggerKey(T::class.simpleName))
            .withSchedule(CronScheduleBuilder.cronSchedule(cronExpression))
            .build()

        scheduler.scheduleJob(jobDetail, trigger)
    }

    fun scheduleJob(formSchedule: FormScheduleRecord) {
        if (formSchedule.active) {
            val id = formSchedule.id
            val timeZoneRecord = timeZoneService.findOne(formSchedule.timeZoneId)!!

            val trigger = when (formSchedule.type) {
                FormScheduleType.MANUAL -> {
                    val startsAt = formSchedule.startsAt!!.atZone(timeZoneRecord.zoneId())
                        .let {
                            // quart 가 동작하고 있는 리눅스 서버의 시간으로 변경 한다.
                            it.withZoneSameInstant(ZoneId.systemDefault())
                        }
                        .let {
                            Date.from(it.toInstant())
                        }

                    TriggerBuilder.newTrigger()
                        .startAt(startsAt)
                }
                FormScheduleType.CRON -> {
                    TriggerBuilder.newTrigger()
                        .withSchedule(
                            CronScheduleBuilder
                                .cronSchedule(formSchedule.cronExpression)
                                .inTimeZone(timeZoneRecord.timeZone())
                        )
                }
            }.let { builder ->
                builder
                    .withIdentity(TriggerKey.triggerKey("campaign_job_$id"))
                    .usingJobData(CampaignScheduleStartJob.FORM_SCHEDULE_ID, id)
                    .forJob(campaignScheduleStartJobKey)
                    .build()
            }

            scheduler.scheduleJob(trigger)
        }
    }

    fun unscheduleJob(id: Long) {
        scheduler.unscheduleJob(TriggerKey.triggerKey("campaign_job_$id"))
    }
}
