package de.genpare.query.result_transformers

import de.genpare.data.enums.Gender
import de.genpare.modules.IntermediateResult
import kotlin.math.roundToInt

class AverageResultTransformer : AbstractResultTransformer("average") {
    data class AverageResult(
        val averageTotal: Int?,
        val averageMale: Int?,
        val averageFemale: Int?,
        val averageDiverse: Int?
    ) : AbstractResult("average")

    private fun List<IntermediateResult>.averageSalary() =
        map { it.salary }
            .average()
            .takeUnless { it.isNaN() }
            ?.roundToInt()

    override fun transform(result: List<IntermediateResult>): AverageResult {
        val averageTotal = result.averageSalary()
        val averageMale = result.filter { it.gender == Gender.MALE }.averageSalary()
        val averageFemale = result.filter { it.gender == Gender.FEMALE }.averageSalary()
        val averageDiverse = result.filter { it.gender == Gender.DIVERSE }.averageSalary()

        return AverageResult(averageTotal, averageMale, averageFemale, averageDiverse)
    }
}