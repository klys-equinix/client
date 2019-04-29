package distributed.dao

import distributed.dto.ItemGroupMetadata
import distributed.main.AppState
import distributed.util.FileUtil.downloadFile
import distributed.util.FileUtil.unzipFile
import distributed.util.FileUtil.zipFile
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File

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
        val queryObject = JSONObject(itemQuery)
        if (queryObject.isEmpty)
            return itemGroupFile.useLines { lines -> lines.toList() }
        return itemGroupFile.useLines { lines ->
            lines.filter {
                val jsonObject = JSONObject(it)
                queryObject.keySet().any { key ->
                    if(jsonObject.has(key)) {
                        jsonObject.get(key) == queryObject.get(key)
                    } else false
                }
            }.toList()
        }
    }

    fun deleteFromItemGroup(itemGroupName: String, itemQuery: String) {
        val itemGroup = findByName(itemGroupName) ?: throw Exception("Collection unavailable")
        val queryObject = JSONObject(itemQuery)
        if (queryObject.isEmpty)
            return
        val itemGroupFile = File(itemGroup.name)
        val newItems = getItemsAfterDelete(itemGroupFile, queryObject)
        itemGroup.itemCount = newItems.size
        AppState.itemGroups.put(itemGroup.id, itemGroup)
        if(newItems.isEmpty()) {
            itemGroupFile.delete()
        } else {
            itemGroupFile.writeText(newItems.reduce { acc, s ->  acc + s})
        }
    }

    fun getItemGroupFile(itemGroupName: String): ByteArrayOutputStream {
        val itemGroup = findByName(itemGroupName) ?: throw Exception("Collection unavailable")
        val itemGroupFile = File(itemGroup.name)
        return zipFile(itemGroupFile, itemGroup.name)
    }

    fun importItemGroup(itemGroupsRequestMetadata: String) {
        val itemGroupSourceData = JSONObject(itemGroupsRequestMetadata)
        val fileStream = downloadFile(itemGroupSourceData.get("url").toString() + "/collections/" + itemGroupSourceData.get("name") + "/" + "copy")
        unzipFile(fileStream, itemGroupSourceData.get("name").toString())
        AppState.addItemGroup(itemGroupSourceData.get("name").toString(), itemGroupSourceData.get("size").toString().toInt())
    }

    private fun getItemsAfterDelete(
        itemGroupFile: File,
        queryObject: JSONObject
    ): List<String> {
        return itemGroupFile.useLines { lines ->
            lines.filter {
                val jsonObject = JSONObject(it)
                queryObject.keySet().none { key ->
                    if(jsonObject.has(key)) {
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
        File(name).appendText(item.replace("\n", "") + "\n")
    }

    private fun deleteCollectionFile(name: String) {
        File(name).delete()
    }

}