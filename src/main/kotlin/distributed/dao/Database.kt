package distributed.dao

import distributed.dto.ItemGroupMetadata
import distributed.main.AppState
import distributed.util.FileUtil.downloadFile
import distributed.util.FileUtil.unzipFile
import distributed.util.FileUtil.zipFile
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.*

class Database {
    fun save(name: String, item: String) {
        val itemGroup = findByName(name)
        addToCollection(name, item)
        if (itemGroup == null) {
            AppState.addItemGroup(name)
        } else {
            itemGroup.itemCount += 1
            AppState.itemGroups.put(itemGroup.id, itemGroup)
        }
    }

    fun findByName(name: String): ItemGroupMetadata? {
        return AppState.itemGroups.values.find { it.name == name }
    }

    fun deleteItemGroup(name: String) {
        AppState.itemGroups.remove(findByName(name)?.id)
        deleteCollectionFile(name)
    }

    fun queryItemGroup(itemGroupName: String, itemQuery: String): List<String>? {
        val itemGroup = findByName(itemGroupName) ?: return emptyList()
        val itemGroupFile = File(itemGroup.name)
        if (itemQuery.isBlank())
            return itemGroupFile.useLines { lines -> lines.toList() }
        val queryObject = JSONObject(itemQuery)
        return itemGroupFile.useLines { lines ->
            lines.filter {
                val jsonObject = JSONObject(it)
                queryObject.keySet().any { key ->
                    if (jsonObject.has(key)) {
                        jsonObject.get(key) == queryObject.get(key)
                    } else false
                }
            }.toList()
        }
    }

    fun deleteFromItemGroup(itemGroupName: String, itemQuery: String) {
        val itemGroup = findByName(itemGroupName) ?: throw RuntimeException("Collection unavailable")
        if (itemQuery.isBlank())
            return
        val queryObject = JSONObject(itemQuery)
        val itemGroupFile = File(itemGroup.name)
        val newItems = getItemsNotInQuery(itemGroupFile, queryObject)
        itemGroup.itemCount = newItems.size
        AppState.itemGroups.put(itemGroup.id, itemGroup)
        if (newItems.isEmpty()) {
            itemGroupFile.delete()
        } else {
            itemGroupFile.writeText(newItems.reduce { acc, s -> acc + s })
        }
    }

    fun update(itemGroupName: String, id: String, item: String) {
        val itemGroup = findByName(itemGroupName) ?: throw RuntimeException("Collection unavailable")
        val itemGroupFile = File(itemGroup.name)
        val queryObject = JSONObject()
        queryObject.put("___id___", id)
        val notUpdatedItems = getItemsNotInQuery(itemGroupFile, queryObject)
        val asObject = JSONObject(item)
        asObject.put("___id___", id)
        val allObjects = notUpdatedItems.toMutableList()
        allObjects.add(asObject.toString())
        itemGroupFile.writeText(allObjects.reduce { acc, s -> acc + s })
    }

    fun getItemGroupFile(itemGroupName: String): ByteArrayOutputStream {
        val itemGroup = findByName(itemGroupName) ?: throw RuntimeException("Collection unavailable")
        val itemGroupFile = File(itemGroup.name)
        return zipFile(itemGroupFile, itemGroup)
    }

    fun importItemGroup(itemGroupsRequestMetadata: String) {
        val itemGroupSourceData = JSONObject(itemGroupsRequestMetadata)
        val fileStream =
            downloadFile(itemGroupSourceData.get("url").toString() + "/collections/" + itemGroupSourceData.get("name") + "/" + "copy")
        val metadata = unzipFile(fileStream, itemGroupSourceData.get("name").toString()) ?: throw RuntimeException("Unzip failed")
        AppState.addItemGroup(
            metadata.name,
            metadata.itemCount
        )
    }

    private fun getItemsNotInQuery(
        itemGroupFile: File,
        queryObject: JSONObject
    ): List<String> {
        return itemGroupFile.useLines { lines ->
            lines.filter {
                val jsonObject = JSONObject(it)
                queryObject.keySet().none { key ->
                    if (jsonObject.has(key)) {
                        jsonObject.get(key) == queryObject.get(key)
                    } else false
                }
            }.toList()
        }
    }

    private fun addToCollection(name: String, item: String) {
        val collectionFile = File(name)
        if (!collectionFile.exists()) {
            collectionFile.createNewFile()
        }
        JSONObject.testValidity(item)
        val asObject = JSONObject(item)
        asObject.put("___id___", UUID.randomUUID().toString())
        File(name).appendText(asObject.toString() + "\n")
    }

    private fun deleteCollectionFile(name: String) {
        File(name).delete()
    }

}