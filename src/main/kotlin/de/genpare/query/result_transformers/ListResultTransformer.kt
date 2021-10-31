package de.genpare.query.result_transformers

import de.genpare.data.enums.Gender
import de.genpare.data.enums.LevelOfEducation
import de.genpare.data.enums.State
import de.genpare.modules.IntermediateResult
import de.genpare.util.Utils.toRange

class ListResultTransformer : AbstractResultTransformer("list") {
    data class AnonymizedSalary(
        val age: IntRange,
        val salary: IntRange,
        val gender: Gender,
        val jobTitle: String,
        val state: State,
        val levelOfEducation: LevelOfEducation
    )

    data class ListResult(
        val results: List<AnonymizedSalary>
    ) : AbstractResult("list")

    override fun transform(result: List<IntermediateResult>) =
        ListResult(
            result.map {
                AnonymizedSalary(
                    age = it.age.toRange(5),
                    salary = it.salary.toRange(500),
                    gender = it.gender,
                    jobTitle = it.jobTitle,
                    state = it.state,
                    levelOfEducation = it.levelOfEducation
                )
            }
        )
}