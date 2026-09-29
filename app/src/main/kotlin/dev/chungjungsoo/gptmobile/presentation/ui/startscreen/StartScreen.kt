package dev.chungjungsoo.gptmobile.presentation.ui.startscreen

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.chungjungsoo.gptmobile.presentation.common.BoardInk
import dev.chungjungsoo.gptmobile.presentation.common.BoardPrimaryButton

@Composable
fun StartScreen(onStartClick: () -> Unit, onOpenCodeClick: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(28.dp), horizontalAlignment = Alignment.Start) {
        Spacer(Modifier.weight(0.35f))
        Card(Modifier.size(64.dp), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = BoardInk)) { androidx.compose.foundation.layout.Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("O", color = Color.White, style = androidx.compose.material3.MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold) } }
        Text("Connect OpenCode\nto your workspace", Modifier.padding(top = 28.dp), style = androidx.compose.material3.MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.SemiBold)
        Text("Read server-backed chat history, use tools, and keep Basic credentials encrypted on this device.", Modifier.padding(top = 14.dp), color = Color(0xFF616161))
        Spacer(Modifier.weight(0.65f))
        BoardPrimaryButton("Connect OpenCode", onOpenCodeClick)
        androidx.compose.material3.TextButton(onClick = onStartClick, modifier = Modifier.padding(top = 8.dp)) { Text("Set up direct providers", color = BoardInk) }
    }
}

@Composable fun StartScreenLogo(modifier: Modifier = Modifier) {}

@Composable fun WelcomeText(modifier: Modifier = Modifier) {}
