package com.foss.aihub.utils

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Air
import androidx.compose.material.icons.rounded.AutoAwesomeMosaic
import androidx.compose.material.icons.rounded.Brush
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material.icons.rounded.DataObject
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.DeveloperMode
import androidx.compose.material.icons.rounded.FlashOn
import androidx.compose.material.icons.rounded.Forum
import androidx.compose.material.icons.rounded.IntegrationInstructions
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.People
import androidx.compose.material.icons.rounded.PrivacyTip
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.RocketLaunch
import androidx.compose.material.icons.rounded.SentimentVerySatisfied
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.TravelExplore
import androidx.compose.ui.graphics.Color
import com.foss.aihub.models.AiService

const val USER_AGENT =
    "Mozilla/5.0 (Linux; Android 10; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/142.0.0.0 Mobile Safari/537.36"

val aiServices = listOf(
    AiService(
        "chatgpt",
        "ChatGPT",
        "https://chatgpt.com/",
        "General Purpose",
        "Versatile conversation assistant",
        Color(0xFF10A37F)
    ),
    AiService(
        "duck",
        "Duck AI",
        "https://duck.ai/",
        "Privacy First",
        "Anonymous AI conversations",
        Color(0xFF8B7355)
    ),
    AiService(
        "venice",
        "Venice",
        "https://venice.ai/chat",
        "Creative Arts",
        "Multimedia creative assistant",
        Color(0xFF9C27B0)
    ),
    AiService(
        "grok",
        "Grok",
        "https://grok.com/",
        "Entertainment",
        "Witty humorous companion",
        Color(0xFFE91E63)
    ),
    AiService(
        "lumo",
        "Lumo",
        "https://lumo.proton.me/",
        "Secure AI",
        "Encrypted privacy assistant",
        Color(0xFF2196F3)
    ),
    AiService(
        "deepseek",
        "Deepseek",
        "https://chat.deepseek.com/",
        "Development",
        "Code generation specialist",
        Color(0xFF00ACC1)
    ),
    AiService(
        "gemini",
        "Gemini",
        "https://gemini.google.com/",
        "Multimodal",
        "Google ecosystem integration",
        Color(0xFF4285F4)
    ),
    AiService(
        "claude",
        "Claude",
        "https://claude.ai/chat",
        "Professional Writing",
        "Long-form content expert",
        Color(0xFF673AB7)
    ),
    AiService(
        "perplexity",
        "Perplexity",
        "https://www.perplexity.ai/",
        "Research",
        "Citation-based search engine",
        Color(0xFF00AB97)
    ),
    AiService(
        "qwen",
        "Qwen",
        "https://chat.qwen.ai/",
        "Multilingual",
        "100+ languages support",
        Color(0xFFFF6D00)
    ),
    AiService(
        "mistral",
        "Mistral",
        "https://chat.mistral.ai/",
        "Efficiency",
        "Fast reasoning model",
        Color(0xFF3F51B5)
    ),
    AiService(
        "blackbox",
        "Blackbox",
        "https://app.blackbox.ai/",
        "Coding",
        "Code optimization assistant",
        Color(0xFFFF5722)
    ),
    AiService(
        "copilot",
        "Copilot",
        "https://copilot.microsoft.com/",
        "Productivity",
        "Microsoft 365 integration",
        Color(0xFF0078D4)
    ),
    AiService(
        "brave",
        "Brave",
        "https://search.brave.com/ask",
        "Search",
        "Privacy-focused searching",
        Color(0xFF198038)
    ),
    AiService(
        "huggingface",
        "HuggingFace",
        "https://huggingface.co/chat",
        "Open Source",
        "Open model experimentation",
        Color(0xFFFFD600)
    ),
    AiService(
        "meta",
        "Meta AI",
        "https://www.meta.ai/",
        "Social",
        "Social platform integration",
        Color(0xFF0081FB)
    ),
    AiService(
        "euria",
        "Euria",
        "https://euria.infomaniak.com/",
        "Privacy First",
        "Swiss AI assistant",
        Color(0xFF0D47A1)
    ),
    AiService(
        "zai",
        "Z.ai",
        "https://chat.z.ai/",
        "Conversational",
        "Social AI assistant",
        Color(0xFF8E24AA) // Purple 600
    ),
    AiService(
        "h2ogpte",
        "H2O GPTe",
        "https://h2ogpte.genai.h2o.ai/",
        "Data Science",
        "Enterprise AI platform",
        Color(0xFF00695C) // Teal 700
    ),
)

val serviceIcons = mapOf(
    "chatgpt" to Icons.Rounded.FlashOn,
    "duckai" to Icons.Rounded.PrivacyTip,
    "venice" to Icons.Rounded.Brush,
    "grok" to Icons.Rounded.SentimentVerySatisfied,
    "lumo" to Icons.Rounded.Shield,
    "deepseek" to Icons.Rounded.DeveloperMode,
    "gemini" to Icons.Rounded.AutoAwesomeMosaic,
    "claude" to Icons.Rounded.Description,
    "perplexity" to Icons.Rounded.TravelExplore,
    "qwen" to Icons.Rounded.Language,
    "mistral" to Icons.Rounded.Air,
    "blackbox" to Icons.Rounded.IntegrationInstructions,
    "copilot" to Icons.Rounded.RocketLaunch,
    "brave" to Icons.Rounded.Public,
    "huggingface" to Icons.Rounded.Cloud,
    "meta" to Icons.Rounded.People,
    "euria" to Icons.Rounded.Language,
    "zai" to Icons.Rounded.Forum,
    "h2ogpte" to Icons.Rounded.DataObject,
)

val serviceDomains = mapOf(
    // ChatGPT
    "chatgpt" to listOf(
        "chatgpt.com",
        "openai.com",
        "fileserviceuploadsperm.blob.core.windows.net",
        "cdn.oaistatic.com",
        "oaiusercontent.com",

        // Auth
        "cdn.auth0.com",
        "auth.openai.com",
        "auth-cdn.oaistatic.com"
    ),

    // Duck AI
    "duck" to listOf(
        "duck.ai", "duckduckgo.com"
    ),

    // Venice
    "venice" to listOf(
        "venice.ai"
    ),

    // Grok
    "grok" to listOf(
        "grok.com",

        // Auth
        "accounts.x.ai",
        "auth.grokipedia.com"
    ),

    // Lumo
    "lumo" to listOf(
        "lumo.proton.me",

        // Auth
        "account.proton.me"
    ),

    // Deepseek
    "deepseek" to listOf(
        "chat.deepseek.com", "cdn.deepseek.com", "static.deepseek.com"
    ),

    // Gemini
    "gemini" to listOf(
        "gemini.google.com",
        "fonts.gstatic.com",
        "www.gstatic.com",
    ),

    // Claude
    "claude" to listOf(
        "claude.ai"
    ),

    // Perplexity
    "perplexity" to listOf(
        "www.perplexity.ai", "pplx-next-static-public.perplexity.ai"
    ),

    // Qwen
    "qwen" to listOf(
        "chat.qwen.ai",
        "qwen.ai",
        "alicdn.com",
        "cdnjs.cloudflare.com",
        "assets.alicdn.com",
        "img.alicdn.com",
        "at.alicdn.com",
        "d.alicdn.com",
        "o.alicdn.com",
        "g.alicdn.com",
        "aplus.qwen.ai",
        "aliyuncs.com",
        "tdum.alibaba.com",
        "sg-wum.alibaba.com"
    ),

    // Mistral
    "mistral" to listOf(
        "chat.mistral.ai", "mistral.ai", "api.mistral.ai", "console.mistral.ai", "mistralcdn.net",

        // Auth
        "v2.auth.mistral.ai",
    ),

    // Blackbox
    "blackbox" to listOf(
        "app.blackbox.ai", "js.stripe.com", "m.stripe.network"
    ),

    // Copilot
    "copilot" to listOf(
        "copilot.microsoft.com"
    ),

    // Brave
    "brave" to listOf(
        "cdn.search.brave.com", "search.brave.com"
    ),

    // HuggingFace
    "huggingface" to listOf(
        "huggingface.co", "token.awswaf.com"
    ),

    // Meta
    "meta" to listOf(
        "www.meta.ai", "static.xx.fbcdn.net", "video.fdel64-1.fna.fbcdn.net"
    ),

    // Eurai
    "euria" to listOf(
        "euria.infomaniak.com",
        "web-components.storage.infomaniak.com",
        "fonts.storage.infomaniak.com",

        // Auth
        "welcome.infomaniak.com",
        "login.infomaniak.com",
        "login.storage.infomaniak.com"
    ),

    // Zai
    "zai" to listOf(
        "chat.z.ai", "z-cdn.chatglm.cn"
    ),

    // H2ogpte
    "h2ogpte" to listOf(
        "h2ogpte.genai.h2o.ai", "ok10static.oktacdn.com", "op3static.oktacdn.com",

        // Auth
        "id.public.h2o.ai"
    )
)

val alwaysBlockedDomains = mapOf(
    "chatgpt" to listOf(
        "ab.chatgpt.com"
    ),
    "duck" to listOf(
        "improving.duckduckgo.com"
    ),
    "grok" to listOf(
        "www.google-analytics.com",
    ),
    "gemini" to listOf(
        "www.google-analytics.com", "www.googletagmanager.com"
    ),
    "perplexity" to listOf(
        "suggest.perplexity.ai"
    ),
)

val commonAuthDomains = listOf(
    "accounts.google.com",
    "appleid.apple.com",
    "login.live.com",
    "login.microsoftonline.com",
    "github.githubassets.com",
    "challenges.cloudflare.com"
)

val TRACKING_PARAMS = setOf(
    "utm_source",
    "utm_medium",
    "utm_campaign",
    "utm_term",
    "utm_content",
    "fbclid",
    "gclid",
    "msclkid",
    "dclid",
    "zanpid",
    "ref",
    "source",
    "campaign",
    "medium",
    "content",
    "referrer"
)