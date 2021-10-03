package de.genpare.modules

import de.genpare.database.Database
import io.ktor.application.*

fun Application.setup() {
    val dbUrl = environment.config.property("database.url").getString()
    val dbUser = environment.config.property("database.user").getString()
    val dbPassword = environment.config.property("database.password").getString()

    Database.init(dbUrl, dbUser, dbPassword)
}