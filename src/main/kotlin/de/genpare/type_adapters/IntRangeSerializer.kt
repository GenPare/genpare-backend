package de.genpare.type_adapters

import com.google.gson.JsonObject
import com.google.gson.JsonSerializationContext
import com.google.gson.JsonSerializer
import java.lang.reflect.Type

object IntRangeSerializer : JsonSerializer<IntRange> {
    override fun serialize(intRange: IntRange, type: Type, context: JsonSerializationContext) =
        JsonObject().apply {
            addProperty("min", intRange.first)
            addProperty("max", intRange.last)
        }
}