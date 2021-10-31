package de.genpare.query.filters

import de.genpare.data.enums.LevelOfEducation
import de.genpare.database.tables.SalaryTable
import org.jetbrains.exposed.sql.Expression

class LevelOfEducationFilter(
    val desiredLevelOfEducation: LevelOfEducation
) : AbstractFilter("levelOfEducation") {
    override val op =
        Expression.build { SalaryTable.levelOfEducation eq desiredLevelOfEducation }
}