package distributed.main
import distributed.dao.Database
import distributed.dto.NodeStatusDto
import distributed.util.LoadUtil
import io.javalin.apibuilder.ApiBuilder.*
import io.javalin.Javalin
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

fun main(args: Array<String>) {
    val itemGroupDao = Database()
    val app = Javalin.create().apply {
        exception(Exception::class.java) { e, _ -> e.printStackTrace() }
        error(404) { ctx -> ctx.json("not found") }
    }.start(if(args[0].isBlank()) 7000 else args[0].toInt())

    val nodeName = "Node " + LocalDateTime.now().toInstant(ZoneOffset.UTC).epochSecond

    app.routes {

        get("/status") { ctx ->
            ctx.json(NodeStatusDto(nodeName, LoadUtil.getFreeSpacePercentage(), itemGroupDao.getItemGroupsList()))
        }

        get("/collections/:name") { ctx ->
            ctx.json(itemGroupDao.findByName(ctx.pathParam("name"))!!)
        }

        get("/collections/:name/query") { ctx ->
            val itemQuery = ctx.body()
            ctx.json(itemGroupDao.queryItemGroup(ctx.pathParam("name"), itemQuery)!!)
        }

        post("/collections/:name") { ctx ->
            val item = ctx.body()
            itemGroupDao.save(name = ctx.pathParam("name"), item = item)
            ctx.status(201)
        }

        delete("/collections/:name/query") { ctx ->
            val itemQuery = ctx.body()
            itemGroupDao.deleteFromItemGroup(itemGroupName = ctx.pathParam("name"), itemQuery = itemQuery)
            ctx.status(204)
        }

        delete("/collections/:name") { ctx ->
            itemGroupDao.deleteItemGroup(ctx.pathParam("name"))
            ctx.status(204)
        }
    }
}
