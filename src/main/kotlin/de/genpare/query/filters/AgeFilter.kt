package de.genpare.query.filters

import de.genpare.database.tables.MemberTable
import org.jetbrains.exposed.sql.Expression
import org.jetbrains.exposed.sql.and
import java.time.LocalDate

class AgeFilter(min: Int, max: Int) : AbstractRangeFilter("age", min, max) {
    override val op =
        Expression.build {
            val upperDate = LocalDate.now().minusYears(min.toLong())
            val lowerDate = LocalDate.now().minusYears(max.toLong())

            (MemberTable.birthdate greaterEq lowerDate) and (MemberTable.birthdate lessEq upperDate)
        }
}