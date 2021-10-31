package de.genpare.query.filters

import de.genpare.data.enums.State
import de.genpare.database.tables.SalaryTable
import org.jetbrains.exposed.sql.Expression

class StateFilter(
    val desiredState: State
) : AbstractFilter("state") {
    override val op =
        Expression.build { SalaryTable.state eq desiredState }
}