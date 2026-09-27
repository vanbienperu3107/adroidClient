package dev.chungjungsoo.gptmobile.data.model

/** The destination selected for the Chats screen's primary New chat action. */
enum class ChatStartDestination {
    OPEN_CODE,
    DIRECT;

    companion object {
        fun fromStored(value: String?): ChatStartDestination = entries.firstOrNull { it.name == value } ?: OPEN_CODE
    }
}
