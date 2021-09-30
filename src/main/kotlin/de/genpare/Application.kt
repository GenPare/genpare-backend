package de.genpare

import io.ktor.server.engine.*
import io.ktor.server.netty.*
import de.genpare.plugins.*

fun main() {
    embeddedServer(Netty, port = 8080, host = "127.0.0.1") {
        configureRouting()
    }.start(wait = true)
}
