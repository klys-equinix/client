package distributed.main

import distributed.dto.ItemGroupMetadata
import java.io.File
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.concurrent.atomic.AtomicInteger

object AppState {
    var nodeName = ""
    var lastId: AtomicInteger = AtomicInteger(0)
    var itemGroups = mutableMapOf<Int, ItemGroupMetadata>()

    fun load() {
        val nodeConfigFile = File("nodeConfig")
        if(nodeConfigFile.exists()) {
            nodeConfigFile.useLines { lines ->
                val lineList = lines.toList()
                nodeName = lineList[0]
                lastId.set(lineList[1].toInt())
            }
        } else {
            nodeName = "Node-" + LocalDateTime.now().toInstant(ZoneOffset.UTC).epochSecond
            nodeConfigFile.createNewFile()
            nodeConfigFile.writeText("$nodeName\n0")
        }

        val itemGroupsFile = File("collections")

        if(itemGroupsFile.exists()) {
            itemGroupsFile.useLines { lines ->
                lines.forEach { line ->
                    val lineElements = line.split(" ")
                    itemGroups.put(lineElements[0].toInt(), ItemGroupMetadata(lineElements[0].toInt(), lineElements[1], lineElements[2].toInt()))
                }
            }
        } else {
            itemGroupsFile.createNewFile()
        }

        Runtime.getRuntime().addShutdownHook(Thread {
            run {
                val collectionsFile = File("collections")
                val itemGroupsText = itemGroups.map { "${it.value.id} ${it.value.name} ${it.value.itemCount}\n" }.reduceRight { s, acc ->  acc + s }
                collectionsFile.writeText(itemGroupsText)
            }
        })
    }

    fun addItemGroup(name: String) {
        val id = lastId.incrementAndGet()
        itemGroups.put(id, ItemGroupMetadata(name = name, id = id, itemCount = 1))
        val itemGroupsFile = File("collections")
        itemGroupsFile.appendText("$id $name 1\n")
    }
}