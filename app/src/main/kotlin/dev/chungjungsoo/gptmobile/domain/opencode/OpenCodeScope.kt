package dev.chungjungsoo.gptmobile.domain.opencode

/** Directory is server-owned and case-sensitive; never normalize it on Android. */
data class OpenCodeScope(val serverId: String, val profileRevision: Long, val directory: String) {
    init {
        require(serverId.isNotBlank())
        require(profileRevision >= 0)
        require(directory.isNotBlank() && '\u0000' !in directory)
    }
}

data class OpenCodeSessionKey(val scope: OpenCodeScope, val sessionId: String) {
    init {
        require(sessionId.startsWith("ses") && sessionId.length > 3)
    }
}
