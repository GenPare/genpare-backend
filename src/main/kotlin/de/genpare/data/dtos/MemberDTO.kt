package de.genpare.data.dtos

import de.genpare.data.enums.Gender
import java.time.LocalDate

data class MemberDTO(
    val id: Long?,
    val email: String,
    val name: String,
    val birthdate: LocalDate,
    val gender: Gender
)
