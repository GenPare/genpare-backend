package de.genpare.data.dtos

import de.genpare.data.enums.Gender

data class ModifyDataDTO(
    val name: String?,
    val gender: Gender?,
    val sessionId: String
)