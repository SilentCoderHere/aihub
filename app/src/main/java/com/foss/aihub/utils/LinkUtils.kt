package com.foss.aihub.utils

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.core.net.toUri
import com.foss.aihub.R

fun extractLinkTitle(context: Context, url: String): String {
    if (url.isEmpty()) return context.getString(R.string.label_link)

    return try {
        url.substringAfterLast("/").substringBefore("?").substringBefore("#")
            .replace(Regex("[_-]"), " ").replace(Regex("\\.[a-zA-Z]{2,4}$"), "").trim()
            .takeIf { it.isNotEmpty() && it != "null" }?.split(" ")?.joinToString(" ") { word ->
                word.replaceFirstChar { it.uppercase() }
            } ?: context.getString(R.string.label_link)
    } catch (_: Exception) {
        context.getString(R.string.label_link)
    }
}

fun copyLinkToClipboard(context: Context, url: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = ClipData.newPlainText(context.getString(R.string.label_url), url)
    clipboard.setPrimaryClip(clip)
}

fun shareLink(context: Context, url: String, title: String) {
    val shareIntent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, url)
        putExtra(Intent.EXTRA_SUBJECT, title)
    }
    context.startActivity(
        Intent.createChooser(shareIntent, context.getString(R.string.action_share_link)).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        })
}

fun cleanTrackingParams(url: String): String {
    val trackingParams = listOf(
        "gclid", "fbclid", "msclkid", "ttclid", "twclid", "yclid",
        "igshid", "li_fat_id", "gbraid", "wbraid", "gad_source",
        "srsltid", "ndclid", "sccid", "dclid",
        "_ga", "_gl", "ef_id", "s_kwcid",
        "mc_cid", "mc_eid", "_bta_tid", "_bta_c",
        "trk_contact", "trk_msg", "_ke", "_kx", "dm_i", "mkt_tok",
        "ref", "affiliate_id", "click_id", "campid", "customid",
        "irclickid", "mkwid", "pcrid", "_branch_match_id",
        "gclsrc", "gdfms", "gdftrk", "epik", "pp", "si", "rtid", "vmcid",
        "_hsenc", "_hsmi", "__hssc", "__hstc",
    )
    return try {
        val uri = url.toUri()
        val builder = uri.buildUpon()
        builder.clearQuery()

        uri.queryParameterNames.forEach { param ->
            if (!param.startsWith(
                    "utm_", ignoreCase = true
                ) && !trackingParams.contains(param.lowercase())
            ) {
                val values = uri.getQueryParameters(param)
                values.forEach { value ->
                    builder.appendQueryParameter(param, value)
                }
            }
        }
        builder.build().toString()
    } catch (_: Exception) {
        url
    }
}

fun openInExternalBrowser(
    context: Context, url: String
) {
    try {
        val intent = Intent(Intent.ACTION_VIEW, url.toUri()).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    } catch (_: ActivityNotFoundException) {
        Toast.makeText(
            context,
            context.getString(R.string.msg_no_suitable_app_found),
            Toast.LENGTH_SHORT,
        ).show()
    } catch (_: Exception) {
        Toast.makeText(
            context,
            context.getString(R.string.error_generic_title),
            Toast.LENGTH_SHORT,
        ).show()
    }
}