package de.genpare.type_adapters

import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import de.genpare.data.enums.LevelOfEducation
import java.lang.reflect.Type

object LevelOfEducationDeserializer : JsonDeserializer<LevelOfEducation> {
    override fun deserialize(
        json: JsonElement,
        type: Type,
        context: JsonDeserializationContext
    ): LevelOfEducation? {
        if (json.asString == null) return null

        return LevelOfEducation.valueOf(json.asString)
    }
}