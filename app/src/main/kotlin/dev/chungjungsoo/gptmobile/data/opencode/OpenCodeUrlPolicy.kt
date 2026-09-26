package dev.chungjungsoo.gptmobile.data.opencode

import java.net.URI

class OpenCodeUrlPolicy(private val allowHttpLoopback: Boolean) {

    fun canonicalize(value: String): String {
        val uri = try {
            URI(value.trim())
        } catch (_: Exception) {
            throw IllegalArgumentException("Invalid OpenCode URL")
        }
        val scheme = uri.scheme?.lowercase() ?: throw IllegalArgumentException("OpenCode URL needs a scheme")
        val host = uri.host?.lowercase() ?: throw IllegalArgumentException("OpenCode URL needs a host")
        require(uri.userInfo == null && uri.query == null && uri.fragment == null) { "OpenCode URL cannot contain credentials, query, or fragment" }
        require(scheme == "https" || (allowHttpLoopback && scheme == "http" && host in LOOPBACK_HOSTS)) {
            "OpenCode requires HTTPS"
        }
        val port = when {
            uri.port == -1 -> ""
            scheme == "https" && uri.port == 443 -> ""
            scheme == "http" && uri.port == 80 -> ""
            else -> ":${uri.port}"
        }
        val path = uri.path.orEmpty().trimEnd('/').let { if (it.isEmpty()) "" else it }
        return "$scheme://$host$port$path"
    }

    private companion object {
        val LOOPBACK_HOSTS = setOf("localhost", "127.0.0.1", "10.0.2.2")
    }
}
