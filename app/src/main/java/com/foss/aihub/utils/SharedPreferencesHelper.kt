package com.foss.aihub.utils

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.core.content.edit
import com.foss.aihub.models.AppSettings
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class SettingsManager(context: Context) {
    private val sharedPref: SharedPreferences =
        context.getSharedPreferences("aihub_settings", Context.MODE_PRIVATE)
    private val gson = Gson()

    private val _settingsFlow = MutableStateFlow(loadSettings())
    val settingsFlow: StateFlow<AppSettings> = _settingsFlow

    fun cleanupAndFixServices() {
        val currentServices = aiServices.map { it.id }.toSet()

        Log.d("AI_HUB", "cleanupAndFixServices - Current: ${currentServices.size} services")

        val lastService = getLastOpenedService()
        if (lastService != null && lastService !in currentServices) {
            Log.w("AI_HUB", "Last service '$lastService' removed, clearing")
            sharedPref.edit { remove("lastOpenedService") }
        }

        updateSettings { settings ->
            val removedFromEnabled = settings.enabledServices.filter { it !in currentServices }
            val removedFromOrder = settings.serviceOrder.filter { it !in currentServices }

            if (removedFromEnabled.isNotEmpty() || removedFromOrder.isNotEmpty()) {
                Log.d("AI_HUB", "Cleaning removed services - enabled: ${removedFromEnabled.size}, order: ${removedFromOrder.size}")
            }

            val newEnabled = settings.enabledServices.filter { it in currentServices }.toMutableSet()
            val existingOrder = settings.serviceOrder.filter { it in currentServices }
            val newServices = currentServices.filter { it !in existingOrder }
            val newOrder = (existingOrder + newServices).toMutableList()

            newEnabled.addAll(newServices)

            settings.enabledServices = newEnabled
            settings.serviceOrder = newOrder

            if (settings.defaultServiceId !in currentServices) {
                val newDefault = newEnabled.firstOrNull() ?: currentServices.firstOrNull() ?: "chatgpt"
                Log.w("AI_HUB", "Default changed from '${settings.defaultServiceId}' to '$newDefault'")
                settings.defaultServiceId = newDefault
            }

            Log.d("AI_HUB", "Completed - enabled: ${settings.enabledServices.size}, order: ${settings.serviceOrder.size}, default: ${settings.defaultServiceId}")
        }
    }

    fun updateSettings(update: (AppSettings) -> Unit) {
        val current = loadSettings()
        update(current)
        saveSettings(current)
        _settingsFlow.value = current
    }

    private fun loadSettings(): AppSettings {
        return AppSettings(
            enableZoom = sharedPref.getBoolean("enableZoom", true),
            loadLastOpenedAI = sharedPref.getBoolean("loadLastOpenedAI", true),
            fontSize = sharedPref.getString("fontSize", "medium") ?: "medium",
            defaultServiceId = sharedPref.getString("defaultServiceId", "chatgpt") ?: "chatgpt",
            enabledServices = loadEnabledServices(),
            serviceOrder = loadServiceOrder()
        )
    }

    private fun saveSettings(settings: AppSettings) {
        sharedPref.edit {
            putBoolean("enableZoom", settings.enableZoom)
            putBoolean("loadLastOpenedAI", settings.loadLastOpenedAI)
            putString("fontSize", settings.fontSize)
            putString("defaultServiceId", settings.defaultServiceId)
            saveEnabledServices(settings.enabledServices)
            saveServiceOrder(settings.serviceOrder)
        }
    }

    private fun loadEnabledServices(): Set<String> {
        val json = sharedPref.getString("enabledServices", null)
        return if (json.isNullOrEmpty()) {
            aiServices.map { it.id }.toSet()
        } else {
            val type = object : TypeToken<Set<String>>() {}.type
            gson.fromJson(json, type)
        }
    }

    private fun saveEnabledServices(services: Set<String>) {
        val json = gson.toJson(services)
        sharedPref.edit { putString("enabledServices", json) }
    }

    private fun loadServiceOrder(): List<String> {
        val json = sharedPref.getString("serviceOrder", null)
        return if (json.isNullOrEmpty()) {
            aiServices.map { it.id }
        } else {
            val type = object : TypeToken<List<String>>() {}.type
            gson.fromJson(json, type)
        }
    }

    private fun saveServiceOrder(order: List<String>) {
        val json = gson.toJson(order)
        sharedPref.edit { putString("serviceOrder", json) }
    }

    fun saveLastOpenedService(serviceId: String) {
        sharedPref.edit { putString("lastOpenedService", serviceId) }
    }

    fun getLastOpenedService(): String? {
        return sharedPref.getString("lastOpenedService", null)
    }
}