package de.genpare.query.filters

import com.google.gson.annotations.SerializedName
import org.jetbrains.exposed.sql.Expression

abstract class AbstractFilter(
    @SerializedName("name")
    val name: String
) {
    abstract val op: Expression<Boolean>
}