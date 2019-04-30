package distributed.dao

import org.json.JSONArray
import org.json.JSONObject
import java.io.File

object QueryHandler {
    fun getItemsNotInQuery(
        itemGroupFile: File,
        queryObject: JSONObject
    ): List<String> {
        return itemGroupFile.useLines { lines ->
            lines.filter {
                val jsonObject = JSONObject(it)
                queryObject.keySet().none { key ->
                    checkQueryKey(key, queryObject, jsonObject)
                }
            }.toList()
        }
    }

    fun getItemsInQuery(
        itemGroupFile: File,
        queryObject: JSONObject
    ): List<String> {
        return itemGroupFile.useLines { lines ->
            lines.filter {
                val jsonObject = JSONObject(it)
                queryObject.keySet().any { key ->
                    checkQueryKey(key, queryObject, jsonObject)
                }
            }.toList()
        }
    }

    private fun checkQueryKey(key: String, queryObject: JSONObject, dbObject: JSONObject): Boolean {
        return if (dbObject.has(key)) {
            if(queryObject.get(key) is String) {
                return dbObject.get(key) == queryObject.get(key)
            }
            if(queryObject.get(key) is JSONArray) {
                return (queryObject.get(key) as JSONArray).contains(dbObject.get(key))
            }
            return true
        } else false
    }
}