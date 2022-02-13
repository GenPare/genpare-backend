package de.genpare.modules

import de.genpare.database.Database
import io.ktor.application.*

fun Application.setup() {
    val dbUrl = System.getenv("GENPARE_DB_URL")
    val dbUser = System.getenv("GENPARE_DB_USER")
    val dbPassword = System.getenv("GENPARE_DB_PASSWORD")
    val maxRetries = System.getenv("GENPARE_DB_MAX_RETRIES").toInt()

    Database.init(log, dbUrl, dbUser, dbPassword, maxRetries)
}