package de.genpare.data.dtos

import de.genpare.data.enums.Gender
import de.genpare.data.enums.State

data class NewSalaryDTO(
    val sessionId: Long,
    val salary: Int,
    val gender: Gender,
    val jobTitle: String,
    val state: State
)