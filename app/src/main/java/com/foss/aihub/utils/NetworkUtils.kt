package com.foss.aihub.utils

import io.ktor.util.network.UnresolvedAddressException
import java.net.ConnectException
import java.net.SocketException
import java.net.UnknownHostException


fun Throwable.isNoNetworkError(): Boolean {
    val root = rootCause()

    return root is UnresolvedAddressException || root is UnknownHostException || root is ConnectException || (root is SocketException && root.message?.contains(
        "unreachable", ignoreCase = true
    ) == true) || root.message?.contains(
        "Network is unreachable", ignoreCase = true
    ) == true || root.message?.contains("unresolved address", ignoreCase = true) == true
}

private fun Throwable.rootCause(): Throwable {
    var cause = this
    while (cause.cause != null && cause.cause !== cause) {
        cause = cause.cause!!
    }
    return cause
}