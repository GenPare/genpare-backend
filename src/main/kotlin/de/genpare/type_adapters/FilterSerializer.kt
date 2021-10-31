package de.genpare.type_adapters

import com.google.gson.JsonObject
import com.google.gson.JsonSerializationContext
import com.google.gson.JsonSerializer
import de.genpare.database.entities.Salary
import de.genpare.query.filters.*
import java.lang.reflect.Type

object FilterSerializer : JsonSerializer<AbstractFilter> {
    override fun serialize(filter: AbstractFilter, type: Type, context: JsonSerializationContext) =
        JsonObject().apply {
            addProperty("name", filter.name)

            when (filter) {
                is AbstractRangeFilter -> {
                    addProperty("min", filter.min)
                    addProperty("max", filter.max)
                }
                is JobTitleFilter -> {
                    addProperty("desiredJobTitle", filter.desiredJobTitle)
                }
                is LevelOfEducationFilter -> {
                    addProperty("desiredLevelOfEducation", filter.desiredLevelOfEducation.name)
                }
                is StateFilter -> {
                    addProperty("desiredState", filter.desiredState.name)
                }
            }
        }
}