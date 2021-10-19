package de.genpare.database.tables

import de.genpare.util.Utils.enumColumnDefinition
import de.genpare.data.enums.Gender
import de.genpare.data.enums.State
import org.jetbrains.exposed.dao.id.LongIdTable

object SalaryTable : LongIdTable() {
    val memberId = long("member_id")
    val salary = integer("salary")
    val gender = enumColumnDefinition<Gender>("gender")
    val jobTitle = varchar("job_title", 63)
    val state = enumColumnDefinition<State>("state")
}