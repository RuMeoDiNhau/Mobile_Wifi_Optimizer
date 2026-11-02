package com.mobilelegend.wlo.presentation.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.PersonOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mobilelegend.wlo.presentation.navigation.Screen
import com.mobilelegend.wlo.presentation.theme.CardBorder
import com.mobilelegend.wlo.presentation.theme.DividerLight
import com.mobilelegend.wlo.presentation.theme.NotificationDotOrange
import com.mobilelegend.wlo.presentation.theme.PrimaryGreen
import com.mobilelegend.wlo.presentation.theme.SurfaceWhite
import com.mobilelegend.wlo.presentation.theme.TextMuted
import com.mobilelegend.wlo.presentation.theme.TextPrimary

@Composable
fun AppTopHeader(
    overline: String,
    title: String,
    modifier: Modifier = Modifier,
    onNotificationClick: () -> Unit = {}
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = overline.uppercase(),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                color = PrimaryGreen
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = title,
                fontSize = 26.sp,
                fontWeight = FontWeight.ExtraBold,
                color = TextPrimary,
                lineHeight = 32.sp
            )
        }

        Box(
            modifier = Modifier
                .size(46.dp)
                .shadow(
                    elevation = 6.dp,
                    shape = RoundedCornerShape(14.dp),
                    ambientColor = Color.Black.copy(alpha = 0.06f),
                    spotColor = Color.Black.copy(alpha = 0.08f)
                )
                .clip(RoundedCornerShape(14.dp))
                .background(SurfaceWhite)
                .border(1.dp, CardBorder, RoundedCornerShape(14.dp))
                .clickable { onNotificationClick() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.Notifications,
                contentDescription = "Thông báo",
                tint = TextPrimary,
                modifier = Modifier.size(22.dp)
            )
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = (-10).dp, y = 10.dp)
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(NotificationDotOrange)
            )
        }
    }
}

/**
 * Icon đường sóng đo mạng (ECG / Pulse Waveform) đồng bộ với ảnh mẫu.
 */
@Composable
fun PulseWaveIcon(
    color: Color,
    modifier: Modifier = Modifier,
    strokeWidthDp: Dp = 2.dp
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val path = Path().apply {
            moveTo(w * 0.10f, h * 0.52f)
            lineTo(w * 0.30f, h * 0.52f)
            lineTo(w * 0.43f, h * 0.18f)
            lineTo(w * 0.57f, h * 0.84f)
            lineTo(w * 0.70f, h * 0.52f)
            lineTo(w * 0.90f, h * 0.52f)
        }
        drawPath(
            path = path,
            color = color,
            style = Stroke(
                width = strokeWidthDp.toPx(),
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
            )
        )
    }
}

private data class BottomNavTab(
    val screen: Screen,
    val label: String,
    val icon: ImageVector?,
    val isPulseWave: Boolean = false
)

@Composable
fun WloBottomNavigationBar(
    currentRoute: String,
    onNavigate: (Screen) -> Unit,
    modifier: Modifier = Modifier
) {
    val tabs = remember {
        listOf(
            BottomNavTab(Screen.Home, "Trang chủ", Icons.Outlined.Home),
            BottomNavTab(Screen.WifiMap, "Bản đồ", Icons.Outlined.Map),
            BottomNavTab(Screen.NetworkMeasure, "Đo mạng", null, isPulseWave = true),
            BottomNavTab(Screen.SavedPlaces, "Đã lưu", Icons.Outlined.BookmarkBorder),
            BottomNavTab(Screen.Profile, "Cá nhân", Icons.Outlined.PersonOutline)
        )
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = SurfaceWhite,
        shadowElevation = 8.dp
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(DividerLight)
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                tabs.forEach { tab ->
                    val isSelected = currentRoute == tab.screen.route
                    val itemColor = if (isSelected) PrimaryGreen else TextMuted
                    val interactionSource = remember { MutableInteractionSource() }

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clickable(
                                interactionSource = interactionSource,
                                indication = null
                            ) {
                                onNavigate(tab.screen)
                            }
                            .padding(vertical = 2.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        if (tab.isPulseWave) {
                            PulseWaveIcon(
                                color = itemColor,
                                modifier = Modifier.size(22.dp),
                                strokeWidthDp = if (isSelected) 2.2.dp else 1.8.dp
                            )
                        } else if (tab.icon != null) {
                            Icon(
                                imageVector = tab.icon,
                                contentDescription = tab.label,
                                tint = itemColor,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = tab.label,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = itemColor
                        )
                    }
                }
            }
        }
    }
}
