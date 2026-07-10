package app.dramaverse.stream.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.foundation.clickable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import app.dramaverse.stream.data.AppNotification
import app.dramaverse.stream.model.NotificationViewModel

private val Background = Color(0xFF09090B)
private val Panel = Color(0xFF161519)
private val PanelRaised = Color(0xFF1D1A20)
private val Stroke = Color(0x24FFFFFF)
private val Gold = Color(0xFFF6C54F)
private val SoftPink = Color(0xFFFFB5C1)
private val Muted = Color(0xFFCDB5BC)
private val Dim = Color(0xFF897B83)

@Composable
fun NotificationScreen(
    backendBaseUrl: String,
    onBack: () -> Unit,
    onHome: () -> Unit,
    onShorts: () -> Unit,
    onLibrary: () -> Unit,
    onRewards: () -> Unit,
    viewModel: NotificationViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(backendBaseUrl) {
        viewModel.loadNotifications(backendBaseUrl)
    }

    Box(Modifier.fillMaxSize().background(Background)) {
        NotificationBackdrop()
        Column(Modifier.fillMaxSize()) {
            NotificationHeader(
                unreadCount = uiState.unreadCount,
                totalCount = uiState.items.size,
                isClearing = uiState.isClearing,
                onBack = onBack,
                onClearAll = { viewModel.clearAll(backendBaseUrl) }
            )
            if (uiState.isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = SoftPink)
                }
            } else if (uiState.items.isEmpty()) {
                EmptyNotifications(uiState.isClearing)
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 6.dp, bottom = 108.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item {
                        Text(
                            "Recent activity",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 0.sp,
                            modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)
                        )
                    }
                    items(uiState.items) { notification ->
                        NotificationRow(notification)
                    }
                }
            }
        }
        BottomNavigationBar(
            selected = "Notifications",
            onHome = onHome,
            onShorts = onShorts,
            onLibrary = onLibrary,
            onRewards = onRewards,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

@Composable
private fun NotificationBackdrop() {
    Box(
        Modifier
            .fillMaxWidth()
            .height(260.dp)
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF1A2038), Color(0xFF21141E), Color(0x9909090B), Color.Transparent)
                )
            )
    )
}

@Composable
private fun NotificationHeader(
    unreadCount: Int,
    totalCount: Int,
    isClearing: Boolean,
    onBack: () -> Unit,
    onClearAll: () -> Unit
) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(start = 18.dp, end = 18.dp, top = 28.dp, bottom = 14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(40.dp).clip(CircleShape).background(Color(0x9918171C)).border(1.dp, Stroke, CircleShape).clickable(onClick = onBack), contentAlignment = Alignment.Center) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = Color.White, modifier = Modifier.size(21.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("Notifications", color = Color.White, fontSize = 29.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.sp)
                Text(
                    if (totalCount == 0) "You are all caught up" else "$unreadCount unread • $totalCount total",
                    color = Muted,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.sp
                )
            }
            if (totalCount > 0) {
                ClearAllButton(isClearing = isClearing, onClearAll = onClearAll)
            }
        }
        Spacer(Modifier.height(18.dp))
        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Brush.linearGradient(listOf(Color(0xFF201A23), Color(0xFF151418))))
                .border(1.dp, Color(0x33F6C54F), RoundedCornerShape(16.dp))
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(Modifier.size(42.dp).clip(CircleShape).background(Color(0x22F6C54F)), contentAlignment = Alignment.Center) {
                Icon(Icons.Filled.Notifications, contentDescription = null, tint = Gold, modifier = Modifier.size(23.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("Inbox", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.sp)
                Text("Planner reminders and reward updates in one place.", color = Muted, fontSize = 12.sp, lineHeight = 17.sp, letterSpacing = 0.sp)
            }
            CountPill("$unreadCount new")
        }
    }
}

@Composable
private fun ClearAllButton(isClearing: Boolean, onClearAll: () -> Unit) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0x22FFB5C1))
            .border(1.dp, Color(0x33FFB5C1), RoundedCornerShape(14.dp))
            .clickable(enabled = !isClearing, onClick = onClearAll)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Filled.DeleteSweep, contentDescription = null, tint = SoftPink, modifier = Modifier.size(17.dp))
        Spacer(Modifier.width(5.dp))
        Text(if (isClearing) "Clearing" else "Clear", color = SoftPink, fontSize = 12.sp, fontWeight = FontWeight.Black, letterSpacing = 0.sp)
    }
}

@Composable
private fun CountPill(text: String) {
    Text(
        text,
        color = Gold,
        fontSize = 11.sp,
        fontWeight = FontWeight.Black,
        letterSpacing = 0.sp,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0x22F6C54F))
            .padding(horizontal = 9.dp, vertical = 6.dp)
    )
}

@Composable
private fun TypeChip(type: String) {
    Text(
        when (type) {
            "planner" -> "Planner"
            "reward" -> "Reward"
            else -> "Update"
        },
        color = if (type == "reward") Gold else SoftPink,
        fontSize = 10.sp,
        fontWeight = FontWeight.Black,
        maxLines = 1,
        letterSpacing = 0.sp,
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (type == "reward") Color(0x1FF6C54F) else Color(0x22FFB5C1))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    )
}

private fun notificationIcon(type: String) = when (type) {
    "planner" -> Icons.Filled.CalendarMonth
    "reward" -> Icons.Filled.Star
    else -> Icons.Filled.Widgets
}

private fun notificationAccent(type: String): Color = if (type == "reward") Gold else SoftPink

private fun compactNotificationTime(createdAt: String): String {
    if (createdAt.isBlank()) return "Now"
    return createdAt.substringBefore(".").replace("T", " ").takeLast(16).ifBlank { "Now" }
}

@Composable
private fun UnreadDot(read: Boolean) {
    if (!read) {
        Box(Modifier.size(8.dp).clip(CircleShape).background(Gold))
    }
}

@Composable
private fun NotificationRow(notification: AppNotification) {
    val accent = notificationAccent(notification.type)
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(if (!notification.read) PanelRaised else Panel)
            .border(1.dp, if (!notification.read) Color(0x55F6C54F) else Stroke, RoundedCornerShape(16.dp))
            .padding(14.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            Modifier
                .size(46.dp)
                .clip(CircleShape)
                .background(if (notification.type == "reward") Color(0x26F6C54F) else Color(0x26FFB5C1)),
            contentAlignment = Alignment.Center
        ) {
            Icon(notificationIcon(notification.type), contentDescription = null, tint = accent, modifier = Modifier.size(23.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    notification.title,
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    letterSpacing = 0.sp,
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(8.dp))
                UnreadDot(notification.read)
            }
            Spacer(Modifier.height(5.dp))
            Text(
                notification.body.ifBlank { "Open DramaVerse for the latest update." },
                color = Muted,
                fontSize = 12.sp,
                lineHeight = 17.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                letterSpacing = 0.sp
            )
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                TypeChip(notification.type)
                Spacer(Modifier.width(8.dp))
                Text(compactNotificationTime(notification.createdAt), color = Dim, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.sp)
            }
        }
    }
}

@Composable
private fun EmptyNotifications(isClearing: Boolean) {
    Box(Modifier.fillMaxSize().padding(horizontal = 34.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.size(76.dp).clip(CircleShape).background(Color(0x22FFB5C1)).border(1.dp, Color(0x33FFB5C1), CircleShape), contentAlignment = Alignment.Center) {
                Icon(Icons.Filled.Notifications, contentDescription = null, tint = SoftPink, modifier = Modifier.size(38.dp))
            }
            Spacer(Modifier.height(16.dp))
            Text(if (isClearing) "Clearing inbox" else "No notifications yet", color = Color.White, fontSize = 21.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.sp)
            Spacer(Modifier.height(6.dp))
            Text(
                if (isClearing) "Removing old updates..." else "Planner reminders and reward updates will appear here.",
                color = Muted,
                fontSize = 13.sp,
                lineHeight = 19.sp,
                letterSpacing = 0.sp
            )
        }
    }
}
