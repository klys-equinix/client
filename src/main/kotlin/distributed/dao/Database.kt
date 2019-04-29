package distributed.dao

import distributed.dto.ItemGroupMetadata
import distributed.main.AppState
import org.json.JSONObject
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
                    jsonObject.get(key) == queryObject.get(key)
                }
            }.toList()
        }
    }

    fun deleteFromItemGroup(itemGroupName: String, itemQuery: String) {
        val itemGroup = findByName(itemGroupName) ?: return
        val queryObject = JSONObject(itemQuery)
        if (queryObject.isEmpty)
            return
        val itemGroupFile = File(itemGroup.name)
        val newItems = itemGroupFile.useLines { lines ->
            lines.filter {
                val jsonObject = JSONObject(it)
                queryObject.keySet().none { key -> jsonObject.get(key) == queryObject.get(key) }
            }.toList()
        }
        itemGroup.itemCount = newItems.size
        AppState.itemGroups.put(itemGroup.id, itemGroup)
        if(newItems.isEmpty()) {
            itemGroupFile.delete()
        } else {
            itemGroupFile.writeText(newItems.reduce { acc, s ->  acc + s})
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