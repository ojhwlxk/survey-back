package monad

import arrow.core.*
import arrow.core.computations.either
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import kotlin.random.Random

data class JoinMemberForm(
    val username: String,
    val password: String,
    val confirmPassword: String
)

class MonadTest {

    fun validCheckUserId(userId: String): Either<Throwable, Boolean> {
        return Either.catch {
            if (Random.nextBoolean()) {
                throw RuntimeException("에러")
            } else {
                true
            }
        }
    }

    fun validCheckUserIdLengthCheck(userId: String): Either<Throwable, Boolean> {
        return if (userId.length > 4 && userId.length < 16) {
            true.right()
        } else {
            RuntimeException("패스워드 불 일치").left()
        }
    }

    fun validConfirmPasswordCheck(password: String, confirmPassword: String): Either<Throwable, Boolean> {
        return if (password == confirmPassword) {
            true.right()
        } else {
            RuntimeException("패스워드 불 일치").left()
        }
    }

    fun join(form: JoinMemberForm): Either<Throwable, Boolean> {
        return runBlocking {
            either {
                validCheckUserId(form.username).bind()
                validCheckUserIdLengthCheck(form.username).bind()
                validConfirmPasswordCheck(form.password, form.confirmPassword).bind()
            }
        }
//        return validCheckUserId(form.username)
//            .flatMap {
//                validCheckUserIdLengthCheck(it)
//            }
//            .flatMap {
//                validConfirmPasswordCheck(form.password, form.confirmPassword)
//            }
    }

    @Test
    fun joinTest() {
        join(JoinMemberForm("hong", "123", "456"))
            .fold({
                println("에러")
            }, {
                println("성공")
            })
    }

    fun div(a: Int, b: Int): Either<Throwable, Int> {
        return Either.catch { a / b }
//
//        return try {
//            (a/b).right()
//        } catch (e : Exception) {
//            e.left()
//        }
    }

    @Test
    fun divTest() {
        val map = div(10, 0)
            .flatMap { c(it) }
    }

    fun c(s: Int): Either<Throwable, String> {
        return Either.catch { s.toString() }
    }

    fun a(s: String): Either<Throwable, String> {
        return Either.catch { s }
    }

    fun b(s: String): Either<Throwable, Int> {
        return Either.catch { s.toInt() }
    }

    @Test
    fun basicTest() {
        val either = a("1")
            .flatMap { b(it) }

        when (either) {
            is Either.Left -> {
                println("에러 : ${either.value}")
            }
            is Either.Right -> {
                println("정상결과 : ${either.value}")
            }
        }
    }

    @Test
    fun foldTest() {
        a("1")
            .flatMap { b(it) }
            .fold(
                { throwable -> println("에러 : $throwable") },
                { i -> println("정상 결과 : $i") }
            )
    }

    @Test
    fun bindTest() {
        runBlocking {
            val either = either<Throwable, Int> {
                val s = a("1").bind()
                val b = b(s).bind()
                b
            }
            when (either) {
                is Either.Left -> {
                    println("에러 : ${either.value}")
                }
                is Either.Right -> {
                    println("정상결과 : ${either.value}")
                }
            }
        }
    }

    @Test
    fun recoverTest() {
        val either = Either
            .catch {
                if (Random.nextBoolean()) {
                    error("에러1")
                } else {
                    "1"
                }
            }
            .handleErrorWith {
                Either.catch {
                    if (Random.nextBoolean()) {
                        error("에러2")
                    } else {
                        "2"
                    }
                }
            }
            .handleError { "3" }

        when (either) {
            is Either.Left -> {
                println("에러 : ${either.value}")
            }
            is Either.Right -> {
                println("정상결과 : ${either.value}")
            }
        }
    }
}
