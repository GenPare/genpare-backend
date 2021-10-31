package de.genpare.data.dtos

import de.genpare.query.filters.AbstractFilter
import de.genpare.query.result_transformers.AbstractResultTransformer

data class FilterListDTO(
    val filters: List<AbstractFilter>,
    val resultTransformers: List<AbstractResultTransformer>
)