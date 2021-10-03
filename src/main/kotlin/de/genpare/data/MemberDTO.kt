package de.genpare.data

import com.beust.klaxon.Json

data class MemberDTO(
    @Json(serializeNull = false)
    val id: Long?,
    val email: String,
    val name: String
)
