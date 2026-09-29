package dev.chungjungsoo.gptmobile.presentation.common

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

val BoardInk = Color(0xFF141414)
val BoardMuted = Color(0xFF616161)
val BoardLine = Color(0xFFD1D1D1)
val BoardSurface = Color(0xFFF7F7F7)

@Composable
fun BoardHeader(title: String, subtitle: String? = null, onBack: (() -> Unit)? = null, action: String? = null, onAction: (() -> Unit)? = null, secondaryAction: String? = null, onSecondaryAction: (() -> Unit)? = null) {
    Row(
        modifier = Modifier.fillMaxWidth().height(77.dp).background(Color.White).padding(horizontal = 18.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (onBack != null) {
            IconButton(onClick = onBack, modifier = Modifier.size(28.dp)) { Text("‹", color = BoardInk, style = MaterialTheme.typography.headlineMedium) }
            Spacer(Modifier.width(8.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(title, color = BoardInk, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            subtitle?.let { Text(it, color = BoardMuted, style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis) }
        }
        action?.let {
            IconButton(onClick = onAction ?: {}, modifier = Modifier.size(34.dp).background(BoardSurface, RoundedCornerShape(10.dp))) { Text(it, color = BoardInk) }
        }
        secondaryAction?.let {
            Spacer(Modifier.width(8.dp))
            IconButton(onClick = onSecondaryAction ?: {}, modifier = Modifier.size(34.dp).background(BoardSurface, RoundedCornerShape(10.dp))) { Text(it, color = BoardInk) }
        }
    }
    Box(Modifier.fillMaxWidth().height(1.dp).background(BoardLine))
}

@Composable
fun BoardSection(label: String, modifier: Modifier = Modifier) {
    Text(label.uppercase(), modifier = modifier.padding(top = 18.dp, bottom = 7.dp), color = BoardMuted, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold)
}

@Composable
fun BoardRow(title: String, subtitle: String? = null, icon: String = "", onClick: (() -> Unit)? = null, trailing: String = "›", modifier: Modifier = Modifier) {
    val rowModifier = modifier.fillMaxWidth().height(62.dp).then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
    Card(shape = RoundedCornerShape(12.dp), border = BorderStroke(1.dp, BoardLine), colors = CardDefaults.cardColors(containerColor = BoardSurface), modifier = rowModifier) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 11.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            if (icon.isNotBlank()) Box(Modifier.size(32.dp), contentAlignment = Alignment.Center) { Text(icon, color = BoardInk) }
            Column(Modifier.weight(1f)) {
                Text(title, color = BoardInk, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                subtitle?.let { Text(it, color = BoardMuted, style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis) }
            }
            if (trailing.isNotBlank()) Text(trailing, color = BoardMuted, style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
fun BoardPrimaryButton(label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Card(modifier = modifier.fillMaxWidth().height(44.dp).clickable(onClick = onClick), shape = RoundedCornerShape(10.dp), colors = CardDefaults.cardColors(containerColor = BoardInk)) {
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { Text(label, color = Color.White, fontWeight = FontWeight.SemiBold) }
    }
}
