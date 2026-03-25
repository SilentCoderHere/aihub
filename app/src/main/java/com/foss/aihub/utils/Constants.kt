package com.foss.aihub.utils

import android.content.Context
import android.util.Log
import androidx.compose.ui.graphics.Color
import com.foss.aihub.models.AiService

const val USER_AGENT_DESKTOP =
    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/145.0.0.0 Safari/537.36"
const val USER_AGENT_MOBILE =
    "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/145.0.7632.121 Mobile Safari/537.36"
const val GITHUB_USER_NAME = "SilentCoderHere"
const val GITHUB_REPO_NAME = "aihub"
const val CLOUD_BASE_URL = "https://silentcoderhere.github.io/aihub-config-data/"
const val AI_SERVICES_FILE = "ai_services_list.json"
const val DOMAIN_AND_RULES_FILE = "domain_filtering_rules.json"

var aiServices: List<AiService> = emptyList()