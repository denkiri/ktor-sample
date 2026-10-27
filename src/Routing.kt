package com.example
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.http.content.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import com.example.model.*
import io.ktor.server.request.receiveParameters

fun Application.configureRouting() {
    install(StatusPages) {
        exception<IllegalStateException> { call, cause ->
            call.respondText("App in illegal state as ${cause.message}")
        }
    }
    routing {
        staticResources("/content", "mycontent")
        staticResources("task-ui", "task-ui")
        get("/") {
            call.respondText("Hello, World!")
        }
        get("/test1") {
            val text = "<h1> Hello from Ktor </h1>"
            val type = ContentType.parse("text/html")
            call.respondText(text, type)
        }
        get("/sample") {
            call.respondResource("mycontent/sample.html")
        }
        get("/error-test") {
            throw IllegalStateException("Too Busy")
        }
        route("/tasks") {
            get {
                val tasks = TaskRepository.allTasks()
                call.respondText(
                    contentType = ContentType.parse("text/html"),
                    text = tasks.tasksAsTable()
                )
            }
            post {
                val formContent = call.receiveParameters()

                val params = Triple(
                    formContent["name"] ?: "",
                    formContent["description"] ?: "",
                    formContent["priority"] ?: ""
                )

                if (params.toList().any { it.isEmpty() }) {
                    call.respond(HttpStatusCode.BadRequest)
                    return@post
                }

                try {
                    val priority = Priority.valueOf(params.third)
                    TaskRepository.addTask(
                        Task(
                            params.first,
                            params.second,
                            priority
                        )
                    )

                    call.respond(HttpStatusCode.NoContent)
                } catch (ex: IllegalArgumentException) {
                    call.respond(HttpStatusCode.BadRequest)
                } catch (ex: IllegalStateException) {
                    call.respond(HttpStatusCode.BadRequest)
                }
            }

            get("/byPriority/{priority?}") {
                val priorityAsText = call.parameters["priority"]
                if (priorityAsText == null) {
                    call.respond(HttpStatusCode.BadRequest)
                    return@get
                }

                try {
                    val priority = Priority.valueOf(priorityAsText)
                    val tasks = TaskRepository.tasksByPriority(priority)

                    if (tasks.isEmpty()) {
                        call.respond(HttpStatusCode.NotFound)
                        return@get
                    }

                    call.respondText(
                        contentType = ContentType.parse("text/html"),
                        text = tasks.tasksAsTable()
                    )
                } catch (ex: IllegalArgumentException) {
                    call.respond(HttpStatusCode.BadRequest)
                }
            }
        }
    }

}