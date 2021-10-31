package de.genpare.query.filters

import de.genpare.database.tables.SalaryTable
import org.jetbrains.exposed.sql.Expression

class JobTitleFilter(
    val desiredJobTitle: String
) : AbstractFilter("jobTitle") {
    override val op =
        Expression.build { SalaryTable.jobTitle eq desiredJobTitle }
}