package de.genpare.database.tables

import org.jetbrains.exposed.dao.id.LongIdTable

object MemberTable : LongIdTable() {
    val email = text("email")
    val name = varchar("name", 20)
    val sessionId = long("session_id").default(0)
}