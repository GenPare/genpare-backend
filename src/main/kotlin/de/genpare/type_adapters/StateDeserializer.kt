package de.genpare.type_adapters

import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import de.genpare.data.enums.State
import java.lang.reflect.Type

object StateDeserializer : JsonDeserializer<State> {
    override fun deserialize(
        json: JsonElement,
        type: Type,
        context: JsonDeserializationContext
    ): State? {
        if (json.asString == null) return null

        return State.valueOf(json.asString)
    }
}