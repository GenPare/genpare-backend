package de.genpare.type_adapters

import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import de.genpare.query.result_transformers.AbstractResultTransformer
import de.genpare.query.result_transformers.AverageResultTransformer
import de.genpare.query.result_transformers.ListResultTransformer
import java.lang.reflect.Type

object ResultDeserializer : JsonDeserializer<AbstractResultTransformer.AbstractResult> {
    override fun deserialize(
        json: JsonElement,
        type: Type,
        context: JsonDeserializationContext
    ): AbstractResultTransformer.AbstractResult? {
        val jsonObj = json.asJsonObject

        return when (jsonObj.get("resultOf").asString) {
            "average" -> context.deserialize<AverageResultTransformer.AverageResult>(
                json,
                AverageResultTransformer.AverageResult::class.java
            )
            "list" -> context.deserialize<ListResultTransformer.ListResult>(
                json,
                ListResultTransformer.ListResult::class.java
            )
            else -> null
        }
    }
}