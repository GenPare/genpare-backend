package de.genpare.type_adapters

import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import de.genpare.data.enums.LevelOfEducation
import de.genpare.data.enums.State
import de.genpare.query.filters.*
import java.lang.reflect.Type

object FilterDeserializer : JsonDeserializer<AbstractFilter> {
    override fun deserialize(json: JsonElement, type: Type, context: JsonDeserializationContext): AbstractFilter? {
        val jsonObj = json.asJsonObject

        return when (jsonObj.get("name").asString) {
            "age" -> AgeFilter(
                jsonObj.get("min").asInt,
                jsonObj.get("max").asInt
            )
            "jobTitle" -> JobTitleFilter(
                jsonObj.get("desiredJobTitle").asString
            )
            "levelOfEducation" -> LevelOfEducationFilter(
                LevelOfEducation.valueOf(jsonObj.get("desiredLevelOfEducation").asString)
            )
            "salary" -> SalaryFilter(
                jsonObj.get("min").asInt,
                jsonObj.get("max").asInt
            )
            "state" -> StateFilter(
                State.valueOf(jsonObj.get("desiredState").asString)
            )
            else -> null
        }
    }
}