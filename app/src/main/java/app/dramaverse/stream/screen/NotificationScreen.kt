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
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Star
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
private val Gold = Color(0xFFF6C54F)
private val SoftPink = Color(0xFFFFB5C1)

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
        Column(Modifier.fillMaxSize()) {
            NotificationHeader(uiState.unreadCount, onBack)
            if (uiState.isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = SoftPink)
                }
            } else if (uiState.items.isEmpty()) {
                EmptyNotifications()
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 18.dp, end = 18.dp, bottom = 104.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
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
private fun NotificationHeader(unreadCount: Int, onBack: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .height(112.dp)
            .background(Brush.verticalGradient(listOf(Color(0xFF050506), Color(0xEE09090B), Color.Transparent)))
            .padding(start = 18.dp, end = 18.dp, top = 28.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(40.dp).clip(CircleShape).background(Color(0x6618171C)).clickable(onClick = onBack), contentAlignment = Alignment.Center) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = Color.White, modifier = Modifier.size(21.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text("Notifications", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.sp)
            Text("$unreadCount unread updates", color = Color(0xFFCDB5BC), fontSize = 13.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.sp)
        }
    }
}

@Composable
private fun NotificationRow(notification: AppNotification) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(15.dp))
            .background(Panel)
            .border(1.dp, if (!notification.read) Color(0x55F6C54F) else Color(0x22FFFFFF), RoundedCornerShape(15.dp))
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(44.dp).clip(CircleShape).background(if (notification.type == "reward") Color(0x33F6C54F) else Color(0x33FFB5C1)), contentAlignment = Alignment.Center) {
            Icon(
                if (notification.type == "planner") Icons.Filled.CalendarMonth else if (notification.type == "reward") Icons.Filled.Star else Icons.Filled.Notifications,
                contentDescription = null,
                tint = if (notification.type == "reward") Gold else SoftPink,
                modifier = Modifier.size(23.dp)
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(notification.title, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1, overflow = TextOverflow.Ellipsis, letterSpacing = 0.sp)
            Text(notification.body, color = Color(0xFFCDB5BC), fontSize = 12.sp, lineHeight = 16.sp, maxLines = 2, overflow = TextOverflow.Ellipsis, letterSpacing = 0.sp)
        }
    }
}

@Composable
private fun EmptyNotifications() {
    Box(Modifier.fillMaxSize().padding(horizontal = 34.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Filled.Notifications, contentDescription = null, tint = SoftPink, modifier = Modifier.size(52.dp))
            Spacer(Modifier.height(12.dp))
            Text("No notifications yet", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.sp)
            Text("Planner reminders and reward updates will appear here.", color = Color(0xFFCDB5BC), fontSize = 13.sp, lineHeight = 18.sp, letterSpacing = 0.sp)
        }
    }
}
