package de.genpare.data.dtos

import de.genpare.data.enums.LevelOfEducation
import de.genpare.data.enums.State

data class ModifySalaryDTO(
    val sessionId: String,
    val salary: Int?,
    val jobTitle: String?,
    val state: State?,
    val levelOfEducation: LevelOfEducation?
)