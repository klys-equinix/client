package distributed.dao

import org.json.JSONObject
import java.util.concurrent.atomic.AtomicInteger

class Database {

    val itemGroups = HashMap<Int, ItemGroup>()

    var lastId: AtomicInteger = AtomicInteger(itemGroups.size - 1)

    fun save(name: String, item: String) {
        val itemGroup = findByName(name)
        if (itemGroup == null) {
            val id = lastId.incrementAndGet()
            itemGroups.put(id, ItemGroup(name = name, items = mutableListOf(item), id = id, itemCount = 0))
        } else {
            itemGroup.items.add(item)
            itemGroup.itemCount += 1
            itemGroups.put(itemGroup.id, itemGroup)
        }
    }

    fun findByName(name: String): ItemGroup? {
        return itemGroups.values.find { it.name == name }
    }

    fun deleteItemGroup(id: Int) {
        itemGroups.remove(id)
    }

    fun getItemGroupsList(): List<ItemGroupDto> {
        return itemGroups.map { ItemGroupDto(it.value.id, it.value.name, it.value.itemCount) }
    }

    fun queryItemGroup(itemGroupName: String, itemQuery: String): List<String>? {
        val itemGroup = findByName(itemGroupName)
        val queryObject = JSONObject(itemQuery)
        if(queryObject.isEmpty)
            return itemGroup?.items
        return itemGroup?.items?.filter {
            val jsonObject = JSONObject(it)
            queryObject.keySet().any { key -> jsonObject.get(key) == queryObject.get(key) }
        }
    }

    fun deleteFromItemGroup(itemGroupName: String, itemQuery: String) {
        val itemGroup = findByName(itemGroupName)
        val queryObject = JSONObject(itemQuery)
        if(queryObject.isEmpty)
            return
        val newItems = itemGroup?.items?.filter {
            val jsonObject = JSONObject(it)
            queryObject.keySet().none { key -> jsonObject.get(key) == queryObject.get(key) }
        }!!
        itemGroup.items = newItems.toMutableList()
        itemGroup.itemCount -= 1
        itemGroups.put(itemGroup.id, itemGroup)
    }

}