package member

class JoinForm(
    val name: String,
    val password: String,
    val email: String,
)

// @SpringBootTest(classes = [FormApplication::class])
// class MemberTest {
//
//    @Autowired
//    lateinit var memberService: MemberService
//
//    private fun validEmail(email: String): Either<Throwable, Boolean> {
//        return if (memberService.findByEmail(email) == null) {
//            true.right()
//        } else {
//            RuntimeException("이미 존재합니다.").left()
//        }
//    }
//
//    private fun validPassword(password: String): Either<Throwable, Boolean> {
//        /* 최소 8 자, 최소 하나의 문자 및 하나의 숫자 */
//        val regex = """^(?=.*[A-Za-z])(?=.*\d)[A-Za-z\d]{8,}$""".toRegex()
//
//        return if (password.matches(regex)) {
//            true.right()
//        } else {
//            RuntimeException("비밀번호가 유효하지 않습니다.").left()
//        }
//    }
//
//    private fun join(form: member.JoinForm): Either<Throwable, Boolean> {
//        return runBlocking {
//            either {
//                validEmail(form.email).bind()
//                validPassword(form.password).bind()
//            }
//        }
//    }
//
//    @Test
//    fun validEmailTest() {
//        validEmail("ojhwlxk@pharmcadd.com")
//            .fold({
//                println("에러")
//            }, {
//                println("성공")
//            })
//    }
//
//    @Test
//    fun joinTest() {
//        join(JoinForm("오지혜", "1111", "ojhwlxk@pharmcadd.com"))
//            .fold({
//                println("에러")
//            }, {
//                println("성공")
//            })
//
//        join(JoinForm("오지혜", "qwe123", "ojhwlxk@pharmcadd.com"))
//            .fold({
//                println("에러")
//            }, {
//                println("성공")
//            })
//    }
// }
