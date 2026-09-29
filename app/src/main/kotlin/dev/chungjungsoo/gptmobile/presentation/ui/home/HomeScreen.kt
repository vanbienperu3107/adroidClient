package dev.chungjungsoo.gptmobile.presentation.ui.home

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import dev.chungjungsoo.gptmobile.data.database.entity.ChatRoom
import dev.chungjungsoo.gptmobile.data.model.ApiType
import dev.chungjungsoo.gptmobile.data.opencode.CachedOpenCodeSession
import dev.chungjungsoo.gptmobile.presentation.common.BoardHeader
import dev.chungjungsoo.gptmobile.presentation.common.BoardInk
import dev.chungjungsoo.gptmobile.presentation.common.BoardLine
import dev.chungjungsoo.gptmobile.presentation.common.BoardRow
import dev.chungjungsoo.gptmobile.presentation.common.BoardSection
import dev.chungjungsoo.gptmobile.util.collectManagedState
import dev.chungjungsoo.gptmobile.util.getPlatformTitleResources

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(homeViewModel: HomeViewModel = hiltViewModel(), settingOnClick: () -> Unit, onExistingChatClick: (ChatRoom) -> Unit, navigateToNewChat: (List<ApiType>) -> Unit, onOpenCodeSession: (String, String, String) -> Unit, onOpenCodeSetup: () -> Unit) {
    val chats by homeViewModel.chatListState.collectManagedState()
    val platforms by homeViewModel.platformState.collectManagedState()
    val openCode by homeViewModel.openCodeState.collectManagedState()
    val lifecycle by androidx.lifecycle.compose.LocalLifecycleOwner.current.lifecycle.currentStateFlow.collectManagedState()
    var search by remember { mutableStateOf(false) }
    var choice by remember { mutableStateOf(false) }
    var all by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(lifecycle) {
        if (lifecycle == Lifecycle.State.RESUMED) {
            homeViewModel.fetchChats()
            homeViewModel.fetchPlatformStatus()
            homeViewModel.fetchOpenCodeSessions()
        }
    }
    LaunchedEffect(Unit) { homeViewModel.openCodeNavigation.collect { onOpenCodeSession(it.first, it.second, it.third) } }
    val direct = chats.chats
    if (search) {
        ChatSearchScreen(openCode.sessions, direct, { search = false }, { session -> onOpenCodeSession(openCode.serverId ?: return@ChatSearchScreen, openCode.directory ?: return@ChatSearchScreen, session.sessionId) }, onExistingChatClick)
        return
    }
    Column {
        BoardHeader("Chats", action = "⌕", onAction = { search = true })
        LazyColumn(Modifier.weight(1f).padding(horizontal = 18.dp)) {
            item {
                BoardRow("New chat", "OpenCode or Direct provider", "✎", { choice = true }, trailing = "Choose", modifier = Modifier.padding(top = 21.dp).testTag("new-chat"))
                BoardSection("OpenCode chats")
            }
            if (openCode.serverId == null) item { BoardRow("Connect OpenCode", "Add a server to see workspace chats", "▣", onOpenCodeSetup) }
            items(openCode.sessions.take(3), key = { it.sessionId }) { session ->
                OpenCodeChatRow(session) { onOpenCodeSession(openCode.serverId ?: return@OpenCodeChatRow, openCode.directory ?: return@OpenCodeChatRow, session.sessionId) }
            }
            item {
                if (openCode.sessions.size > 3) SeeAll { all = "OpenCode chats" }
                HorizontalDivider(Modifier.padding(top = 10.dp), color = BoardLine)
                BoardSection("Direct chats")
            }
            items(direct.take(3), key = { it.id }) { chat -> DirectChatRow(chat, false, false, getPlatformTitleResources(), {}, { onExistingChatClick(chat) }, {}) }
            item {
                if (direct.size > 3) SeeAll { all = "Direct chats" }
                BoardRow("How Chats works", "Tap an OpenCode chat to load its server history. Tap a Direct chat to load its direct history.", trailing = "", modifier = Modifier.padding(vertical = 20.dp))
            }
        }
    }
    if (choice) {
        NewChatChoiceDialog({ choice = false }, {
            choice = false
            if (openCode.serverId == null || openCode.directory == null) onOpenCodeSetup() else homeViewModel.createOpenCodeSession()
        }, {
            choice = false
            val enabled = platforms.filter { it.enabled }.map { it.name }
            if (enabled.isNotEmpty()) navigateToNewChat(enabled)
        })
    }
    all?.let { title -> AlertDialog(onDismissRequest = { all = null }, title = { Text(title) }, text = { Text("Full history is available through search.") }, confirmButton = { TextButton(onClick = { all = null }) { Text("Done") } }) }
}

@Composable private fun SeeAll(onClick: () -> Unit) = Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = androidx.compose.foundation.layout.Arrangement.End) { TextButton(onClick = onClick) { Text("See all", color = BoardInk) } }

@Composable fun ChatSearchScreen(openCode: List<CachedOpenCodeSession>, direct: List<ChatRoom>, onBack: () -> Unit, onOpen: (CachedOpenCodeSession) -> Unit, onDirect: (ChatRoom) -> Unit) {
    var query by remember { mutableStateOf("") }
    Column {
        BoardHeader("Search chats", onBack = onBack)
        androidx.compose.material3.OutlinedTextField(query, { query = it }, Modifier.fillMaxWidth().padding(horizontal = 18.dp).testTag("chat-search"), placeholder = { Text("Search chats") }, singleLine = true)
        LazyColumn(Modifier.padding(horizontal = 18.dp)) {
            val oc = openCode.filter { it.title.contains(query, true) }
            val dc = direct.filter { it.title.contains(query, true) }
            item { BoardSection("OpenCode chats") }
            items(oc, key = { it.sessionId }) { OpenCodeChatRow(it) { onOpen(it) } }
            item { BoardSection("Direct chats") }
            items(dc, key = { it.id }) { DirectChatRow(it, false, false, emptyMap(), {}, { onDirect(it) }, {}) }
            if (oc.isEmpty() && dc.isEmpty()) item { Text("No chats match \"$query\"", Modifier.padding(24.dp)) }
        }
    }
}

@Composable fun OpenCodeChatRow(session: CachedOpenCodeSession, onClick: () -> Unit) = BoardRow(session.title, "OpenCode chat · ${session.status}", "▣", onClick, modifier = Modifier.padding(vertical = 3.dp).testTag("opencode-chat-${session.sessionId}"))

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DirectChatRow(chatRoom: ChatRoom, isSelectionMode: Boolean, selected: Boolean, platformTitles: Map<ApiType, String>, onLongClick: () -> Unit, onClick: () -> Unit, onCheckedChange: () -> Unit) {
    val subtitle = chatRoom.enabledPlatform.joinToString(", ") { platformTitles[it].orEmpty() }.ifBlank { "Direct provider" }
    BoardRow(chatRoom.title, subtitle, "○", onClick, modifier = Modifier.padding(vertical = 3.dp).testTag("direct-chat-${chatRoom.id}").combinedClickable(onClick = onClick, onLongClick = onLongClick))
}

@Composable fun NewChatChoiceDialog(onDismiss: () -> Unit, onOpenCode: () -> Unit, onDirect: () -> Unit) = AlertDialog(onDismissRequest = onDismiss, title = { Text("New chat") }, text = {
    Column {
        TextButton(onClick = onOpenCode, modifier = Modifier.fillMaxWidth().testTag("new-chat-opencode")) {
            Text("OpenCode", Modifier.weight(1f))
            Text("Uses the server default project")
        }
        TextButton(onClick = onDirect, modifier = Modifier.fillMaxWidth().testTag("new-chat-direct")) {
            Text("Direct provider", Modifier.weight(1f))
            Text("Uses a configured provider")
        }
    }
}, confirmButton = { TextButton(onClick = onDismiss) { Text("Cancel") } })

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun HomeTopAppBar(isSelectionMode: Boolean, selectedChats: Int, searchOpen: Boolean, searchQuery: String, scrollBehavior: TopAppBarScrollBehavior, onSearchChange: (String) -> Unit, onSearchToggle: () -> Unit, actionOnClick: () -> Unit, navigationOnClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(18.dp), horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween) {
        Text(if (isSelectionMode) "$selectedChats selected" else "Chats", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        Row {
            TextButton(onClick = onSearchToggle, modifier = Modifier.semantics { contentDescription = "Search chats" }) { Text("⌕") }
            TextButton(onClick = actionOnClick, modifier = Modifier.semantics { contentDescription = "Settings" }) { Text("⚙") }
        }
    }
}
