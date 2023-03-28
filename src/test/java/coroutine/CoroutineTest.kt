package coroutine

import kotlinx.coroutines.*
import org.junit.jupiter.api.Test
import org.springframework.web.client.RestTemplate
import org.springframework.web.client.getForObject
import kotlin.time.Duration.Companion.seconds

class CoroutineTest {

    @Test
    fun blockingTest() {
        runBlocking {
            a()
            b()
            c()
        }
    }

    @Test
    fun nonBlockingTest() {
        runBlocking {
            launch {
                a()
            }
            launch {
                b()
            }
            launch {
                c()
            }
        }
    }

    suspend fun a() {
        delay(1.seconds)
        println("aa")
    }

    suspend fun b() {
        delay(1.seconds)
        println("bb")
    }

    suspend fun c() {
        delay(1.seconds)
        println("cc")
    }

    @Test
    fun blockingApiTest() {
        val restTemplate = RestTemplate()
        listOf("allbegray", "ojhwlxk", "leecheongsu")
            .map { user ->
                val s = restTemplate.getForObject<String>("https://api.github.com/users/$user")
                Thread.sleep((1 * 1000).toLong())
                s
            }
            .forEach { println(it) }
    }

    @Test
    fun asyncApiTest() {
        val restTemplate = RestTemplate()
        runBlocking {
            listOf("allbegray", "ojhwlxk", "leecheongsu")
                .map { user ->
                    async {
                        val s = restTemplate.getForObject<String>("https://api.github.com/users/$user")
                        delay(1.seconds)
                        s
                    }
                }
                .awaitAll()
                .forEach { println(it) }
        }
    }
}
