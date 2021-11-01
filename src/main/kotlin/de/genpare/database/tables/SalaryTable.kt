package de.genpare.database.tables

import de.genpare.data.enums.LevelOfEducation
import de.genpare.data.enums.State
import de.genpare.util.Utils.enumColumnDefinition
import org.jetbrains.exposed.dao.id.LongIdTable

object SalaryTable : LongIdTable() {
    val memberId = long("member_id")
    val salary = integer("salary")
    val jobTitle = varchar("job_title", 63)
    val state = enumColumnDefinition<State>("state")
    val levelOfEducation = enumColumnDefinition<LevelOfEducation>("level_of_education")
}