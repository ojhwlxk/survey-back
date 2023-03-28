package pharmcadd.form

import org.jooq.DSLContext
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.CommandLineRunner
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.r2dbc.R2dbcAutoConfiguration
import org.springframework.boot.runApplication
import org.springframework.cache.annotation.EnableCaching
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.security.crypto.password.PasswordEncoder
import pharmcadd.form.jooq.Tables.*
import pharmcadd.form.jooq.enums.QuestionType
import pharmcadd.form.jooq.enums.UserRole
import pharmcadd.form.model.FormVo
import pharmcadd.form.service.*
import java.time.Duration

@SpringBootApplication(exclude = [R2dbcAutoConfiguration::class])
@EnableScheduling
@EnableCaching
class FormApplication : CommandLineRunner {

    @Autowired
    lateinit var formService: FormService

    @Autowired
    lateinit var formScheduleService: FormScheduleService

    @Autowired
    lateinit var formScheduleParticipantService: FormScheduleParticipantService

    @Autowired
    lateinit var dsl: DSLContext

    @Autowired
    lateinit var timeZoneService: TimeZoneService

    @Autowired
    lateinit var positionService: PositionService

    @Autowired
    lateinit var groupService: GroupService

    @Autowired
    lateinit var userService: UserService

    @Autowired
    lateinit var passwordEncoder: PasswordEncoder

    override fun run(vararg args: String?) {
        if (dsl.selectCount().from(FORM).fetchOne()!!.value1() > 0) return

        val indiaTimeZoneId = timeZoneService.findAll().first { it.zoneId == "Asia/Kolkata" }.id

        positionService.add(
            listOf(
                " 대표이사",
                " 부사장",
                " 전무이사",
                " 상무이사",
                " 이사",
                " 부장",
                " 차장",
                " 과장",
                " 대리",
                " 주임",
                " 사원",
                " 연구소장",
                " 수석연구원",
                " 책임연구원",
                " 선임연구원",
                " 연구원",
                " 기타",
            ).map { it.trim() }
        )
        val positions = positionService.findAll()
        val findPosition: (String) -> Long = { name -> positions.first { it.name == name }.id }

        val defaultPassword = "pharmcadd"
        val insertUser: (name: String, emailId: String, timeZoneId: Long, groupId: Long, position: String?) -> Long =
            { name: String, emailId: String, timeZoneId: Long, groupId: Long, position: String? ->
                userService.add(
                    name,
                    emailId,
                    defaultPassword,
                    "$emailId@pharmcadd.com",
                    UserRole.USER,
                    timeZoneId,
                    true
                ).also {
                    userService.addGroup(it, groupId, position?.trim()?.let(findPosition))
                }
            }

        val indiaGroupId: Long
        val itDevGroupId: Long
        // 인도 법인에 포함되었지만 국내에 있는 3명
        val moonUserId: Long
        val kim1000UserId: Long
        val leeUserId: Long
        val kunduUserId: Long
        // https://hr.office.hiworks.com/pharmcadd.com/insa/info/member/hr_lists
        groupService.add("팜캐드").also { rootId ->
            insertUser("우상욱", "s.wu", 1, rootId, "대표이사")
            insertUser("권태형", "eunice", 1, rootId, "대표이사")

            groupService.add("R&D Center", rootId).also { sub1 ->
                insertUser("윤일상", "isyoon", 1, sub1, "부사장")

                groupService.add("AI", sub1).also { sub2 ->
                    insertUser("이성민", "jrpeter", 1, sub2, "상무이사")
                    insertUser("Suneel Kumar", "suneel", 1, sub2, "이사")
                    insertUser("Jayaraman", "jaainasa", 1, sub2, "수석연구원")
                    insertUser("Mandar Kulkarni", "mkulkarni", 1, sub2, "책임연구원")
                    insertUser("한성국", "sghan", 1, sub2, "책임연구원")
                    insertUser("이일구", "ilgu.yi", 1, sub2, "책임연구원")
                    insertUser("조수민", "chosm", 1, sub2, "책임연구원")
                    insertUser("정자민", "jaminjeong", 1, sub2, "책임연구원")
                    insertUser("최재한", "powerwin11", 1, sub2, "책임연구원")
                    insertUser("류성관", "ryusg716", 1, sub2, "선임연구원")
                    insertUser("조병철", "jobc", 1, sub2, "선임연구원")
                    insertUser("전민준", "jmj", 1, sub2, "선임연구원")
                    insertUser("홍예찬", "ychuh", 1, sub2, "선임연구원")
                    insertUser("Paulo Cesar Telles d", "paulocts", 1, sub2, "수석연구원")
                    insertUser("Mohammad Alwarawrah", "malwarawrah", 1, sub2, "수석연구원")
                    insertUser("Martina Pannuzzo", "martina", 1, sub2, "수석연구원")
                }
                groupService.add("Drug Design", sub1).also { sub2 ->
                    insertUser("장성훈", "jwjang", 1, sub2, "이사")
                    insertUser("abdennour braka", "a.braka", 1, sub2, "이사")
                    insertUser("sathish kumar mudedl", "mudedla", 1, sub2, "이사")
                    insertUser("Shivakumar", "sivakumar", 1, sub2, "수석연구원")
                    insertUser("Mukherjee", "goutam.mukherjee", 1, sub2, "수석연구원")
                    insertUser("Muhammad", "muhammadjan", 1, sub2, "책임연구원")
                    insertUser("윤현정", "hjyoon", 1, sub2, "책임연구원")
                    insertUser("이현수", "hsulee", 1, sub2, "선임연구원")
                }
                itDevGroupId = groupService.add("개발", sub1).also { sub2 ->
                    insertUser("김천호", "k1005", 1, sub2, "부장")
                    insertUser("이용홍", "hong", 1, sub2, "부장")
                    insertUser("오지혜", "ojhwlxk", 1, sub2, "과장")
                    insertUser("이진아", "jalee", 1, sub2, "과장")
                    insertUser("이청수", "leecheongsu", 1, sub2, "주임")
                }
                groupService.add("Bio Assay Lab", sub1).also { sub2 ->
                    insertUser("신재영", "jyshin", 1, sub2, "수석연구원")
                    insertUser("이재혜", "jaehye.lee", 1, sub2, "책임연구원")
                    insertUser("신유정", "yjshin", 1, sub2, "선임연구원")
                }
            }
            groupService.add("경영지원부문", rootId).also { sub1 ->
                insertUser("김준구", "jkkim", 1, sub1, "부사장")

                groupService.add("사업개발본부", sub1).also { sub2 ->
                    insertUser("김선장", "sjkim", 1, sub2, "전무이사")
                    insertUser("박재형", "jh.park", 1, sub2, "전무이사")

                    groupService.add("Biz.Development", sub2).also { sub3 ->
                        insertUser("황진하", "jhwang", 1, sub3, "상무이사")
                        insertUser("임채홍", "chhlim", 1, sub3, "차장")
                        insertUser("이지선", "jisun", 1, sub3, "대리")
                        insertUser("김예지", "yeji.kim", 1, sub3, "대리")
                    }
                    groupService.add("Biz.Strategy", sub2).also { sub3 ->
                        insertUser("김현중", "harrykim", 1, sub3, "전무이사")
                        insertUser("이종윤", "jylee", 1, sub3, "상무이사")
                        insertUser("유승국", "skyoo", 1, sub3, "차장")
                        insertUser("최운창", "unchangc", 1, sub3, "과장")
                    }
                }
                groupService.add("MS 본부", sub1).also { sub2 ->
                    insertUser("김종완", "jwkim", 1, sub2, "전무이사")

                    groupService.add("인사/총무팀", sub2).also { sub3 ->
                        insertUser("오현종", "spec12", 1, sub3, "이사")
                        insertUser("이기열", "sasusung", 1, sub3, "과장")
                        leeUserId = insertUser("이희승", "victor", 1, sub3, "과장")
                    }
                    groupService.add("R&D지원팀", sub2).also { sub3 ->
                        insertUser("김홍균", "redviruskim", 1, sub3, "부장")
                        insertUser("이소정", "yepi97", 1, sub3, "주임")
                        insertUser("진현희", "jin", 1, sub3, "주임")
                    }
                    groupService.add("홍보/IR팀", sub2).also { sub3 ->
                        insertUser("권현정", "stellar", 1, sub3, "이사")
                        insertUser("기태훈", "taehoonkee", 1, sub3, "주임")
                    }
                    groupService.add("지원팀", sub2).also { sub3 ->
                        insertUser("이종희", "cotmyjh", 1, sub3, "과장")
                        insertUser("최의혁", "ceh4803", 1, sub3, "대리")
                    }
                }
                groupService.add("기획재정실", sub1).also { sub2 ->
                    moonUserId = insertUser("문정완", "moon", 1, sub2, "상무이사")

                    groupService.add("재무/회계팀", sub2).also { sub3 ->
                        kim1000UserId = insertUser("김천웅", "bantura", 1, sub3, "이사")
                        insertUser("임경희", "khlim", 1, sub3, "과장")
                    }
                    groupService.add("기획팀", sub2).also { sub3 ->
                        insertUser("정서영", "syjung", 1, sub3, "대리")
                        insertUser("권선영", "tjsdud9183", 1, sub3, "사원")
                        insertUser("박지현", "clarepark", 1, sub3, "사원")
                    }
                    groupService.add("IDC", sub2).also { sub3 ->
                        groupService.add("데이터설계팀", sub3).also { sub4 ->
                            insertUser("임대원", "moses", 1, sub4, "부장")
                            insertUser("정현호", "jungh20503", 1, sub4, "대리")
                            insertUser("송리하", "lhsong", 1, sub4, "대리")
                            insertUser("정수빈", "subinjeong", 1, sub4, "주임")
                        }
                        groupService.add("정보보안팀", sub3).also { sub4 ->
                            insertUser("금봉권", "keum1976", 1, sub4, "부장")
                        }
                        groupService.add("서버관리팀", sub3).also { sub4 ->
                            insertUser("안성민", "jerry.ahn", 1, sub4, "부장")
                            insertUser("강현호", "heno7609", 1, sub4, "주임")
                            insertUser("황승준", "mark", 1, sub4, null)
                        }
                    }
                }
            }
            indiaGroupId = groupService.add("인도법인", rootId).also { sub1 ->
                userService.addGroup(moonUserId, sub1, findPosition("상무이사"))
                userService.addGroup(kim1000UserId, sub1, findPosition("이사"))
                userService.addGroup(leeUserId, sub1, findPosition("과장"))
                insertUser("Gopinath Krishnasamy", "gopi", indiaTimeZoneId, sub1, "수석연구원")
                insertUser("Gaurao Dhoke", "gauraodhoke", indiaTimeZoneId, sub1, "수석연구원")

                insertUser("pratiti bhadra", "pratiti.bhadra", indiaTimeZoneId, sub1, "수석연구원")
                insertUser("Shubhadip Das", "shubhadip", indiaTimeZoneId, sub1, "책임연구원")
                insertUser("Boyli Ghosh", "boyli", indiaTimeZoneId, sub1, "책임연구원")
                insertUser("Ramesh Muthusamy", "ramesh", indiaTimeZoneId, sub1, "수석연구원")
                insertUser("Munikumar R Doddared", "kumardmk", indiaTimeZoneId, sub1, "수석연구원")

                insertUser("Sanam Swetha Yadav", "swetha", indiaTimeZoneId, sub1, "수석연구원")
                insertUser("Indrajit Deb", "ideb", indiaTimeZoneId, sub1, "수석연구원")
                insertUser("Gundabathula Sri Kal", "rochish.g", indiaTimeZoneId, sub1, "책임연구원")
                insertUser("Leela Sarath Kumar K", "klsarathk", indiaTimeZoneId, sub1, "책임연구원")
                insertUser("Pradeep Kumar Yadav", "pradeep", indiaTimeZoneId, sub1, "책임연구원")

                kunduUserId = insertUser("Sibsankar Kundu", "sskundu", indiaTimeZoneId, sub1, "이사")
                insertUser("Janardhan Sridhara", "sjanardhan", indiaTimeZoneId, sub1, "이사")
            }
        }
        userService.changeRole(leeUserId, UserRole.CAMPAIGN_ADMIN)
//        userService.changeRole(kunduUserId, UserRole.CAMPAIGN_ADMIN)

        run {
            val formId = formService.save(
                FormVo(
                    title = "PharmCADD India Disease Control and Prevention",
                    description = "Frequently asked questions about negative COVID-19\nTest Requirement for office attendance",
                    questions = listOf(
                        FormVo.QuestionVo(
                            title = "Are you suffering from any of the following symptoms?",
                            type = QuestionType.CHOICE_SINGLE,
                            required = true,
                            options = listOf(
                                FormVo.QuestionVo.OptionVo(text = "Fever"),
                                FormVo.QuestionVo.OptionVo(text = "Cough"),
                                FormVo.QuestionVo.OptionVo(text = "Respiratory distress"),
                                FormVo.QuestionVo.OptionVo(text = "Shivers"),
                                FormVo.QuestionVo.OptionVo(text = "X"),
                            )
                        ),
                        FormVo.QuestionVo(
                            title = "Do any of your family member or roommate have symptoms mentioned above?",
                            type = QuestionType.CHOICE_SINGLE,
                            required = true,
                            options = listOf(
                                FormVo.QuestionVo.OptionVo(text = "Fever"),
                                FormVo.QuestionVo.OptionVo(text = "Cough"),
                                FormVo.QuestionVo.OptionVo(text = "Respiratory distress"),
                                FormVo.QuestionVo.OptionVo(text = "Shivers"),
                                FormVo.QuestionVo.OptionVo(text = "X"),
                            )
                        ),
                        FormVo.QuestionVo(
                            title = "Have you visited any place, where patients with confirmed COVID-19 infection have reportedly been?",
                            type = QuestionType.TEXT_SHORT
                        ),
                        FormVo.QuestionVo(
                            title = "Have you visited any place, Any public facilities, crowded places or events?",
                            type = QuestionType.TEXT_SHORT
                        ),
                        FormVo.QuestionVo(
                            title = "Did you receive any communication/Guide from Government Health Officials? If yes, please provide details to hr@pharmcadd.com",
                            type = QuestionType.TEXT_LONG
                        ),
                    )
                ),
                1L
            )

            // 인도법인 코로나검진 캠페인 - 매일 9시 5분(인도시간) 이희승과장, 쿤두박사 결과 메일링
            val scheduleId = formScheduleService.addCron(
                formId,
                indiaTimeZoneId,
                "0 0 6 ? * MON-SAT", // 6시부터 시작해서
                Duration.ofMinutes(((3 * 60) + 10).toLong()).toMillis(), // 9시 5분에 만료
                active = true,
                mailing = false
            )

            dsl
                .select(
                    USER.ID
                ).from(USER)
                .join(GROUP_USER).on(GROUP_USER.USER_ID.eq(USER.ID))
                .join(GROUP).on(GROUP_USER.GROUP_ID.eq(GROUP.ID))
                .where(
                    GROUP.ID.eq(indiaGroupId)
                )
                .fetch { it.value1() }
                .also { userIds ->
                    val removedKorean = userIds.toMutableList().apply {
                        removeAll(listOf(leeUserId, moonUserId, kim1000UserId))
                    }
                    formScheduleParticipantService.addUsers(scheduleId, formId, removedKorean)
                }

            formService.addNotification(formId, leeUserId)
            formService.addNotification(formId, kunduUserId)
        }
    }
}

fun main(args: Array<String>) {
    runApplication<FormApplication>(*args)
}
