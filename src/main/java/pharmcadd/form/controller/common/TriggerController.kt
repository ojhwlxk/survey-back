package pharmcadd.form.controller.common

import org.quartz.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import pharmcadd.form.common.controller.BaseController
import pharmcadd.form.common.extension.timeZone
import pharmcadd.form.common.extension.zoneId
import pharmcadd.form.controller.common.form.TriggerForm
import pharmcadd.form.service.TimeZoneService
import pharmcadd.form.service.UserService
import java.time.OffsetDateTime
import java.time.ZoneId
import java.util.*
import javax.annotation.PostConstruct

@RestController
@RequestMapping("/triggers")
class TriggerController : BaseController() {

    class EmptyJob : Job {
        override fun execute(context: JobExecutionContext?) {
        }
    }

    @Autowired
    lateinit var scheduler: Scheduler

    @Autowired
    lateinit var timeZoneService: TimeZoneService

    @Autowired
    lateinit var userService: UserService

    private val emptyJobKey = JobKey.jobKey(EmptyJob::class.simpleName)

    @PostConstruct
    fun init() {
        val emptyJob = JobBuilder.newJob(EmptyJob::class.java)
            .storeDurably()
            .withIdentity(emptyJobKey)
            .build()
        scheduler.addJob(emptyJob, true)
    }

    @PostMapping("/next-fire-times")
    fun nextFireTimes(@RequestBody form: TriggerForm): List<OffsetDateTime> {
        val timeZoneId = form.timeZoneId ?: userService.findOne(securityService.userId)?.timeZoneId
        val timeZone = timeZoneId?.let { timeZoneService.findOne(it) }

        val triggerKey = TriggerKey.triggerKey(UUID.randomUUID().toString())
        return try {
            val trigger = TriggerBuilder.newTrigger()
                .withSchedule(
                    CronScheduleBuilder
                        .cronSchedule(form.cronExpression)
                        .inTimeZone(timeZone?.timeZone() ?: TimeZone.getDefault())
                )
                .withIdentity(triggerKey)
                .forJob(EmptyJob::class.simpleName)
                .build()

            scheduler.scheduleJob(trigger)

            val dates = mutableListOf<Date>()
            var nextFireTime: Date = trigger.nextFireTime
            while (dates.size < form.repeat) {
                dates.add(nextFireTime)
                nextFireTime = trigger.getFireTimeAfter(nextFireTime)
            }

            dates.map {
                it.toInstant().atZone(timeZone?.zoneId() ?: ZoneId.systemDefault()).toOffsetDateTime()
            }
        } finally {
            scheduler.unscheduleJob(triggerKey)
        }
    }
}
