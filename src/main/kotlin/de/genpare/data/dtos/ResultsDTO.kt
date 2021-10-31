package de.genpare.data.dtos

import de.genpare.query.result_transformers.AbstractResultTransformer

data class ResultsDTO(
    val results: List<AbstractResultTransformer.AbstractResult>
)
