package de.genpare.modules

import io.ktor.application.*
import io.ktor.response.*
import io.ktor.routing.*

fun Application.helloWorld() {
    routing {
        get("/") {
            call.respondText("Hello World!")
        }
    }
}
