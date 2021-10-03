package de.genpare.database

import de.genpare.database.tables.MemberTable
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction

object Database {
    private lateinit var url: String
    private lateinit var user: String
    private lateinit var password: String

    val db by lazy {
        val db = Database.connect(
            url = url,
            driver = "com.mysql.jdbc.Driver",
            user = user,
            password = password
        )

        transaction {
            SchemaUtils.create(MemberTable)
        }

        db
    }

    fun init(url: String, user: String, password: String) {
        this.url = url
        this.user = user
        this.password = password

        db // We need to connect!
    }
}