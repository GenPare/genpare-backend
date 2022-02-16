package de.genpare.type_adapters

import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import de.genpare.query.result_transformers.AbstractResultTransformer
import de.genpare.query.result_transformers.AverageResultTransformer
import de.genpare.query.result_transformers.ListResultTransformer
import java.lang.reflect.Type

object ResultTransformerDeserializer : JsonDeserializer<AbstractResultTransformer> {
    override fun deserialize(
        json: JsonElement,
        type: Type,
        context: JsonDeserializationContext
    ): AbstractResultTransformer? {
        val jsonObj = json.asJsonObject

        return when (jsonObj.get("name").asString) {
            "average" -> AverageResultTransformer()
            "list" -> ListResultTransformer()
            else -> null
        }
    }
}