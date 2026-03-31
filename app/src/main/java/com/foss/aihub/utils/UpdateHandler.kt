package com.foss.aihub.utils

import android.content.Context
import com.foss.aihub.R
import com.foss.aihub.models.AiService

data class DetailedUpdateDetails(
    val serviceChanges: ServiceChanges,
    val serviceDomainChanges: ServiceChanges,
    val alwaysBlockedDomainChanges: ServiceChanges,
    val commonAuthDomainChanges: ServiceChanges,
    val trackingParamsChanges: ServiceChanges,
)

data class ServiceChanges(
    val added: List<String>, val removed: List<String>, val changed: List<String>
)

data class UpdateResult(
    val message: String?, val details: DetailedUpdateDetails
)

suspend fun checkUpdate(context: Context): UpdateResult {
    val settingsManager = SettingsManager(context)

    val oldAiServices = aiServices
    val oldServiceDomains = settingsManager.getServiceDomains()
    val oldAlwaysBlockedDomains = settingsManager.getAlwaysBlockedDomains()
    val oldCommonAuthDomains = settingsManager.getCommonAuthDomains()
    val oldTrackingParams = settingsManager.getTrackingParams()

    val (domainUpdated, aiUpdated) = ConfigUpdater.updateBothIfNeeded(context)

    val newAiServices = aiServices
    val newServiceDomains = settingsManager.getServiceDomains()
    val newAlwaysBlockedDomains = settingsManager.getAlwaysBlockedDomains()
    val newCommonAuthDomains = settingsManager.getCommonAuthDomains()
    val newTrackingParams = settingsManager.getTrackingParams()

    val details = computeDetailedUpdateDetails(
        oldAiServices,
        newAiServices,
        oldServiceDomains,
        newServiceDomains,
        oldAlwaysBlockedDomains,
        newAlwaysBlockedDomains,
        oldCommonAuthDomains,
        newCommonAuthDomains,
        oldTrackingParams,
        newTrackingParams
    )

    if (!(domainUpdated || aiUpdated)) {
        return UpdateResult(null, details)
    }

    val message = when {
        domainUpdated && aiUpdated -> context.getString(R.string.msg_update_all_success)
        domainUpdated -> context.getString(R.string.msg_domain_update_success)
        else -> context.getString(R.string.msg_ai_update_success)
    }

    return UpdateResult(message, details)
}

private fun computeDetailedUpdateDetails(
    oldAiServices: List<AiService>,
    newAiServices: List<AiService>,
    oldServiceDomains: Map<String, List<String>>,
    newServiceDomains: Map<String, List<String>>,
    oldAlwaysBlockedDomains: Map<String, List<String>>,
    newAlwaysBlockedDomains: Map<String, List<String>>,
    oldCommonAuthDomains: List<String>,
    newCommonAuthDomains: List<String>,
    oldTrackingParams: List<String>,
    newTrackingParams: List<String>
): DetailedUpdateDetails {
    fun computeServiceChanges(
        oldMap: Map<String, List<String>>, newMap: Map<String, List<String>>
    ): ServiceChanges {
        val added = newMap.keys.filter { it !in oldMap }
        val removed = oldMap.keys.filter { it !in newMap }
        val changed = (oldMap.keys intersect newMap.keys).filter { id ->
            oldMap[id].orEmpty().toSet() != newMap[id].orEmpty().toSet()
        }
        return ServiceChanges(added, removed, changed)
    }

    val oldServiceIds = oldAiServices.map { it.name }.toSet()
    val newServiceIds = newAiServices.map { it.name }.toSet()
    val addedServices = (newServiceIds - oldServiceIds).toList()
    val removedServices = (oldServiceIds - newServiceIds).toList()
    val changedServices = (oldServiceIds intersect newServiceIds).filter { name ->
        val oldService = oldAiServices.find { it.name == name }
        val newService = newAiServices.find { it.name == name }
        oldService != newService
    }
    val serviceChanges = ServiceChanges(addedServices, removedServices, changedServices)

    val serviceDomainChanges = computeServiceChanges(oldServiceDomains, newServiceDomains)
    val alwaysBlockedDomainChanges =
        computeServiceChanges(oldAlwaysBlockedDomains, newAlwaysBlockedDomains)

    val oldCommonAuthSet = oldCommonAuthDomains.toSet()
    val newCommonAuthSet = newCommonAuthDomains.toSet()
    val addedCommonAuthDomains = (newCommonAuthSet - oldCommonAuthSet).toList()
    val removedCommonAuthDomains = (oldCommonAuthSet - newCommonAuthSet).toList()
    val commonAuthDomainChanges =
        ServiceChanges(addedCommonAuthDomains, removedCommonAuthDomains, emptyList())

    val oldTrackingParamsSet = oldTrackingParams.toSet()
    val newTrackingParamsSet = newTrackingParams.toSet()
    val addedTrackingParams = (newTrackingParamsSet - oldTrackingParamsSet).toList()
    val removedTrackingParams = (oldTrackingParamsSet - newTrackingParamsSet).toList()
    val trackingParamsChanges =
        ServiceChanges(addedTrackingParams, removedTrackingParams, emptyList())

    return DetailedUpdateDetails(
        serviceChanges = serviceChanges,
        serviceDomainChanges = serviceDomainChanges,
        alwaysBlockedDomainChanges = alwaysBlockedDomainChanges,
        commonAuthDomainChanges = commonAuthDomainChanges,
        trackingParamsChanges = trackingParamsChanges
    )
}