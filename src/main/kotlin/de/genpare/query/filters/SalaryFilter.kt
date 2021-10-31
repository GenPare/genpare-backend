package de.genpare.query.filters

import de.genpare.database.tables.SalaryTable
import org.jetbrains.exposed.sql.Expression
import org.jetbrains.exposed.sql.and

class SalaryFilter(min: Int, max: Int) : AbstractRangeFilter("salary", min, max) {
    override val op =
        Expression.build { (SalaryTable.salary greaterEq min) and (SalaryTable.salary lessEq max) }
}