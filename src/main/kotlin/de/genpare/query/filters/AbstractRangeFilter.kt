package de.genpare.query.filters

abstract class AbstractRangeFilter(
    name: String,
    val min: Int,
    val max: Int
) : AbstractFilter(name)