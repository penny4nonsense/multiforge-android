package com.example.multiforge_android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.multiforge_android.ui.theme.MultiforgeandroidTheme
import kotlinx.coroutines.launch
import androidx.core.view.WindowCompat

val DarkBg = Color(0xFF0A0A0F)
val SidebarBg = Color(0xFF111118)
val BubbleBg = Color(0xFF1E1E2E)
val UserBubble = Color(0xFF4F46E5)
val AccentColor = Color(0xFF818CF8)
val ProposalBg = Color(0xFF1A1500)
val ProposalBorder = Color(0xFF92400E)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MultiforgeandroidTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = DarkBg
                ) {
                    MultiforgeApp()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MultiforgeApp(vm: MainViewModel = viewModel()) {
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier.width(280.dp),
                drawerContainerColor = SidebarBg
            ) {
                Sidebar(vm = vm, onClose = { scope.launch { drawerState.close() } })
            }
        }
    ) {
        Scaffold(
            containerColor = DarkBg,
            modifier = Modifier.safeDrawingPadding(),
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            "Multiforge",
                            color = AccentColor,
                            fontFamily = FontFamily.Monospace
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, contentDescription = "Menu", tint = AccentColor)
                        }
                    },
                    colors = TopAppBarDefaults.mediumTopAppBarColors(containerColor = SidebarBg)
                )
            }
        ) { padding ->
            ChatScreen(vm = vm, modifier = Modifier.padding(padding))
        }
    }
}

@Composable
fun Sidebar(vm: MainViewModel, onClose: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize()) {
        // User switcher
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("jason", "shelley").forEach { u ->
                Button(
                    onClick = { vm.setUser(u); onClose() },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (vm.user.value == u) AccentColor else Color(0xFF2D2D3D)
                    )
                ) {
                    Text(u.replaceFirstChar { it.uppercase() }, fontSize = 12.sp)
                }
            }
        }

        Button(
            onClick = { vm.newConversation(); onClose() },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = AccentColor)
        ) {
            Text("+ New conversation", fontFamily = FontFamily.Monospace, fontSize = 13.sp)
        }

        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn {
            itemsIndexed(vm.conversations) { _, conv ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { vm.loadConversation(conv.id); onClose() }
                        .background(
                            if (conv.id == vm.activeConversationId.value)
                                Color(0xFF1E1E2E) else Color.Transparent
                        )
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = conv.title ?: "Untitled",
                        modifier = Modifier.weight(1f),
                        color = if (conv.id == vm.activeConversationId.value)
                            AccentColor else Color(0xFF9CA3AF),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.sp,
                        maxLines = 1
                    )
                    IconButton(
                        onClick = { vm.deleteConversation(conv.id) },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = Color(0xFF6B7280),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(vm: MainViewModel, modifier: Modifier = Modifier) {
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    LaunchedEffect(vm.messages.size) {
        if (vm.messages.isNotEmpty()) {
            listState.animateScrollToItem(vm.messages.size - 1)
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f).padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(vertical = 12.dp)
        ) {
            itemsIndexed(vm.messages) { index, msg ->
                MessageBubble(
                    msg = msg,
                    index = index,
                    onConfirm = { proposal -> vm.confirmProposal(proposal, index) },
                    onDismiss = { proposal -> vm.dismissProposal(proposal, index) }
                )
            }
            if (vm.loading.value) {
                item {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            "Thinking...",
                            modifier = Modifier
                                .background(BubbleBg, RoundedCornerShape(12.dp))
                                .padding(12.dp),
                            color = Color(0xFF9CA3AF),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }

        // Input bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(SidebarBg)
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = vm.input.value,
                onValueChange = { vm.input.value = it },
                modifier = Modifier.weight(1f),
                placeholder = {
                    Text(
                        "Message Multiforge...",
                        color = Color(0xFF6B7280),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.sp
                    )
                },
                textStyle = LocalTextStyle.current.copy(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 13.sp
                ),
                shape = RoundedCornerShape(12.dp)
            )
            Button(
                onClick = { vm.sendMessage() },
                enabled = !vm.loading.value,
                colors = ButtonDefaults.buttonColors(containerColor = AccentColor)
            ) {
                Text("Send", fontFamily = FontFamily.Monospace)
            }
        }
    }
}

@Composable
fun MessageBubble(
    msg: UiMessage,
    index: Int,
    onConfirm: (MemoryProposal) -> Unit,
    onDismiss: (MemoryProposal) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (msg.role == "user") Alignment.End else Alignment.Start
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 300.dp)
                .background(
                    if (msg.role == "user") UserBubble else BubbleBg,
                    RoundedCornerShape(12.dp)
                )
                .padding(12.dp)
        ) {
            Text(
                text = msg.content,
                color = Color.White,
                fontFamily = FontFamily.Monospace,
                fontSize = 13.sp,
                lineHeight = 20.sp
            )
        }

        // Memory proposals
        msg.proposals.forEach { proposal ->
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(ProposalBg, RoundedCornerShape(8.dp))
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    "[${proposal.type}/${proposal.category}]",
                    color = Color(0xFFFBBF24),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp
                )
                Text(
                    proposal.content,
                    modifier = Modifier.weight(1f),
                    color = Color(0xFFFEF3C7),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp
                )
                TextButton(onClick = { onConfirm(proposal) }) {
                    Text("✓", color = Color(0xFF34D399))
                }
                TextButton(onClick = { onDismiss(proposal) }) {
                    Text("✗", color = Color(0xFFF87171))
                }
            }
        }
    }
}