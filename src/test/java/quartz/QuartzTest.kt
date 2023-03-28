package quartz

import org.junit.jupiter.api.Test
import org.quartz.CronScheduleBuilder.cronSchedule
import org.quartz.JobBuilder
import org.quartz.JobExecutionContext
import org.quartz.JobKey
import org.quartz.TriggerBuilder
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.scheduling.quartz.QuartzJobBean
import org.springframework.scheduling.quartz.SchedulerFactoryBean
import java.util.*

class TestJob : QuartzJobBean() {

    override fun executeInternal(context: JobExecutionContext) {
        println("execution")
    }
}

@SpringBootTest(classes = [SchedulerFactoryBean::class, TestJob::class])
internal class QuartzTest {

    @Autowired
    lateinit var schedulerFactoryBean: SchedulerFactoryBean

    @Test
    fun main() {

        val newJob = JobBuilder.newJob(TestJob::class.java)
            .withIdentity(JobKey.jobKey("TestJob"))
            .storeDurably()
            .build()

        val cronTrigger = TriggerBuilder.newTrigger().forJob(newJob)
            .withSchedule(cronSchedule("0 * * * * ?"))
            .build()

        schedulerFactoryBean.scheduler.scheduleJob(newJob, cronTrigger)
    }
}
