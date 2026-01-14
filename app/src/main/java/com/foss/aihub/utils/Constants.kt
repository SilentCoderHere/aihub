package com.foss.aihub.utils

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Chat
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
import androidx.compose.material.icons.rounded.Psychology
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
        Color(0xFF10A37F) // Green
    ),
    AiService(
        "duck",
        "Duck AI",
        "https://duck.ai/",
        "Privacy First",
        "Anonymous AI conversations",
        Color(0xFF8B7355) // Brown
    ),
    AiService(
        "venice",
        "Venice",
        "https://venice.ai/chat",
        "Creative Arts",
        "Multimedia creative assistant",
        Color(0xFF9C27B0) // Purple
    ),
    AiService(
        "grok",
        "Grok",
        "https://grok.com/",
        "Entertainment",
        "Witty humorous companion",
        Color(0xFFE91E63) // Pink
    ),
    AiService(
        "lumo",
        "Lumo",
        "https://lumo.proton.me/",
        "Secure AI",
        "Encrypted privacy assistant",
        Color(0xFF2196F3) // Blue
    ),
    AiService(
        "deepseek",
        "Deepseek",
        "https://chat.deepseek.com/",
        "Development",
        "Code generation specialist",
        Color(0xFF00ACC1) // Cyan
    ),
    AiService(
        "gemini",
        "Gemini",
        "https://gemini.google.com/",
        "Multimodal",
        "Google ecosystem integration",
        Color(0xFF4285F4) // Blue
    ),
    AiService(
        "claude",
        "Claude",
        "https://claude.ai/chat",
        "Professional Writing",
        "Long-form content expert",
        Color(0xFF673AB7) // Purple
    ),
    AiService(
        "perplexity",
        "Perplexity",
        "https://www.perplexity.ai/",
        "Research",
        "Citation-based search engine",
        Color(0xFF00AB97) // Teal
    ),
    AiService(
        "qwen",
        "Qwen",
        "https://chat.qwen.ai/",
        "Multilingual",
        "100+ languages support",
        Color(0xFFFF6D00) // Orange
    ),
    AiService(
        "mistral",
        "Mistral",
        "https://chat.mistral.ai/",
        "Efficiency",
        "Fast reasoning model",
        Color(0xFF3F51B5) // Indigo
    ),
    AiService(
        "blackbox",
        "Blackbox",
        "https://app.blackbox.ai/",
        "Coding",
        "Code optimization assistant",
        Color(0xFFFF5722) // Orange
    ),
    AiService(
        "copilot",
        "Copilot",
        "https://copilot.microsoft.com/",
        "Productivity",
        "Microsoft 365 integration",
        Color(0xFF0078D4) // Blue
    ),
    AiService(
        "brave",
        "Brave",
        "https://search.brave.com/ask",
        "Search",
        "Privacy-focused searching",
        Color(0xFF198038) // Green
    ),
    AiService(
        "huggingface",
        "HuggingFace",
        "https://huggingface.co/chat",
        "Open Source",
        "Open model experimentation",
        Color(0xFFFFD600) // Yellow
    ),
    AiService(
        "meta",
        "Meta AI",
        "https://www.meta.ai/",
        "Social",
        "Social platform integration",
        Color(0xFF0081FB) // Blue
    ),
    AiService(
        "euria",
        "Euria",
        "https://euria.infomaniak.com/",
        "Privacy First",
        "Swiss AI assistant",
        Color(0xFF0D47A1) // Blue
    ),
    AiService(
        "zai",
        "Z.ai",
        "https://chat.z.ai/",
        "Conversational",
        "Social AI assistant",
        Color(0xFF8E24AA) // Purple
    ),
    AiService(
        "h2ogpte",
        "H2O GPTe",
        "https://h2ogpte.genai.h2o.ai/",
        "Data Science",
        "Enterprise AI platform",
        Color(0xFF00695C) // Teal
    ),
    AiService(
        "dola",
        "Dola",
        "https://www.dola.com/chat",
        "General",
        "AI conversation assistant",
        Color(0xFF009688) // Teal
    ),
    AiService(
        "khoj",
        "Khoj",
        "https://app.khoj.dev/",
        "Productivity",
        "Personal AI assistant",
        Color(0xFFFF9800) // Orange
    ),
)

val serviceIcons = mapOf(
    "chatgpt" to Icons.Rounded.FlashOn,
    "duck" to Icons.Rounded.PrivacyTip,
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
    "dola" to Icons.AutoMirrored.Rounded.Chat,
    "khoj" to Icons.Rounded.Psychology
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
        "imagine-public.x.ai",

        // Auth
        "accounts.x.ai", "auth.grokipedia.com"
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
        "www.meta.ai", "static.xx.fbcdn.net", "video.fdel64-1.fna.fbcdn.net", "graph.meta.ai"
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
    ),

    // Dola
    "dola" to listOf(
        "www.dola.com",
        "sf-flow-web-cdn.ciciaicdn.com",
        "opt-i18n.ciciai.com",
        "mcs-sg.ciciai.com",
        "sf-sf-flow-web-cdn-nontt.ciciaicdn.com",
        "p16-flow-sign-sg.ciciai.com",
        "vmweb-sg.ciciai.com",
        "mssdk-i18n-sg.ciciai.com",
        "sf-rc2.yhgfb-static.com",
        "sf-rc.yhgfb-static.com"
    ),

    // Khoj
    "khoj" to listOf(
        "app.khoj.dev", "khoj.dev", "khoj.auth0.com", "cdn.khoj.dev"
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
    // Google
    "accounts.google.com",

    // Apple
    "appleid.apple.com",
    "www.apple.com",
    "appleid.cdn-apple.com",

    // Microsoft
    "login.live.com",
    "login.microsoftonline.com",
    "aadcdn.msauth.net",
    "aadcdn.msftauth.net",

    // Github
    "github.githubassets.com",

    // Cloudflare
    "challenges.cloudflare.com",
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
