package de.genpare.database.tables

import de.genpare.data.enums.Gender
import de.genpare.util.Utils.enumColumnDefinition
import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.javatime.date

object MemberTable : LongIdTable() {
    val email = text("email")
    val name = varchar("name", 20)
    val sessionId = long("session_id").default(0)
    val birthdate = date("birthdate")
    val gender = enumColumnDefinition<Gender>("gender")
}