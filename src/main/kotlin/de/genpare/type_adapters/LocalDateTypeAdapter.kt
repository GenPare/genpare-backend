package de.genpare.type_adapters

import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter
import java.time.LocalDate

object LocalDateTypeAdapter : TypeAdapter<LocalDate>() {
    override fun write(writer: JsonWriter, value: LocalDate?) {
        if (value == null) {
            writer.nullValue()
            return
        }

        writer.value(value.toString())
    }

    override fun read(reader: JsonReader): LocalDate? {
        if (reader.peek() == null) {
            reader.nextNull()
            return null
        }

        val str = reader.nextString()
        val parts = str.split("-").map(String::toInt)
        return LocalDate.of(parts[0], parts[1], parts[2])
    }
}