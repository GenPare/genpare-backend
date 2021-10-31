package de.genpare.query.result_transformers

import de.genpare.modules.IntermediateResult

abstract class AbstractResultTransformer(
    val name: String
) {
    abstract class AbstractResult(
        val resultOf: String
    )

    abstract fun transform(result: List<IntermediateResult>): AbstractResult
}