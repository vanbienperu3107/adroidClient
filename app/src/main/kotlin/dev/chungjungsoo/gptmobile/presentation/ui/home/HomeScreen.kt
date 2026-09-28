package dev.chungjungsoo.gptmobile.presentation.ui.home

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import dev.chungjungsoo.gptmobile.R
import dev.chungjungsoo.gptmobile.data.database.entity.ChatRoom
import dev.chungjungsoo.gptmobile.data.dto.Platform
import dev.chungjungsoo.gptmobile.data.model.ApiType
import dev.chungjungsoo.gptmobile.presentation.common.PlatformCheckBoxItem
import dev.chungjungsoo.gptmobile.util.collectManagedState
import dev.chungjungsoo.gptmobile.util.getPlatformTitleResources

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    homeViewModel: HomeViewModel = hiltViewModel(),
    settingOnClick: () -> Unit,
    onExistingChatClick: (ChatRoom) -> Unit,
    navigateToNewChat: (enabledPlatforms: List<ApiType>) -> Unit,
    onOpenCodeSession: (serverId: String, directory: String, sessionId: String) -> Unit,
    onOpenCodeSetup: () -> Unit
) {
    val platformTitles = getPlatformTitleResources()
    val chatListState by homeViewModel.chatListState.collectManagedState()
    val showSelectModelDialog by homeViewModel.showSelectModelDialog.collectManagedState()
    val showDeleteWarningDialog by homeViewModel.showDeleteWarningDialog.collectManagedState()
    val platformState by homeViewModel.platformState.collectManagedState()
    val openCodeState by homeViewModel.openCodeState.collectManagedState()
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    val lifecycleState by lifecycleOwner.lifecycle.currentStateFlow.collectManagedState()
    val context = androidx.compose.ui.platform.LocalContext.current
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    var searchOpen by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var showAllHistory by remember { mutableStateOf<String?>(null) }
    val directChats = chatListState.chats.matchingTitles(searchQuery) { it.title }
    val openCodeSessions = openCodeState.sessions.matchingTitles(searchQuery) { it.title }

    LaunchedEffect(lifecycleState) {
        if (lifecycleState == Lifecycle.State.RESUMED && !chatListState.isSelectionMode) {
            homeViewModel.fetchChats()
            homeViewModel.fetchPlatformStatus()
            homeViewModel.fetchOpenCodeSessions()
        }
    }
    LaunchedEffect(Unit) {
        homeViewModel.openCodeNavigation.collect { (server, directory, session) ->
            onOpenCodeSession(server, directory, session)
        }
    }
    BackHandler(enabled = chatListState.isSelectionMode) { homeViewModel.disableSelectionMode() }

    fun startDirectChat() {
        val enabledApiTypes = platformState.filter { it.enabled }.map { it.name }
        if (enabledApiTypes.size == 1) navigateToNewChat(enabledApiTypes) else homeViewModel.openSelectModelDialog()
    }
    var showNewChatChoice by remember { mutableStateOf(false) }

    fun startOpenCodeChat() {
        if (openCodeState.serverId != null && openCodeState.directory != null) {
            homeViewModel.createOpenCodeSession()
        } else {
            onOpenCodeSetup()
        }
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            HomeTopAppBar(
                isSelectionMode = chatListState.isSelectionMode,
                selectedChats = chatListState.selected.count { it },
                searchOpen = searchOpen,
                searchQuery = searchQuery,
                scrollBehavior = scrollBehavior,
                onSearchChange = { searchQuery = it },
                onSearchToggle = {
                    searchOpen = !searchOpen
                    if (!searchOpen) searchQuery = ""
                },
                actionOnClick = {
                    if (chatListState.isSelectionMode) homeViewModel.openDeleteWarningDialog() else settingOnClick()
                },
                navigationOnClick = homeViewModel::disableSelectionMode
            )
        }
    ) { innerPadding ->
        LazyColumn(Modifier.padding(innerPadding)) {
            item { ChatsTitle(scrollBehavior) }
            item {
                TextButton(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    onClick = { showNewChatChoice = true }
                ) {
                    Icon(Icons.Outlined.Edit, contentDescription = null)
                    Text("  New chat", modifier = Modifier.weight(1f))
                    Text("Choose", style = MaterialTheme.typography.labelMedium)
                }
            }
            item { SectionHeader("OpenCode chats", openCodeSessions.size) { showAllHistory = "OpenCode chats" } }
            if (openCodeState.serverId == null) {
                item { TextButton(onClick = onOpenCodeSetup, modifier = Modifier.padding(horizontal = 16.dp)) { Text("Connect OpenCode to see workspace sessions") } }
            } else {
                openCodeState.error?.let { error -> item { Text(error, modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)) } }
                items(openCodeSessions.homeHistory(), key = { it.sessionId }) { session ->
                    ListItem(
                        modifier = Modifier.fillMaxWidth().combinedClickable(onClick = {
                            val directory = openCodeState.directory ?: return@combinedClickable
                            val server = openCodeState.serverId ?: return@combinedClickable
                            onOpenCodeSession(server, directory, session.sessionId)
                        }).padding(horizontal = 8.dp),
                        headlineContent = { Text(session.title) },
                        supportingContent = { Text("OpenCode chat · ${session.status}") },
                        leadingContent = { Icon(Icons.Filled.Add, contentDescription = "OpenCode chat") }
                    )
                }
                if (openCodeState.stale) item { Text("Showing cached OpenCode sessions", modifier = Modifier.padding(horizontal = 24.dp)) }
            }
            item { HorizontalDivider(Modifier.padding(horizontal = 24.dp, vertical = 16.dp)) }
            item { SectionHeader("Direct provider chats", directChats.size) { showAllHistory = "Direct provider chats" } }
            items(directChats.homeHistory(), key = { it.id }) { chatRoom ->
                val sourceIndex = chatListState.chats.indexOfFirst { it.id == chatRoom.id }
                DirectChatRow(
                    chatRoom = chatRoom,
                    isSelectionMode = chatListState.isSelectionMode,
                    selected = chatListState.selected.getOrElse(sourceIndex) { false },
                    platformTitles = platformTitles,
                    onLongClick = {
                        homeViewModel.enableSelectionMode()
                        homeViewModel.selectChat(sourceIndex)
                    },
                    onClick = {
                        if (chatListState.isSelectionMode) homeViewModel.selectChat(sourceIndex) else onExistingChatClick(chatRoom)
                    },
                    onCheckedChange = { homeViewModel.selectChat(sourceIndex) }
                )
            }
            if (searchOpen && openCodeSessions.isEmpty() && directChats.isEmpty()) {
                item { Text("No chats match \"$searchQuery\"", modifier = Modifier.padding(24.dp), color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
        }

        if (showSelectModelDialog) {
            SelectPlatformDialog(
                platformState,
                onDismissRequest = homeViewModel::closeSelectModelDialog,
                onConfirmation = {
                    homeViewModel.closeSelectModelDialog()
                    navigateToNewChat(it)
                },
                onPlatformSelect = homeViewModel::updateCheckedState
            )
        }
        if (showDeleteWarningDialog) {
            DeleteWarningDialog(
                onDismissRequest = homeViewModel::closeDeleteWarningDialog,
                onConfirm = {
                    val count = chatListState.selected.count { it }
                    homeViewModel.deleteSelectedChats()
                    Toast.makeText(context, context.getString(R.string.deleted_chats, count), Toast.LENGTH_SHORT).show()
                    homeViewModel.closeDeleteWarningDialog()
                }
            )
        }
        showAllHistory?.let { title ->
            val openCode = title == "OpenCode chats"
            AlertDialog(
                onDismissRequest = { showAllHistory = null },
                title = { Text(title) },
                text = {
                    LazyColumn {
                        if (openCode) {
                            items(openCodeSessions, key = { it.sessionId }) { session ->
                                TextButton(onClick = {
                                    val directory = openCodeState.directory ?: return@TextButton
                                    val server = openCodeState.serverId ?: return@TextButton
                                    showAllHistory = null
                                    onOpenCodeSession(server, directory, session.sessionId)
                                }) { Text(session.title) }
                            }
                        } else {
                            items(directChats, key = { it.id }) { chat ->
                                TextButton(onClick = {
                                    showAllHistory = null
                                    onExistingChatClick(chat)
                                }) { Text(chat.title) }
                            }
                        }
                    }
                },
                confirmButton = { TextButton(onClick = { showAllHistory = null }) { Text("Done") } }
            )
        }
        if (showNewChatChoice) {
            AlertDialog(
                onDismissRequest = { showNewChatChoice = false },
                title = { Text("New chat") },
                text = {
                    Column {
                        TextButton(onClick = {
                            showNewChatChoice = false
                            startOpenCodeChat()
                        }, modifier = Modifier.fillMaxWidth()) {
                            Text("OpenCode", modifier = Modifier.weight(1f))
                            Text("Uses the server default project")
                        }
                        TextButton(onClick = {
                            showNewChatChoice = false
                            startDirectChat()
                        }, modifier = Modifier.fillMaxWidth()) {
                            Text("Direct provider", modifier = Modifier.weight(1f))
                            Text("Uses a configured provider")
                        }
                    }
                },
                confirmButton = { TextButton(onClick = { showNewChatChoice = false }) { Text("Cancel") } }
            )
        }
    }
}

@Composable
private fun SectionHeader(title: String, total: Int, onShowAll: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(start = 24.dp, end = 16.dp, top = 12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        if (total > HOME_HISTORY_LIMIT) TextButton(onClick = onShowAll) { Text("See all") }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DirectChatRow(
    chatRoom: ChatRoom,
    isSelectionMode: Boolean,
    selected: Boolean,
    platformTitles: Map<ApiType, String>,
    onLongClick: () -> Unit,
    onClick: () -> Unit,
    onCheckedChange: () -> Unit
) {
    val usingPlatform = chatRoom.enabledPlatform.joinToString(", ") { platformTitles[it].orEmpty() }
    ListItem(
        modifier = Modifier.fillMaxWidth().combinedClickable(onLongClick = onLongClick, onClick = onClick).padding(horizontal = 8.dp),
        headlineContent = { Text(chatRoom.title) },
        supportingContent = { Text(if (usingPlatform.isBlank()) "Direct provider" else "Direct provider · $usingPlatform") },
        leadingContent = {
            if (isSelectionMode) {
                Checkbox(checked = selected, onCheckedChange = { onCheckedChange() })
            } else {
                Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.chat_icon))
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeTopAppBar(
    isSelectionMode: Boolean,
    selectedChats: Int,
    searchOpen: Boolean,
    searchQuery: String,
    scrollBehavior: TopAppBarScrollBehavior,
    onSearchChange: (String) -> Unit,
    onSearchToggle: () -> Unit,
    actionOnClick: () -> Unit,
    navigationOnClick: () -> Unit
) {
    TopAppBar(
        colors = TopAppBarDefaults.topAppBarColors(
            scrolledContainerColor = if (isSelectionMode) MaterialTheme.colorScheme.primaryContainer else Color.Unspecified,
            containerColor = if (isSelectionMode) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.background
        ),
        title = {
            if (searchOpen && !isSelectionMode) {
                OutlinedTextField(searchQuery, onSearchChange, singleLine = true, label = { Text("Search chats") })
            } else {
                Text(if (isSelectionMode) stringResource(R.string.chats_selected, selectedChats) else stringResource(R.string.chats), maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        },
        navigationIcon = {
            if (isSelectionMode) IconButton(onClick = navigationOnClick) { Icon(Icons.Rounded.Close, contentDescription = stringResource(R.string.close)) }
        },
        actions = {
            if (isSelectionMode) {
                IconButton(onClick = actionOnClick) { Icon(Icons.Outlined.Delete, contentDescription = stringResource(R.string.delete)) }
            } else {
                IconButton(onClick = onSearchToggle) { Icon(if (searchOpen) Icons.Rounded.Close else Icons.Filled.Search, contentDescription = "Search chats") }
                IconButton(onClick = actionOnClick) { Icon(Icons.Outlined.Settings, contentDescription = stringResource(R.string.settings)) }
            }
        },
        scrollBehavior = scrollBehavior
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChatsTitle(scrollBehavior: TopAppBarScrollBehavior) {
    Text(
        modifier = Modifier.padding(top = 32.dp).padding(horizontal = 20.dp, vertical = 16.dp),
        text = stringResource(R.string.chats),
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 1f - scrollBehavior.state.overlappedFraction),
        style = MaterialTheme.typography.headlineLarge
    )
}

@Composable
private fun SelectPlatformDialog(
    platforms: List<Platform>,
    onDismissRequest: () -> Unit,
    onConfirmation: (List<ApiType>) -> Unit,
    onPlatformSelect: (Platform) -> Unit
) {
    AlertDialog(
        properties = DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier.widthIn(max = 360.dp),
        onDismissRequest = onDismissRequest,
        title = { Text(stringResource(R.string.select_platform)) },
        text = {
            Column {
                Text(stringResource(R.string.select_platform_description))
                val enabledPlatforms = platforms.filter { it.enabled }
                if (enabledPlatforms.isEmpty()) {
                    Text(
                        stringResource(R.string.enable_at_leat_one_platform),
                        modifier = Modifier.padding(vertical = 16.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    enabledPlatforms.forEach { platform ->
                        PlatformCheckBoxItem(
                            platform = platform,
                            title = getPlatformTitleResources()[platform.name]!!,
                            description = null,
                            onClickEvent = onPlatformSelect
                        )
                    }
                }
            }
        },
        confirmButton = { TextButton(enabled = platforms.any { it.selected }, onClick = { onConfirmation(platforms.filter { it.selected }.map { it.name }) }) { Text(stringResource(R.string.confirm)) } },
        dismissButton = { TextButton(onClick = onDismissRequest) { Text(stringResource(R.string.cancel)) } }
    )
}

@Composable
private fun DeleteWarningDialog(onDismissRequest: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text(stringResource(R.string.delete_selected_chats)) },
        text = { Text(stringResource(R.string.this_operation_can_t_be_undone)) },
        confirmButton = { TextButton(onClick = onConfirm) { Text(stringResource(R.string.confirm)) } },
        dismissButton = { TextButton(onClick = onDismissRequest) { Text(stringResource(R.string.cancel)) } }
    )
}
