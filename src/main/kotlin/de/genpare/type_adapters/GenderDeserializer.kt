package de.genpare.type_adapters

import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import de.genpare.data.enums.Gender
import java.lang.reflect.Type

object GenderDeserializer : JsonDeserializer<Gender> {
    override fun deserialize(
        json: JsonElement,
        type: Type,
        context: JsonDeserializationContext
    ): Gender? {
        if (json.asString == null) return null

        return Gender.valueOf(json.asString)
    }
}