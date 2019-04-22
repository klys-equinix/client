package distributed.main
import distributed.dao.Database
import io.javalin.apibuilder.ApiBuilder.*
import io.javalin.Javalin

fun main(args: Array<String>) {
    val itemGroupDao = Database()

    val app = Javalin.create().apply {
        exception(Exception::class.java) { e, _ -> e.printStackTrace() }
        error(404) { ctx -> ctx.json("not found") }
    }.start(7000)

    app.routes {

        get("/item-groups") { ctx ->
            ctx.json(itemGroupDao.getItemGroupsList())
        }

        get("/item-groups/:name") { ctx ->
            ctx.json(itemGroupDao.findByName(ctx.pathParam("name"))!!)
        }

        get("/item-groups/:name/query") { ctx ->
            val itemQuery = ctx.body()
            ctx.json(itemGroupDao.queryItemGroup(ctx.pathParam("name"), itemQuery)!!)
        }

        post("/item-groups/:name") { ctx ->
            val item = ctx.body()
            itemGroupDao.save(name = ctx.pathParam("name"), item = item)
            ctx.status(201)
        }

        delete("/item-groups/:name/query") { ctx ->
            val itemQuery = ctx.body()
            itemGroupDao.deleteFromItemGroup(itemGroupName = ctx.pathParam("name"), itemQuery = itemQuery)
            ctx.status(204)
        }

        delete("/item-groups/:name") { ctx ->
            itemGroupDao.deleteItemGroup(ctx.pathParam("name"))
            ctx.status(204)
        }
    }
}
