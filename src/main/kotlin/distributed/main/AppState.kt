package distributed.main

import distributed.dto.ItemGroupMetadata
import java.io.File
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.concurrent.atomic.AtomicInteger
import khttp.post as httpPost

object AppState {
    var nodeName = ""
    var lastId: AtomicInteger = AtomicInteger(0)
    var itemGroups = mutableMapOf<Int, ItemGroupMetadata>()
    var port = 7000
    var masterAddress: String = ""

    fun load() {
        loadNodeConfig()

        loadExternalConfig()

        Runtime.getRuntime().addShutdownHook(Thread {
            run {
                val collectionsFile = File("collections")
                val itemGroupsText = itemGroups.map { "${it.value.id} ${it.value.name} ${it.value.itemCount}\n" }
                    .reduceRight { s, acc -> acc + s }
                collectionsFile.writeText(itemGroupsText)
            }
        })
    }

    private fun loadExternalConfig() {
        val externalConfigFileContents =
            this.javaClass.classLoader.getResourceAsStream("config.properties").bufferedReader().use { it.readText() }

        if (externalConfigFileContents.isNotBlank()) {
            val lines = externalConfigFileContents.split("\n")
            port = lines[0].split("=")[1].toInt()
            masterAddress = lines[1].split("=")[1]
            announceToMaster(nodeName)
        }
    }

    private fun loadNodeConfig() {
        val nodeConfigFile = File("nodeConfig")
        if (nodeConfigFile.exists()) {
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

        if (itemGroupsFile.exists()) {
            itemGroupsFile.useLines { lines ->
                lines.forEach { line ->
                    val lineElements = line.split(" ")
                    itemGroups.put(
                        lineElements[0].toInt(),
                        ItemGroupMetadata(lineElements[0].toInt(), lineElements[1], lineElements[2].toInt())
                    )
                }
            }
        } else {
            itemGroupsFile.createNewFile()
        }
    }

    fun addItemGroup(name: String) {
        val id = lastId.incrementAndGet()
        itemGroups.put(id, ItemGroupMetadata(name = name, id = id, itemCount = 1))
    }

    fun addItemGroup(name: String, size: Int) {
        val id = lastId.incrementAndGet()
        itemGroups.put(id, ItemGroupMetadata(name = name, id = id, itemCount = size))
    }

    private fun announceToMaster(nodeName: String) {
        try {
            khttp.post(masterAddress, json = mapOf("name" to nodeName))
        } catch (e: Exception) {
            print(e.message)
        }
    }
}