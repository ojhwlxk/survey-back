import org.jooq.DSLContext
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import java.io.File
import java.util.*

class Pojo(val foo: String, val bar: String) {
//    override fun equals(other: Any?): Boolean {
//        if (this === other) return true
//        if (javaClass != other?.javaClass) return false
//
//        other as Pojo
//
//        if (foo != other.foo) return false
//        if (bar != other.bar) return false
//
//        return true
//    }
//
//    override fun hashCode(): Int {
//        var result = foo.hashCode()
//        result = 31 * result + bar.hashCode()
//        return result
//    }
}
data class DataPojo(val foo: String, val bar: String)

@SpringBootTest(classes = [DSLContext::class])
class DataClassTest {

    @Autowired
    lateinit var dsl: DSLContext

    @Test
    fun test() {
        val pojo1 = Pojo("1", "2")
        val pojo2 = Pojo("2", "3")
        if (pojo1 == pojo2) {
            println("같다")
        } else {
            println("다르다")
        }
    }

    @Test
    fun test2() {
        val pojo1 = DataPojo("1", "2")
        val pojo2 = pojo1.copy(bar = "3")
        if (pojo1 == pojo2) {
            println("같다")
        } else {
            println("다르다")
        }
    }


    class SchemaForm(
        val schemaName: String,
        val isDefault: Boolean
    )

    @Test
    fun test3() {
        val query = "select * from information_schema.tables " +
            "where table_schema = 'public' order by table_name;"
        dsl.fetch(query)
            .map { it ->
                SchemaForm(
                    schemaName = it.getValue("SCHEMA_NAME", String::class.java),
                    isDefault = it.getValue("IS_DEFALUT", Boolean::class.java)
                )
            }
            .forEach{
                println(it)
            }
    }

    @Test

}
