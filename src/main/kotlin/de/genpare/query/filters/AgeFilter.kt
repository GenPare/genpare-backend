package de.genpare.query.filters

import de.genpare.database.tables.MemberTable
import org.jetbrains.exposed.sql.Expression
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.javatime.year
import java.time.LocalDate

class AgeFilter(min: Int, max: Int) : AbstractRangeFilter("age", min, max) {
    override val op =
        Expression.build {
            val diff = MemberTable.birthdate - LocalDate.now()

            (diff.year() greaterEq min) and (diff.year() lessEq max)
        }
}