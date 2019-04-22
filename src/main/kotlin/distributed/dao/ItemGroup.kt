package distributed.dao

data class ItemGroup(val name: String, var items: MutableList<String>, val id: Int, var itemCount: Int)