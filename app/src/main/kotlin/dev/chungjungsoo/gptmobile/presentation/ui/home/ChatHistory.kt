package dev.chungjungsoo.gptmobile.presentation.ui.home

/** Presentation-only search and cap policy. Persistence ordering remains unchanged. */
internal fun <T> List<T>.matchingTitles(query: String, title: (T) -> String): List<T> {
    val normalized = query.trim()
    return if (normalized.isEmpty()) this else filter { title(it).contains(normalized, ignoreCase = true) }
}

internal fun <T> List<T>.homeHistory(): List<T> = take(HOME_HISTORY_LIMIT)

internal const val HOME_HISTORY_LIMIT = 5
