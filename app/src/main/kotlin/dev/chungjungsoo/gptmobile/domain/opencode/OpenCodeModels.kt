package dev.chungjungsoo.gptmobile.domain.opencode

enum class OpenCodeAuthMode {
    BASIC
}

sealed interface OpenCodeCredential {
    data class Basic(val username: String, val password: String) : OpenCodeCredential
}

data class OpenCodeServerProfile(
    val serverId: String,
    val displayName: String,
    val baseUrl: String,
    val authMode: OpenCodeAuthMode,
    val credentialRef: String,
    val profileRevision: Long,
    val lastKnownVersion: String? = null,
    val lastHealthCheckAt: Long? = null
)

sealed interface OpenCodeConnectionState {
    data object NotChecked : OpenCodeConnectionState
    data class Connected(val version: String) : OpenCodeConnectionState
    data object Unhealthy : OpenCodeConnectionState
    data object Unauthorized : OpenCodeConnectionState
    data object Forbidden : OpenCodeConnectionState
    data object NotFound : OpenCodeConnectionState
    data object ServerError : OpenCodeConnectionState
    data object Timeout : OpenCodeConnectionState
    data object Unreachable : OpenCodeConnectionState
    data object TlsFailure : OpenCodeConnectionState
    data object Incompatible : OpenCodeConnectionState
    data object ReauthenticationRequired : OpenCodeConnectionState
    data object VaultUnavailable : OpenCodeConnectionState
}
