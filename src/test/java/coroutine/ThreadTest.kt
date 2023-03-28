package coroutine

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import java.util.concurrent.atomic.AtomicLong
import kotlin.concurrent.thread

class ThreadTest {

    @Test
    fun threadTest() {
        val c = AtomicLong()
        for (i in 1..1_000_000L) {
            thread(start = true) {
                c.addAndGet(i)
            }
        }
        println(c.get())
    }

    @Test
    fun coroutineTest() {
        val c = AtomicLong()
        runBlocking {
            for (i in 1..1_000_000L) {
                GlobalScope.launch {
                    c.addAndGet(i)
                }
            }
        }
        println(c.get())
    }
}
