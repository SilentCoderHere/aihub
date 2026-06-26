package com.foss.aihub.models

data class ModifiedServiceInfo(
    val service: AiService,
    val changes: List<String>
)

data class UpdateResult(
    val added: List<AiService>,
    val removed: List<AiService>,
    val modified: List<ModifiedServiceInfo>,
    val newCategories: Set<String>
)