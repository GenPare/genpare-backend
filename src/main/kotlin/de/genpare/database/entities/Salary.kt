package de.genpare.database.entities

import de.genpare.data.dtos.SalaryDTO
import de.genpare.database.tables.SalaryTable
import org.jetbrains.exposed.dao.LongEntity
import org.jetbrains.exposed.dao.LongEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.transactions.transaction

class Salary(id: EntityID<Long>) : LongEntity(id) {
    companion object : LongEntityClass<Salary>(SalaryTable) {
        fun findByMemberId(id: Long) =
            transaction { Salary.find { SalaryTable.memberId eq id }.firstOrNull() }
    }

    var memberId by SalaryTable.memberId
    var salary by SalaryTable.salary
    var jobTitle by SalaryTable.jobTitle
    var state by SalaryTable.state
    var levelOfEducation by SalaryTable.levelOfEducation

    fun toDTO() =
        SalaryDTO(salary, jobTitle, state, levelOfEducation)
}