package iterator

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class IteratorTest {

    @Test
    fun test() {
        val ids = listOf(1, 2, 3, 5, 6).listIterator()
        Assertions.assertEquals(1, ids.next())
        Assertions.assertEquals(2, ids.next())
        Assertions.assertEquals(3, ids.next())
        Assertions.assertEquals(5, ids.next())
        Assertions.assertEquals(6, ids.next())
    }
}
