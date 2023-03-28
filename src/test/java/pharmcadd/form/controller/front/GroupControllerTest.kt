package pharmcadd.form.controller.front

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.junit.jupiter.api.Test
import pharmcadd.form.common.extension.times
import pharmcadd.form.jooq.tables.pojos.Group
import java.time.OffsetDateTime

internal class GroupControllerTest {

    data class Tree(
        val id: Long,
        @JsonIgnore
        var parentId: Long? = null,
        var name: String,
        var displayName: String = name,
        val children: MutableList<Tree> = mutableListOf()
    )

    private fun recursive(trees: List<Tree>, level: Int, isLasts: List<Boolean> = emptyList()) {
        if (trees.isEmpty()) return
        val indent = " "
        val last = trees.last()
        for (tree in trees) {
            val isLast = tree == last
            val prefix = isLasts.mapIndexed { index, b ->
                (indent * index) + (if (b) "" else "│")
            }.joinToString("")
            tree.displayName = prefix + (indent * level) + (if (isLast) "└" else "├") + tree.name
            recursive(tree.children, level + 1, isLasts + isLast)
        }
    }

    fun flatten(root: Tree): List<Tree> =
        buildList { this += listOf(root) + root.children.flatMap { flatten(it) } }

    @Test
    fun treeTest() {
        val groups = listOf(
            Group(1, "팜캐드", null, 1, arrayOf(), OffsetDateTime.now(), OffsetDateTime.now(), null),
            Group(4, "leaf 2-4", 2, 3, arrayOf(), OffsetDateTime.now(), OffsetDateTime.now(), null),
            Group(5, "leaf 2-4-5", 4, 4, arrayOf(), OffsetDateTime.now(), OffsetDateTime.now(), null),
            Group(9, "leaf 2-4-9", 4, 4, arrayOf(), OffsetDateTime.now(), OffsetDateTime.now(), null),
            Group(10, "leaf 2-4-9-10", 9, 5, arrayOf(), OffsetDateTime.now(), OffsetDateTime.now(), null),
            Group(7, "leaf 2-4-7", 4, 4, arrayOf(), OffsetDateTime.now(), OffsetDateTime.now(), null),
            Group(3, "leaf 3", 1, 2, arrayOf(), OffsetDateTime.now(), OffsetDateTime.now(), null),
            Group(6, "leaf 3-6", 3, 3, arrayOf(), OffsetDateTime.now(), OffsetDateTime.now(), null),
            Group(2, "leaf 2", 1, 2, arrayOf(), OffsetDateTime.now(), OffsetDateTime.now(), null),
            Group(8, "leaf 8", 1, 2, arrayOf(), OffsetDateTime.now(), OffsetDateTime.now(), null),
        )

        val treeMap = groups.associate {
            it.id to Tree(it.id, it.parentId ?: -1, it.name)
        }.toMutableMap()

        for (group in groups) {
            val children = treeMap.values.filter { it.parentId == group.id }
            if (children.isNotEmpty()) {
                treeMap[group.id]!!.children.addAll(children)
            }
        }

        val root = Tree(-1, null, "root", "root", treeMap.values.filter { it.parentId == -1L }.toMutableList())
        recursive(root.children, 1, listOf(true))
        for (tree in flatten(root)) {
            println(tree.displayName)
        }
//        root
//        └팜캐드
//           ├leaf 3
//           │    └leaf 3-6
//           ├leaf 2
//           │    └leaf 2-4
//           │         ├leaf 2-4-5
//           │         └leaf 2-4-7
//           └leaf 8

        val mapper = jacksonObjectMapper()
        val json = mapper.writeValueAsString(root)
        println(json)
    }
}
