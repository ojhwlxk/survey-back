package quartz

import org.junit.jupiter.api.Test
import org.quartz.*
import org.quartz.impl.StdSchedulerFactory
import org.quartz.impl.matchers.GroupMatcher
import java.util.*

class HelloJob : Job {
    override fun execute(context: JobExecutionContext?) {
        println("HelloJob")
    }
}

class CronTriggerTest {

    fun getNextFiveRuns(trigger: Trigger): List<Date> {
        val runs: MutableList<Date> = ArrayList()
        var next: Date = trigger.nextFireTime

        // check for null, which indicates a non-repeating trigger or one with an end-time
        while (runs.size < 5) {
            runs.add(next)
            next = trigger.getFireTimeAfter(next)
        }
        return runs
    }

    @Test
    fun test() {
        val stdSchedulerFactory = StdSchedulerFactory()
        val scheduler = stdSchedulerFactory.scheduler

        val jobDetail = JobBuilder.newJob(HelloJob::class.java)
//            .storeDurably()
            .build()

        val triggerKey = TriggerKey.triggerKey(UUID.randomUUID().toString())
        val trigger = TriggerBuilder.newTrigger()
            .withSchedule(
                CronScheduleBuilder
                    .cronSchedule("0 * * * * ?")
                    .inTimeZone(TimeZone.getTimeZone("Asia/Kolkata")/*.getDefault()*/)
            )
            .withIdentity(triggerKey)
            .build()

        scheduler.scheduleJob(jobDetail, trigger)

        val nextFiveRuns = getNextFiveRuns(trigger)
        nextFiveRuns.forEach {
            println(it.toInstant().atZone(TimeZone.getTimeZone("Asia/Kolkata").toZoneId()))
//            println(it)
        }

        scheduler.unscheduleJob(triggerKey)

        val jobKeys = scheduler.getJobKeys(GroupMatcher.anyGroup())
        println(jobKeys)

        scheduler.shutdown()
    }
}
