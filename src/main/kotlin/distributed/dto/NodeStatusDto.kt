package distributed.dto

data class NodeStatusDto(val name: String, val load: Double, val collections: List<ItemGroupDto>)