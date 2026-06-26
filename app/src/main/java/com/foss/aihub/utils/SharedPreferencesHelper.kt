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
import java.time.LocalDate

class SettingsManager(context: Context) {
    private val sharedPref: SharedPreferences =
        context.getSharedPreferences("aihub_settings", Context.MODE_PRIVATE)
    private val gson = Gson()

    init {
        val oldVersion = sharedPref.getInt(KEY_STORAGE_VERSION, 1)
        if (oldVersion < CURRENT_VERSION) {
            migrateToNewVersion()
            sharedPref.edit { putInt(KEY_STORAGE_VERSION, CURRENT_VERSION) }
        }
    }

    private val _settingsFlow = MutableStateFlow(loadSettings())
    val settingsFlow: StateFlow<AppSettings> = _settingsFlow

    companion object {
        private const val KEY_STORAGE_VERSION = "storageVersion"
        private const val CURRENT_VERSION = 2

        private const val KEY_DOMAINS_ETAG = "domainsETag"
        private const val KEY_AI_SERVICES_ETAG = "aiServicesETag"

        private const val KEY_ENABLED_SERVICES = "enabledServices"
        private const val KEY_SERVICE_ORDER = "serviceOrder"
        private const val KEY_FAVORITE_SERVICES = "favoriteServices"
        private const val KEY_LAST_OPENED_SERVICE = "lastOpenedService"
        private const val KEY_AI_SERVICES_LAST_UPDATED_DATE = "lastUpdatedDate"

        private const val KEY_DOMAINS_LAST_UPDATED_DATE = "domainsLastUpdatedDate"

        private const val KEY_CUSTOM_CSS = "customCss"
        private const val KEY_CUSTOM_JS = "customJs"

        private const val KEY_IS_PROXY = "isProxy"
        private const val KEY_PROXY_TYPE = "proxyType"
        private const val KEY_PROXY_HOST = "proxyHost"
        private const val KEY_PROXY_PORT = "proxyPort"

        private const val KEY_ENABLE_ZOOM = "enableZoom"
        private const val KEY_DESKTOP_VIEW = "desktopView"
        private const val KEY_THIRD_PARTY_COOKIES = "thirdPartyCookies"
        private const val KEY_FONT_SIZE_PERCENT = "fontSizePercent"
        private const val KEY_MAX_KEEP_ALIVE = "maxKeepAlive"
        private const val KEY_UPDATE_FREQUENCY_DAYS = "updateFrequencyDays"
        private const val KEY_BLOCK_ADS_TRACKERS = "blockAdsAndTrackers"

        private const val KEY_LOAD_LAST_OPENED_AI = "loadLastOpenedAI"
        private const val KEY_MULTIPLE_DEFAULT_AI = "multipleDefaultAI"
        private const val KEY_DEFAULT_SERVICE_NAME = "defaultServiceId"
        private const val KEY_DEFAULT_SERVICE_NAMES = "defaultServiceIds"

        private const val KEY_THEME = "theme"

        private const val KEY_FILTER_CATEGORIES = "filterCategories"
        private const val KEY_FILTER_PRICES = "filterPrices"
        private const val KEY_FILTER_PRIVACY = "filterPrivacy"
        private const val KEY_FILTER_LOGIN_REQUIRED = "filterLoginRequired"

        private const val KEY_ENABLE_NEW_SERVICES_BY_DEFAULT = "enableNewServicesByDefault"
        private const val KEY_PREFERRED_CATEGORIES = "preferredCategories"
        private const val KEY_PREFERRED_PRICES = "preferredPrices"
        private const val KEY_PREFERRED_PRIVACY = "preferredPrivacy"
        private const val KEY_PREFERRED_LOGIN_REQUIRED = "preferredLoginRequired"

        private const val KEY_ONBOARDING_COMPLETED = "onboardingCompleted"
    }

    private fun migrateToNewVersion() {
        val fontSize = sharedPref.getString("fontSize", "medium") ?: "medium"

        sharedPref.edit {
            putStringSet(KEY_ENABLED_SERVICES, emptySet())
            putStringSet(KEY_FAVORITE_SERVICES, emptySet())
            putString(KEY_LAST_OPENED_SERVICE, null)
            putInt(
                KEY_FONT_SIZE_PERCENT, when (fontSize) {
                    "x-small" -> 80
                    "small" -> 90
                    "medium" -> 100
                    "large" -> 110
                    "x-large" -> 120
                    else -> 100
                }
            )

            remove("defaultServiceId")
            remove("defaultServiceIds")
            remove("blockUnnecessaryConnections")
        }
    }

    private fun loadSettings(): AppSettings = AppSettings(
        theme = sharedPref.getString(KEY_THEME, "auto") ?: "auto",
        loadLastOpenedAI = sharedPref.getBoolean(KEY_LOAD_LAST_OPENED_AI, true),
        multipleDefaultAi = sharedPref.getBoolean(KEY_MULTIPLE_DEFAULT_AI, false),
        defaultServiceName = sharedPref.getString(KEY_DEFAULT_SERVICE_NAME, null),
        defaultServiceNames = sharedPref.safeGetStringSet(KEY_DEFAULT_SERVICE_NAMES, emptySet())
            ?: emptySet(),
        serviceOrder = loadServiceOrder(),
        enabledServices = loadEnabledServices(),
        favoriteServices = loadFavoriteServices(),
        maxKeepAlive = sharedPref.getInt(KEY_MAX_KEEP_ALIVE, 5),
        enableZoom = sharedPref.getBoolean(KEY_ENABLE_ZOOM, true),
        desktopView = sharedPref.getBoolean(KEY_DESKTOP_VIEW, false),
        thirdPartyCookies = sharedPref.getBoolean(KEY_THIRD_PARTY_COOKIES, false),
        fontSizePercentage = sharedPref.getInt(KEY_FONT_SIZE_PERCENT, 100),
        updateFrequencyDays = sharedPref.getInt(KEY_UPDATE_FREQUENCY_DAYS, 3),
        blockAdsAndTrackers = sharedPref.getBoolean(KEY_BLOCK_ADS_TRACKERS, true),
        isProxy = sharedPref.getBoolean(KEY_IS_PROXY, false),
        proxyType = sharedPref.getString(KEY_PROXY_TYPE, "http") ?: "http",
        proxyHost = sharedPref.getString(KEY_PROXY_HOST, "localhost") ?: "localhost",
        proxyPort = sharedPref.getString(KEY_PROXY_PORT, "9050") ?: "9050",
        customCss = sharedPref.getString(KEY_CUSTOM_CSS, "") ?: "",
        customJs = sharedPref.getString(KEY_CUSTOM_JS, "") ?: "",
        filterCategories = sharedPref.safeGetStringSet(KEY_FILTER_CATEGORIES, emptySet())
            ?: emptySet(),
        filterPrices = sharedPref.safeGetStringSet(KEY_FILTER_PRICES, emptySet()) ?: emptySet(),
        filterPrivacy = sharedPref.safeGetStringSet(KEY_FILTER_PRIVACY, emptySet()) ?: emptySet(),
        filterLoginRequired = sharedPref.getString(KEY_FILTER_LOGIN_REQUIRED, null)
            ?.toBooleanStrictOrNull(),
        enableNewServicesByDefault = sharedPref.getBoolean(
            KEY_ENABLE_NEW_SERVICES_BY_DEFAULT, false
        ),
        preferredCategories = sharedPref.safeGetStringSet(KEY_PREFERRED_CATEGORIES, emptySet())
            ?: emptySet(),
        preferredPrices = sharedPref.safeGetStringSet(KEY_PREFERRED_PRICES, emptySet())
            ?: emptySet(),
        preferredPrivacy = sharedPref.safeGetStringSet(KEY_PREFERRED_PRIVACY, emptySet())
            ?: emptySet(),
        preferredLoginRequired = sharedPref.getString(KEY_PREFERRED_LOGIN_REQUIRED, null)
            ?.toBooleanStrictOrNull()
    )

    private fun saveSettings(settings: AppSettings) {
        sharedPref.edit {
            putString(KEY_THEME, settings.theme)
            putBoolean(KEY_LOAD_LAST_OPENED_AI, settings.loadLastOpenedAI)
            putBoolean(KEY_MULTIPLE_DEFAULT_AI, settings.multipleDefaultAi)
            putString(KEY_DEFAULT_SERVICE_NAME, settings.defaultServiceName)
            putStringSet(KEY_DEFAULT_SERVICE_NAMES, settings.defaultServiceNames)
            saveServiceOrder(settings.serviceOrder)
            saveEnabledServices(settings.enabledServices)
            saveFavoriteServices(settings.favoriteServices)
            putInt(KEY_MAX_KEEP_ALIVE, settings.maxKeepAlive)
            putBoolean(KEY_ENABLE_ZOOM, settings.enableZoom)
            putBoolean(KEY_DESKTOP_VIEW, settings.desktopView)
            putBoolean(KEY_THIRD_PARTY_COOKIES, settings.thirdPartyCookies)
            putInt(KEY_FONT_SIZE_PERCENT, settings.fontSizePercentage)
            putInt(KEY_UPDATE_FREQUENCY_DAYS, settings.updateFrequencyDays)
            putBoolean(KEY_BLOCK_ADS_TRACKERS, settings.blockAdsAndTrackers)
            putBoolean(KEY_IS_PROXY, settings.isProxy)
            putString(KEY_PROXY_TYPE, settings.proxyType)
            putString(KEY_PROXY_HOST, settings.proxyHost)
            putString(KEY_PROXY_PORT, settings.proxyPort)
            putString(KEY_CUSTOM_CSS, settings.customCss)
            putString(KEY_CUSTOM_JS, settings.customJs)
            putStringSet(KEY_FILTER_CATEGORIES, settings.filterCategories)
            putStringSet(KEY_FILTER_PRICES, settings.filterPrices)
            putStringSet(KEY_FILTER_PRIVACY, settings.filterPrivacy)
            putString(KEY_FILTER_LOGIN_REQUIRED, settings.filterLoginRequired?.toString())
            putBoolean(KEY_ENABLE_NEW_SERVICES_BY_DEFAULT, settings.enableNewServicesByDefault)
            putStringSet(KEY_PREFERRED_CATEGORIES, settings.preferredCategories)
            putStringSet(KEY_PREFERRED_PRICES, settings.preferredPrices)
            putStringSet(KEY_PREFERRED_PRIVACY, settings.preferredPrivacy)
            putString(KEY_PREFERRED_LOGIN_REQUIRED, settings.preferredLoginRequired?.toString())
        }
    }

    fun updateSettings(update: (AppSettings) -> Unit) {
        val current = loadSettings()
        update(current)
        saveSettings(current)
        _settingsFlow.value = current
    }

    fun getDomainsEtag(): String? = sharedPref.getString(KEY_DOMAINS_ETAG, null)

    fun saveDomainsEtag(etag: String) {
        sharedPref.edit { putString(KEY_DOMAINS_ETAG, etag) }
    }

    fun getAiServicesEtag(): String? = sharedPref.getString(KEY_AI_SERVICES_ETAG, null)

    fun saveAiServicesEtag(etag: String) {
        sharedPref.edit { putString(KEY_AI_SERVICES_ETAG, etag) }
    }

    fun loadEnabledServices(): Set<String> {
        return sharedPref.safeGetStringSet(KEY_ENABLED_SERVICES, emptySet()) ?: emptySet()
    }

    fun saveEnabledServices(services: Set<String>) {
        sharedPref.edit { putStringSet(KEY_ENABLED_SERVICES, services) }
    }

    fun domainsLastUpdatedDate(): LocalDate? {
        val dateString = sharedPref.getString(KEY_DOMAINS_LAST_UPDATED_DATE, null)
        return dateString?.let { LocalDate.parse(it) }
    }

    fun saveDomainsLastUpdatedDate() {
        sharedPref.edit { putString(KEY_DOMAINS_LAST_UPDATED_DATE, LocalDate.now().toString()) }
    }

    fun loadServiceOrder(): List<String> {
        val json = sharedPref.getString(KEY_SERVICE_ORDER, null)
        return if (!json.isNullOrEmpty()) {
            val type = object : TypeToken<List<String>>() {}.type
            gson.fromJson(json, type)
        } else {
            emptyList()
        }
    }

    fun saveServiceOrder(order: List<String>) {
        val json = gson.toJson(order)
        sharedPref.edit { putString(KEY_SERVICE_ORDER, json) }
    }

    fun loadFavoriteServices(): Set<String> {
        return sharedPref.safeGetStringSet(KEY_FAVORITE_SERVICES, emptySet()) ?: emptySet()
    }

    fun saveFavoriteServices(favorites: Set<String>) {
        sharedPref.edit { putStringSet(KEY_FAVORITE_SERVICES, favorites) }
    }

    fun saveLastOpenedService(serviceName: String) {
        sharedPref.edit { putString(KEY_LAST_OPENED_SERVICE, serviceName) }
    }

    fun getLastOpenedService(): String? = sharedPref.getString(KEY_LAST_OPENED_SERVICE, null)

    fun getAiServicesLastUpdatedDate(): LocalDate? {
        val dateString = sharedPref.getString(KEY_AI_SERVICES_LAST_UPDATED_DATE, null)
        return dateString?.let { LocalDate.parse(it) }
    }

    fun saveLastUpdatedDate() {
        sharedPref.edit { putString(KEY_AI_SERVICES_LAST_UPDATED_DATE, LocalDate.now().toString()) }
    }

    private fun SharedPreferences.safeGetStringSet(
        key: String, defValue: Set<String>?
    ): Set<String>? {
        return try {
            getStringSet(key, defValue)
        } catch (e: ClassCastException) {
            Log.e("SettingsManager", "Type mismatch for key $key, returning default", e)
            defValue
        }
    }

    fun getDefaultService(): String? {
        return sharedPref.getString(KEY_DEFAULT_SERVICE_NAME, null)
    }

    fun isOnboardingCompleted(): Boolean = sharedPref.getBoolean(KEY_ONBOARDING_COMPLETED, false)

    fun setOnboardingCompleted(completed: Boolean = true) {
        sharedPref.edit { putBoolean(KEY_ONBOARDING_COMPLETED, completed) }
    }
}